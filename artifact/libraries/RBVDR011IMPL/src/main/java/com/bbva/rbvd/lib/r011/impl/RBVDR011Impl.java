package com.bbva.rbvd.lib.r011.impl;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.lib.r011.impl.business.CancellationBusiness;
import com.bbva.rbvd.lib.r011.impl.service.api.RimacApi;
import com.bbva.rbvd.lib.r011.impl.transform.bean.CancellationBean;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.INMEDIATE;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isStartDateTodayOrAfterToday;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("RBVDR011Impl - executePolicyCancellation() - START || input: {}", input);
		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());
		if (input.getCancellationType() == null) input.setCancellationType(APPLICATION_DATE.name());

		// Flag que ejecuta el flujo anterior de la cancelación
		boolean isCancellationLegacyFlow = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty("cancellation.legacy.flow"));
		LOGGER.info("executePolicyCancellation() - Es el flujo legacy de cancelación?? : {}", isCancellationLegacyFlow ? "si": "no");

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
		LOGGER.info("executePolicyCancellation - Es un seguro royal?? : {}", isRoyal ? "si": "no");

		// Valores a usar
		if(isRoyal) {
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
		if (isStartDateTodayOrAfterToday(isRoyal, policy)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - executeICF2Transaction begin *****");
			icf2Response = this.icf2Connection.executeICF2Transaction(input);

			if (icf2Response == null) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}

			if(!isRoyal) {
				insuranceProductId = Objects.toString(icf2Response.getIcmf1S2().getCODPROD(), "0");
				policyId = Objects.toString(icf2Response.getIcmf1S2().getNUMPOL(), "0");
				productCodeForRimac = Objects.toString(icf2Response.getIcmf1S2().getPRODRI(), "0");
			}

		} else {
			RimacApi rimacApi = new RimacApi(this.rbvdR311, this.applicationConfigurationService);

			cancellationSimulationResponse = rimacApi.getCancellationSimulationResponse(input, policyId, productCodeForRimac);
			cancellationBusiness.setCancellationSimulationResponse(cancellationSimulationResponse);
		}

		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation insuranceProductId: {} *****", insuranceProductId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation policyId: {} *****", policyId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productCodeForRimac: {} *****", productCodeForRimac);

		// Flag que nos indica que cumple las validaciones para generar una solicitud de cancelación
		boolean isNewCancellation = this.cancellationRequestImpl.validateNewCancellationRequest(input, policy, isRoyal);

		String massiveProductsParameter = this.applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",");

		// Obtner el correo
		String email = cancellationBean.getEmailFromInput(input, null, icf2Response);

		/**
		 * IF   ( <si no> es un seguro VIDA_INVERSION  <Y>  <si> es el flujo legacy (defecto false) || <si> cumple validación para generar solicitud de cancelación )
		 * 			- Genera una solicitud de cancelación que viaja al Pool de retenciones
		 * ELSE
		 * 			- Ejecuta la cancelación al instante
		 */
		if (!ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT.equals(productCodeForRimac) && (isCancellationLegacyFlow || isNewCancellation)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - new cancellation request *****");

			//Registrar la solicitud de cancelación solo en caso de no seguir el flujo legacy
			if(!isCancellationLegacyFlow) {
				boolean isRequestCancellation = this.cancellationRequestImpl.executeFirstCancellationRequest(input, policy, isRoyal, icf2Response,
						cancellationSimulationResponse, massiveProductsParameter, email);

				if (!isRequestCancellation) {
					this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
					return null;
				}
			}
		} else {
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal, massiveProductsParameter);
		}


		/**
		 * IF   ( si es el flujo legacy (defecto false)
		 * 					|| si es cancelación POR RECLAMO
		 * 					|| ( si es cancelación INMEDIATA <Y> si el FLag de canal -- producto -- frecuencia de pago es TRUE )  )
		 * 			- Ejecuta la cancelación al instante
		 * ELSE
		 * 			- Mapea los campos de salida, ya que solo se queda en una solicitud de cancelación
		 */
		if (isCancellation(isCancellationLegacyFlow, input, insuranceProductId, policy)) {
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal, massiveProductsParameter);
		}
		else {
			return cancellationBean.mapRetentionResponse(policyId, input, RBVDConstants.MOV_PEN, RBVDConstants.TAG_PENDING, input.getCancellationDate());
		}
	}

	private boolean isCancellation(boolean isCancellationLegacyFlow, InputParametersPolicyCancellationDTO input, String productId, Map<String, Object> policy) {
		//Si el producto y el canal se encuentran en la parametría de la consola de operaciones, solo se debe insertar la solicitud de cancelación

		return isCancellationLegacyFlow || input.getCancellationType().equals(INMEDIATE.name()) ||
			(input.getCancellationType().equals(APPLICATION_DATE.name()) && !isAPXCancellationRequest(productId, input.getChannelId(), policy)) ;
	}

	// FLag de canal -- producto -- frecuencia de pago
	private boolean isAPXCancellationRequest(String insuranceProductId, String channelId, Map<String, Object> policy) {
		if (policy == null) return false;
		StringBuilder sb = new StringBuilder();
		sb.append(RBVDConstants.CANCELLATION_REQUEST);
		sb.append(StringUtils.defaultString(insuranceProductId));
		sb.append(".");
		sb.append(channelId.toLowerCase());
		sb.append(".");
		sb.append(Optional.ofNullable(policy.get(RBVDProperties.KEY_RESPONSE_PAYMENT_FREQUENCY_NAME.getValue())).map(Object::toString).orElse(StringUtils.EMPTY).toLowerCase());
		String flagCancellationRequest = sb.toString();
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - property: {}", flagCancellationRequest);
		boolean isAPXCancellationRequest = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty(flagCancellationRequest));
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - isAPXCancellationRequest: {}", isAPXCancellationRequest);
		return isAPXCancellationRequest;
	}

}
