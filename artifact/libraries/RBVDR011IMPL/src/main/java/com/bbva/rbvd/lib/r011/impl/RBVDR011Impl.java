package com.bbva.rbvd.lib.r011.impl;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.lib.r011.impl.business.CancellationBusiness;
import com.bbva.rbvd.lib.r011.impl.transform.map.CancellationBean;
import com.bbva.rbvd.lib.r011.impl.transform.map.CancellationMap;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.INMEDIATE;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isStartDateTodayOrAfterToday;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("RBVDR011Impl - executePolicyCancellation START - input: {}", input);
		setInputToDefault(input);

		ICF2Response icf2Response = null;

		CancellationBean cancellationBean = new CancellationBean(this.pisdR401);
		CancellationBusiness cancellationBusiness = new CancellationBusiness(this.pisdR103, this.pisdR100, this.rbvdR311, this.applicationConfigurationService, this.icf3Connection, this.icr4Connection, this.cancellationRequestImpl);
		boolean isCancellationLegacyFlow = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty("cancellation.legacy.flow"));
		cancellationBusiness.setCancellationLegacyFlow(isCancellationLegacyFlow);

		// Busca la póliza en Oracle
		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		LOGGER.info("RBVDR011Impl - executePolicyCancellation - Policy: {} ", policy);
		boolean isRoyal = policy != null;

		/**
		 * Ejecuta la icf2 (Simulación en Host)
		 * - si es un seguro no royal
		 * - si se intenta cancelar un seguro emitido en el dia
		 */
		if (isStartDateTodayOrAfterToday(isRoyal, policy)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - executeICF2Transaction begin *****");
			icf2Response = this.icf2Connection.executeICF2Transaction(input);

			if (icf2Response == null) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}
		}

		Map<String, Object> policyMap = cancellationBean.getPolicyInsuranceData(isRoyal, policy, icf2Response);
		String policyId = Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		String productId = Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()));
		String productCodeForRimac = Objects.toString(policyMap.get(RBVDConstants.PRODUCT_CODE_FOR_RIMAC));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation policyId: {} *****", policyId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productId: {} *****", productId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productCodeForRimac: {} *****", productCodeForRimac);

		boolean isNewCancellation = this.cancellationRequestImpl.validateNewCancellationRequest(input, policy, isRoyal);

		//Validar si se trata de una nueva solicitud de cancelación
		if (!ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT.equals(productCodeForRimac) && (isCancellationLegacyFlow || isNewCancellation)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - new cancellation request *****");
			CancelationSimulationPayloadBO cancellationSimulationResponse = this.cancellationRequestImpl.getCancellationSimulationResponse(isRoyal, policy, input, policyId, productCodeForRimac);
			cancellationBusiness.setCancellationSimulationResponse(cancellationSimulationResponse);

			//Registrar la solicitud de cancelación solo en caso de no seguir el flujo legacy
			if(!isCancellationLegacyFlow) {
				boolean poolRetention = this.cancellationRequestImpl.executeFirstCancellationRequest(input, policy, isRoyal, icf2Response, cancellationSimulationResponse);

				if (!poolRetention) {
					this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
					return null;
				}
			}
		} else {
			//Seguir el flujo de cancelación
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}

		if (isCancellation(isCancellationLegacyFlow, input, productId, policy, isRoyal)) {
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}
		else {
			return cancellationBean.mapRetentionResponse(policyId, input, RBVDConstants.MOV_PEN, RBVDConstants.TAG_PENDING, input.getCancellationDate());
		}
	}

	private boolean isCancellation(boolean isCancellationLegacyFlow, InputParametersPolicyCancellationDTO input, String productId, Map<String, Object> policy, boolean isRoyal){
		//Si el producto y el canal se encuentran en la parametría de la consola de operaciones, solo se debe insertar la solicitud de cancelación

		return isCancellationLegacyFlow ||
				(input.getCancellationType().equals(INMEDIATE.name()) && !isAPXCancellationRequest(productId, input.getChannelId(), policy)) ||
					isStartDateTodayOrAfterToday(isRoyal, policy);
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

	private void setInputToDefault(InputParametersPolicyCancellationDTO input){
		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());
		if (input.getCancellationType() == null) input.setCancellationType(INMEDIATE.name());
	}

}
