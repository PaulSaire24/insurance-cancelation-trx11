package com.bbva.rbvd.lib.r011.impl;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.lib.r011.impl.business.CancellationBusiness;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.INMEDIATE;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isStartDateTodayOrAfterToday;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);
		boolean isRoyal;
		ICF2Response icf2Response = null;

		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());
		//if (input.getCancellationType() == null) input.setCancellationType(APPLICATION_DATE.name());
		if (input.getCancellationType() == null) input.setCancellationType(INMEDIATE.name());

		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		isRoyal = policy != null;

		if (isStartDateTodayOrAfterToday(isRoyal, policy)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - executeICF2Transaction begin *****");
			icf2Response = this.icf2Connection.executeICF2Transaction(input);

			if (icf2Response == null) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}
		}

		Map<String, Object> policyMap = getPolicyInsuranceData(isRoyal, policy, icf2Response);
		String policyId = Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		String productId = Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()));
		String productCodeForRimac = Objects.toString(policyMap.get(RBVDConstants.PRODUCT_CODE_FOR_RIMAC));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation policyId: {} *****", policyId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productId: {} *****", productId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productCodeForRimac: {} *****", productCodeForRimac);

		CancellationBusiness cancellationBusiness = new CancellationBusiness(this.pisdR103, this.pisdR100, this.rbvdR311,
				this.applicationConfigurationService, this.icf3Connection, this.icr4Connection, this.cancellationRequestImpl);

		//Validar si se trata de una nueva solicitud de cancelación
		if (this.cancellationRequestImpl.validateNewCancellationRequest(input, policy, isRoyal)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - new cancellation request *****");
			//Registrar la solicitud de cancelación
			if (!this.cancellationRequestImpl.executeFirstCancellationRequest(input, policy, isRoyal, icf2Response, policyId, productCodeForRimac)) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}
		} else {
			//Seguir el flujo de cancelación
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}

		//Si el producto y el canal se encuentran en la parametría de la consola de operaciones, solo se debe insertar la solicitud de cancelación
		if (isAPXCancellationRequest(productId, input.getChannelId()) &&
				!isStartDateTodayOrAfterToday(isRoyal, policy) && !input.getCancellationType().equals(INMEDIATE.name())) {
			//Retornar respuesta con estado PENDIENTE
			return mapRetentionResponse(policyId, input, RBVDConstants.MOV_PEN, RBVDConstants.TAG_PENDING, input.getCancellationDate());
		} else { //Seguir el flujo de cancelación
			return cancellationBusiness.cancellationPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}
	}

	private EntityOutPolicyCancellationDTO mapRetentionResponse(String policyId, InputParametersPolicyCancellationDTO input,
																String statusId, String statusDescription, Calendar cancellationDate) {
		LOGGER.info("***** RBVDR011Impl - mapRetentionResponse START *****");
		EntityOutPolicyCancellationDTO entityOutPolicyCancellationDTO = new EntityOutPolicyCancellationDTO();
		entityOutPolicyCancellationDTO.setId(policyId);
		entityOutPolicyCancellationDTO.setCancellationDate(cancellationDate);
		entityOutPolicyCancellationDTO.setReason(new GenericIndicatorDTO());
		entityOutPolicyCancellationDTO.getReason().setId(input.getReason().getId());
		entityOutPolicyCancellationDTO.setStatus(new GenericStatusDTO());
		entityOutPolicyCancellationDTO.getStatus().setId(statusId);
		entityOutPolicyCancellationDTO.getStatus().setDescription(statusDescription);
		LOGGER.info("***** RBVDR011Impl - mapRetentionResponse END *****");
		return entityOutPolicyCancellationDTO;
	}

	private boolean isAPXCancellationRequest(String insuranceProductId, String channelId) {
		String flagCancellationRequest = RBVDConstants.CANCELLATION_REQUEST.concat(StringUtils.defaultString(insuranceProductId)).concat(".").concat(channelId.toLowerCase());
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - property: {}", flagCancellationRequest);
		boolean isAPXCancellationRequest = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty(flagCancellationRequest));
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - isAPXCancellationRequest: {}", isAPXCancellationRequest);
		return isAPXCancellationRequest;
	}

	private void validateResponse(EntityOutPolicyCancellationDTO out, String policyId) {
		if (out.getId() == null) {
			out.setId(policyId);
		}
	}

	private Map<String, Object> getPolicyInsuranceData(boolean isRoyal, Map<String, Object> policy, ICF2Response icf2Response){
		String policyId;
		String insuranceProductId;
		String productCode;
		String productCompanyId;
		if(isRoyal){
			policyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
			insuranceProductId= Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");
			productCompanyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
			productCode = getProductCode(insuranceProductId, productCompanyId);
		}else{
			insuranceProductId = icf2Response.getIcmf1S2().getCODPROD();
			policyId = "";
			productCode = "";
		}

		Map<String, Object> policyMap = new HashMap<>();
		policyMap.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), policyId);
		policyMap.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(), insuranceProductId);
		policyMap.put(RBVDConstants.PRODUCT_CODE_FOR_RIMAC, productCode);
		return policyMap;
	}

	private Map<String, Object> getProductByProductId(String productId) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
		return (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
	}

	private boolean isLifeProduct(String businessName){
		return Objects.nonNull(businessName) && (
				businessName.equals(ConstantsUtil.BUSINESS_NAME_VIDA) || businessName.equals(ConstantsUtil.BUSINESS_NAME_EASYYES));
	}

	private String getProductCode(String insuranceProductId, String companyProductCode){
		Map<String, Object> product = getProductByProductId(insuranceProductId);
		LOGGER.info("***** RBVDR011Impl - executeSimulateCancelation: product = {} *****", product);
		String businessName= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME), "");
		String shortDesc= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC), "");
		if(isLifeProduct(businessName)) return shortDesc;
		else return companyProductCode;
	}
}
