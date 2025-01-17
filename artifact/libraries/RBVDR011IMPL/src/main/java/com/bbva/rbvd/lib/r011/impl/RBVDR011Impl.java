package com.bbva.rbvd.lib.r011.impl;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.lib.r011.impl.business.CancellationBusiness;
import com.bbva.rbvd.lib.r011.impl.service.api.RimacApi;
import com.bbva.rbvd.lib.r011.impl.transform.bean.CancellationBean;

import java.util.*;

import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isStartDateTodayOrAfterToday;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("RBVDR011Impl - executePolicyCancellation() - START || input: {}", input);
		setValuesInit(input);

		// Flag que ejecuta el flujo anterior de la cancelación
		boolean isCancellationLegacyFlow = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty("cancellation.legacy.flow"));
		LOGGER.info("executePolicyCancellation() - Es el flujo legacy de cancelación?? : {}", ValidationUtil.validationLogger(isCancellationLegacyFlow));

		CancellationBean cancellationBean = new CancellationBean(this.pisdR401, applicationConfigurationService);
		CancellationBusiness cancellationBusiness = new CancellationBusiness(this.pisdR103, this.pisdR100, this.rbvdR311,
				this.pisdR401, this.applicationConfigurationService, this.icf3Connection, this.icr4Connection, this.cancellationRequestImpl, this.rbvdR305);
		cancellationBusiness.setCancellationLegacyFlow(isCancellationLegacyFlow);

		// Objeto que guarda la simulación de Host o Rimac
		ICF2Response icf2Response = null;
		CancelationSimulationPayloadBO cancellationSimulationResponse = null;

		String insuranceProductId = "";
		String policyId = "";
		String productCompanyId = "";
		String productCodeForRimac = "";

		// Busca la póliza en Oracle
		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		LOGGER.info("executePolicyCancellation() - datos del contrato: {} ", policy);

		// FLag para saber si es un producto royal o no royal
		boolean isRoyal = policy != null;
		LOGGER.info("executePolicyCancellation - Es un seguro royal?? : {}", ValidationUtil.validationLogger(isRoyal));

		// Si el contrato no es baj o anu seteamos valores a usar
		if(isRoyal) {
			String statusIdd= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
			HashSet<String> canceledStatus = new HashSet<>(Arrays.asList(RBVDConstants.TAG_ANU, RBVDConstants.TAG_BAJ));
			if (canceledStatus.contains(statusIdd)) {
				this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
				return null;
			}

			insuranceProductId= Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");
			policyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
			productCompanyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
			productCodeForRimac = cancellationBean.getProductCode(insuranceProductId, productCompanyId);
		}

		/**
		 * IF   ( si es un <seguro no royal>  ||  si se intenta cancelar un seguro emitido en el dia <seguro royal> )
		 * 			- Ejecuta la icf2 (Simulación en Host) para obtener los montos de devolución hacia el cliente
		 * ELSE ( si es cancelación del dia d + 1 <seguro royal> )
		 * 			- Ejecuta el servicio simulación de Rimac para obtener los montos de devolución hacia el cliente
		 */
		if(ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT.equals(productCodeForRimac)){
			LOGGER.info("RBVDR011Impl - executePolicyCancellation() - no entra en esto flujos *****");
		} else if (isStartDateTodayOrAfterToday(isRoyal, policy)) {
			LOGGER.info("RBVDR011Impl - executePolicyCancellation() - executeICF2Transaction begin *****");
			icf2Response = this.icf2Connection.executeICF2Transaction(input);
			LOGGER.info("RBVDR011Impl - executePolicyCancellation() - icf2Response: {}", icf2Response);

			if (icf2Response == null) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}

			if(!isRoyal) {
				insuranceProductId = Objects.toString(icf2Response.getIcmf1S2().getCODPROD(), "0");
				policyId = Objects.toString(icf2Response.getIcmf1S2().getNUMPOL(), "0");
				productCodeForRimac = Objects.toString(icf2Response.getIcmf1S2().getPRODRI(), "0");
			}

			String statusContractNoRoyal = Objects.toString(icf2Response.getIcmf1S2().getIDSTCON(), "");



		} else {
			RimacApi rimacApi = new RimacApi(this.rbvdR311, this.applicationConfigurationService);

			cancellationSimulationResponse = rimacApi.getCancellationSimulationResponse(input, policyId, productCodeForRimac);
			if(cancellationSimulationResponse == null){
				this.addAdvice(RBVDErrors.ERROR_TO_CONNECT_SERVICE_CANCELATIONSIMULATION_RIMAC.getAdviceCode());
				return null;
			}
			LOGGER.info("RBVDR011Impl - executePolicyCancellation() - cancellationSimulationResponse: {}", cancellationSimulationResponse);
			cancellationBusiness.setCancellationSimulationResponse(cancellationSimulationResponse);
		}

		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation insuranceProductId: {} *****", insuranceProductId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation policyId: {} *****", policyId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productCodeForRimac: {} *****", productCodeForRimac);

		// Flag que nos indica que cumple las validaciones para generar una solicitud de cancelación pero si el flag isCancellationLegacyFlow es true
		// siempre sera falso pq representa el flujo legacy donde no se insertaba en tablas de solicitud de cancelación
		boolean isNewCancellation = isCancellationLegacyFlow ? false : this.cancellationRequestImpl.validateNewCancellationRequest(input, policy, isRoyal);
		LOGGER.info("RBVDR011Impl - executePolicyCancellation() - isNewCancellation: {}", isNewCancellation);

		String massiveProductsParameter = this.applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",");
		LOGGER.info("RBVDR011Impl - executePolicyCancellation() - massiveProductsParameter: {}", massiveProductsParameter);

		// Obtener el correo
		String email = cancellationBean.getEmailFromInput(input, null, icf2Response);
		LOGGER.info("RBVDR011Impl - executePolicyCancellation() - email: {}", email);

		// Ejecuta la logica de la cancelacion primera cancelacion o retencion
		return cancellationBusiness.executeFirstCancellationOrCancellationOrRetention(isRoyal, policy, input, policyId,
				icf2Response, productCodeForRimac, isNewCancellation, insuranceProductId,
				massiveProductsParameter, email, cancellationBusiness, cancellationBean);
	}

	private void setValuesInit(InputParametersPolicyCancellationDTO input){
		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());
		if (input.getCancellationType() == null) input.setCancellationType(APPLICATION_DATE.name());
	}

}
