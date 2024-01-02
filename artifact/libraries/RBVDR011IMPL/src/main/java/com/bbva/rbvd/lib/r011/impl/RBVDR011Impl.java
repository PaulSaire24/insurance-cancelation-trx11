package com.bbva.rbvd.lib.r011.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bbva.rbvd.dto.cicsconnection.utils.ICR4DTO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationRescuePayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.rescue.RescueBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.lib.r011.impl.util.ConstantsUtil;
import com.google.common.base.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.aso.cypher.CypherASO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import org.springframework.web.client.RestClientException;

import static java.util.Objects.nonNull;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);
	private static final String CODE_REFUND = "REFUND";
	private static final String RECEIPT_STATUS_TYPE_LIST = "RECEIPT_STATUS_TYPE_LIST";
	private static final String OK = "OK";
	private static final String OK_WARN = "OK_WARN";

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);
		
		String xcontractNumber = this.rbvdR003.executeCypherService(new CypherASO(input.getContractId(), CypherASO.KINDAPXCYPHER_CONTRACTID));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation xcontractNumber: {} *****", xcontractNumber);
		
		EntityOutPolicyCancellationDTO out = this.rbvdR012.executeCancelPolicyHost(xcontractNumber
				, input.getCancellationDate()
				, input.getReason()
				, input.getNotifications());
		if (out == null) { return null; }
		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		if (policy == null) {
			if (!org.springframework.util.CollectionUtils.isEmpty(this.getAdviceList())
					&& this.getAdviceList().get(0).getCode().equals(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode())) {
				LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO NO ROYAL - Response = {} *****", out);
				this.getAdviceList().clear();
				return out;
			}
			return null; 
		}
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: policy = {} *****", policy);
		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}
		String policyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
		String productCompanyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
		String productId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");

		Map<String, Object> product = getProductByProductId(productId);

		String businessName= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME), "");
		String shortDesc= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC), "");

		Boolean isRescueCancellationOk = executeRescueCancellationRequest(input, policy, shortDesc);
		Double totalDebt = NumberUtils.toDouble(java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_TOTAL_DEBT_AMOUNT.getValue()), "0"));
		Double pendingAmount = NumberUtils.toDouble(java.util.Objects.toString(policy.get(RBVDProperties.KEY_REQUEST_CNCL_SETTLE_PENDING_PREMIUM_AMOUNT.getValue()), "0"));
		String statusId = RBVDConstants.TAG_BAJ;
		String movementType = RBVDConstants.MOV_BAJ;
		List<String> receiptStatusList = new ArrayList<>();
		receiptStatusList.add("INC");

		if(Objects.nonNull(out.getStatus()) && CODE_REFUND.equals(out.getStatus().getDescription())) {
			receiptStatusList.add("COB");
			statusId = RBVDConstants.TAG_ANU;
			movementType = RBVDConstants.MOV_ANU;
		}
		String email = "";
		
		Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
		mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());
		Map<String, Object> arguments = new HashMap<>();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue() , input.getBranchId());
		arguments.put(RBVDProperties.KEY_REQUEST_MOVEMENT_TYPE.getValue()               , movementType);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()         , statusId);
		
		this.pisdR100.executeSaveContractMovement(arguments);
		
		if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
				&& input.getNotifications().getContactDetails().get(0).getContact() != null) {
			email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
		}
		
		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue(), input.getBranchId());
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_SEND_CST_EMAIL_DESC.getValue(), email);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RETURNED_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RETURNED_CURRENCY.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_CHANNEL.getValue(), input.getChannelId());
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_CUSTOMER_RETURNED_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_CUSTOMER_RETURNED_CURRENCY.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_TOTAL_DEBT_AMOUNT.getValue(), totalDebt);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_SETTLE_PENDING_PREMIUM_AMOUNT.getValue(), pendingAmount);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_TRANSACTION.getValue(), input.getTransactionId());
		
		this.pisdR100.executeSaveContractCancellation(arguments);
		
		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), statusId);
		
		this.pisdR100.executeUpdateContractStatus(arguments);

		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RECEIPT_STATUS_TYPE.getValue(), statusId);
		arguments.put(RECEIPT_STATUS_TYPE_LIST, receiptStatusList);
		this.pisdR100.executeUpdateReceiptsStatusV2(arguments);

		String listCancellation = this.applicationConfigurationService.getProperty("cancellation.list.endoso");

		String[] channelCancelation = listCancellation.split(",");

		String channelCode = input.getChannelId();
		String isChannelEndoso = Arrays.stream(channelCancelation).filter(channel -> channel.equals(channelCode)).findFirst().orElse(null);
		String userCode = input.getUserId();


		InputRimacBO inputRimac = new InputRimacBO();
		inputRimac.setTraceId(input.getTraceId());
		inputRimac.setNumeroPoliza(Integer.parseInt(policyId));
		inputRimac.setCodProducto(productCompanyId);

		if(isLifeProduct(businessName)){
			inputRimac.setCodProducto(shortDesc);
		}

		PolicyCancellationPayloadBO inputPayload = new PolicyCancellationPayloadBO();
		CancelationRescuePayloadBO rescuePayload = new CancelationRescuePayloadBO();
		PolizaBO poliza = new PolizaBO();
		RescueBO polizaRescue = new RescueBO();
		if (input.getCancellationDate() == null) {
			input.setCancellationDate(Calendar.getInstance());
		}

		if(!Strings.isNullOrEmpty(isChannelEndoso)){
			LOGGER.info("***** RBVDR011Impl - CANAL: {} ACCEPTED  *****", isChannelEndoso);
			AutorizadorDTO autorizadorDTO = new AutorizadorDTO();
			autorizadorDTO.setFlagAutorizador("S");
			autorizadorDTO.setAutorizador(userCode);
			inputPayload.setAutorizador(autorizadorDTO);
			LOGGER.info("***** RBVDR011Impl - isChannelEndoso END  *****");
		}

		Date date = input.getCancellationDate().getTime();
		DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);  
		String strDate = dateFormat.format(date);  
		poliza.setFechaAnulacion(strDate);
		poliza.setCodigoMotivo(input.getReason().getId());
		polizaRescue.setFechaSolicitud(date);
		ContratanteBO contratante = new ContratanteBO();
		contratante.setCorreo(email);
		contratante.setEnvioElectronico("S");
		inputPayload.setPoliza(poliza);
		inputPayload.setContratante(contratante);
		rescuePayload.setPoliza(polizaRescue);
		rescuePayload.setContratante(contratante);

		callExecuteCancelationService(inputRimac, inputPayload, rescuePayload, shortDesc, isRescueCancellationOk);

		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation END *****");
		return out;
	}

	private void callExecuteCancelationService(InputRimacBO inputRimac, PolicyCancellationPayloadBO inputPayload, CancelationRescuePayloadBO rescuePayload, String shortDesc, Boolean isOk){
		if (ConstantsUtil.BUSINESS_NAME_VIDAINVERSION.equals(shortDesc)) {
			if (isOk) {
				rbvdR311.executeRescueCancelationRimac(inputRimac, rescuePayload);
			}
		} else {
			this.rbvdR012.executeCancelPolicyRimac(inputRimac, inputPayload);
		}
	}

	private Map<String, Object> getProductByProductId(String productId) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
		Map<String,Object> productById = (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
		return productById;
	}

	private boolean isLifeProduct(String businessName){
		if(Objects.nonNull(businessName) && (
				businessName.equals(ConstantsUtil.BUSINESS_NAME_VIDA) || businessName.equals(ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES)
		)){
			return true;
		}
		return false;
	}
	private Map<String, Object> mapInRequestCancellationCommon(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy){
		Map<String, Object> argumentsCommon = new HashMap<>();
		argumentsCommon.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
		argumentsCommon.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
		argumentsCommon.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
		argumentsCommon.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
		argumentsCommon.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
		argumentsCommon.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
		argumentsCommon.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
		argumentsCommon.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
		argumentsCommon.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
		argumentsCommon.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
		argumentsCommon.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
		argumentsCommon.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		argumentsCommon.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
		argumentsCommon.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), null);
		argumentsCommon.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), null);
		Date policyDate = new Date();
		DateFormat datePolicyFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);
		String strPolicyDate = datePolicyFormat.format(policyDate);
		argumentsCommon.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), strPolicyDate);
		argumentsCommon.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), strPolicyDate);
		NotificationsDTO notificationsDTO = input.getNotifications();
		if (nonNull(notificationsDTO)) {
			notificationsDTO.getContactDetails().stream().forEach(x -> {
				if (RBVDProperties.CONTACT_EMAIL_ID.getValue().equals(x.getContact().getContactDetailType())) {
					argumentsCommon.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), x.getContact().getAddress());
				} else if (RBVDProperties.CONTACT_MOBILE_ID.getValue().equals(x.getContact().getContactDetailType())) {
					argumentsCommon.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), x.getContact().getNumber());
				}
			});
		}
		argumentsCommon.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));
		argumentsCommon.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
		argumentsCommon.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
		return argumentsCommon;
	}
	private Map<String, Object> mapInRequestCancellationRescue(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy) {
		Map<String, Object> arguments = mapInRequestCancellationCommon(requestCancellationId, input, policy);
		arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);
		InsurerRefundCancellationDTO insurerRefundDTO = input.getInsurerRefund();
		if (nonNull(insurerRefundDTO)) {
			if (RBVDProperties.CONTRACT_TYPE_INTERNAL_ID.getValue().equals(insurerRefundDTO.getPaymentMethod().getContract().getContractType())) {
				arguments.put(RBVDProperties.FIELD_SETTLED_CANCEL_ACCOUNTS_NUMBER.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getId());
			}
			else if(RBVDProperties.CONTRACT_TYPE_EXTERNAL_ID.getValue().equals(insurerRefundDTO.getPaymentMethod().getContract().getContractType())) {
				arguments.put(RBVDProperties.FIELD_SETTLED_CANCEL_ACCOUNTS_NUMBER.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getNumber());
			}
		}
		arguments.put(RBVDProperties.FIELD_REQUEST_TYPE.getValue(), ConstantsUtil.REQUEST_TYPE_DEFAULT);
		return arguments;
	}
	private Map<String, Object> mapInRequestCancellationMov(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, String statusId, Integer requestCancellationMov) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
		arguments.put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), requestCancellationMov);
		arguments.put(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue(), input.getReason().getId());
		arguments.put(RBVDProperties.FIELD_CONTRACT_STATUS_DATE.getValue(), new Date());
		arguments.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), statusId);
		arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
		arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
		return arguments;
	}

	private Boolean setICR4Status(InputParametersPolicyCancellationDTO input, String status){
		ICR4DTO icr4Dto = new ICR4DTO();
		icr4Dto.setNUMCON(input.getContractId());
		icr4Dto.setICSITU(status);
		String result = this.rbvdR042.executeICR4(icr4Dto);
		LOGGER.info("***** RBVDR011Impl - ICR4 result: {}", result);
		if (!result.equals(OK) && !result.equals(OK_WARN)) {
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return false;
		}
		return true;
	}
	private Boolean executeRescueCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, String shortDesc) {
		if(ConstantsUtil.BUSINESS_NAME_VIDAINVERSION.equals(shortDesc)) {
			LOGGER.info("***** RBVDR011Impl - executeRescueCancellationRequest - ICR4 ");
			if (!setICR4Status(input, RBVDConstants.MOV_BAJ)) {
				return false;
			}
			LOGGER.info("***** RBVDR011Impl - executeRescueCancellationRequest - executeGetRequestCancellationId ");
			Map<String, Object> responseGetRequestCancellationId = this.pisdR103.executeGetRequestCancellationId();
			LOGGER.info("***** RBVDR011Impl - executeRescueCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
			BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
			Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellationRescue(requestCancellationId, input, policy);
			LOGGER.info("***s** RBVDR011Impl - executeRescueCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
			int isInserted = this.pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
			LOGGER.info("***** RBVDR011Impl - isInserted: {}", isInserted);
			Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, 1);
			LOGGER.info("***** RBVDR011Impl - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
			int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
			LOGGER.info("***** RBVDR011Impl - isInsertedMov: {}", isInsertedMov);
			return true;
		}
		else{
			return false;
		}
	}
}
