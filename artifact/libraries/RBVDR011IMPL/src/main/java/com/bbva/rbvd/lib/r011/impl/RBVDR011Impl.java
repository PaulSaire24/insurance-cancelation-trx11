package com.bbva.rbvd.lib.r011.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;

import static com.bbva.rbvd.lib.r011.impl.cancellationRequest.CancellationRequestImpl.PENDING_CANCELLATION_STATUS;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);
	private static final String CODE_REFUND = "REFUND";
	private static final String CODE_PENDING = "PENDING";
	private static final String RECEIPT_STATUS_TYPE_LIST = "RECEIPT_STATUS_TYPE_LIST";
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	private static final String PRODUCT_CODE= "productCode";
	private static final String CONTRACT_STATUS_HOST_END_OF_VALIDITY = "contract.status.host.end.of.validity";
	private static final String CANCELLATION_LIST_ENDOSO = "cancellation.list.endoso";
	private static final String CANCELLATION_REQUEST = "cancellation.request.";
	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);
		boolean isRoyal;
		ICF2Response icf2Response = null;

		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());

		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		isRoyal = policy != null;

		if(cancellationRequestImpl.isStartDateTodayOrAfterToday(isRoyal, policy) || !isRoyal) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - executeICF2Transaction begin *****");
			icf2Response = this.icf2Connection.executeICF2Transaction(input);
			if(icf2Response == null) {this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode()); return null;}
		}

		Map<String, Object> policyMap = getPolicyInsuranceData(isRoyal, policy, icf2Response);
		String policyId = java.util.Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		String productId = java.util.Objects.toString(policyMap.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()));
		String productCodeForRimac = java.util.Objects.toString(policyMap.get(PRODUCT_CODE));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation policyId: {} *****", policyId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productId: {} *****", productId);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation productCodeForRimac: {} *****", productCodeForRimac);

		//Validar si se trata de una nueva solicitud de cancelación
		if(this.cancellationRequestImpl.validateNewCancellationRequest(input, policy, isRoyal)){
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - new cancellation request *****");
			//Registrar la solicitud de cancelación
			if(!this.cancellationRequestImpl.executeFirstCancellationRequest(input, policy, isRoyal,  icf2Response, policyId, productCodeForRimac)) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode()); return null;
			}
		}else{
			//Seguir el flujo de cancelación
			return cancelPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}

		//Si el producto y el canal se encuentran en la parametría de la consola de operaciones, solo se debe insertar la solicitud de cancelación
		if(isAPXCancellationRequest(productId, input.getChannelId()) && !cancellationRequestImpl.isStartDateTodayOrAfterToday(isRoyal, policy)){
			//Retornar respuesta con estado PENDIENTE
			return mapRetentionResponse(policyId, input, PENDING_CANCELLATION_STATUS, CODE_PENDING,input.getCancellationDate());
		}else{ //Seguir el flujo de cancelación
			return cancelPolicy(input, policy, policyId, productCodeForRimac, icf2Response, isRoyal);
		}
	}

	private EntityOutPolicyCancellationDTO cancelPolicy(InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
												String policyId, String productCode, ICF2Response icf2Response, boolean isRoyal){
		//se recupera de bd los datos guardados de la simulaciónnDTO
		LOGGER.info("***** RBVDR011Impl - cancelPolicy: Policy cancellation start");
		EntityOutPolicyCancellationDTO out;
		Map<String, Object> argumentsRequest = new HashMap<>();
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
		argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
		Map<String, Object> cancellationRequest = this.pisdR103.executeGetRequestCancellation(argumentsRequest);
		out = validateCancellationType(input, cancellationRequest, policy, icf2Response);
		if (out == null) return null;
		if (!isRoyal) return validatePolicy(out);

		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}

		if(!executeCancellationRequestMov(input)){
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}

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
		Map<String, Object> arguments = new HashMap<>(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue() , input.getBranchId());
		arguments.put(RBVDProperties.KEY_REQUEST_MOVEMENT_TYPE.getValue()               , movementType);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()         , statusId);

		this.pisdR100.executeSaveContractMovement(arguments);

		if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
				&& input.getNotifications().getContactDetails().get(0).getContact() != null) {
			email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
		}

		if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
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
			arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RECEIPT_STATUS_TYPE.getValue(), statusId);
			arguments.put(RECEIPT_STATUS_TYPE_LIST, receiptStatusList);
			this.pisdR100.executeUpdateReceiptsStatusV2(arguments);
		}

		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), updateContractStatusIfEndOfValidity(input, statusId));
		arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(DATE_FORMAT).format(input.getCancellationDate().getTime()));
		this.pisdR100.executeUpdateContractStatus(arguments);

		String listCancellation = this.applicationConfigurationService.getProperty(CANCELLATION_LIST_ENDOSO);

		String[] channelCancelation = listCancellation.split(",");

		String channelCode = input.getChannelId();
		String isChannelEndoso = Arrays.stream(channelCancelation).filter(channel -> channel.equals(channelCode)).findFirst().orElse(null);
		String userCode = input.getUserId();

		executeRimacCancellation(input, policyId, productCode, isChannelEndoso, userCode, cancellationRequest, email);

		validateResponse(out, policyId);
		LOGGER.info("***** RBVDR011Impl - cancelPolicy - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - cancelPolicy END *****");
		return out;
	}
	private EntityOutPolicyCancellationDTO validateCancellationType(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy, ICF2Response icf2Response){
		if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			return this.icf3Connection.executeICF3Transaction(input, cancellationRequest, policy, icf2Response);
		}else{
			boolean icr4RespondsOk = this.icr4Connection.executeICR4Transaction(input, this.applicationConfigurationService.getDefaultProperty(CONTRACT_STATUS_HOST_END_OF_VALIDITY,"08"));
			if(!icr4RespondsOk) {
				this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
				return null;
			}
		}

		return mapRetentionResponse(null, input, input.getCancellationType(), input.getCancellationType(), input.getCancellationDate());
	}

	private String updateContractStatusIfEndOfValidity(InputParametersPolicyCancellationDTO input, String statusId) {
		if (END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			 return  RBVDConstants.TAG_PEB;
		}
		return statusId;
	}

	private boolean isAPXCancellationRequest(String insuranceProductId, String channelId) {
		String flagCancellationRequest = CANCELLATION_REQUEST.concat(StringUtils.defaultString(insuranceProductId)).concat(".").concat(channelId.toLowerCase());
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - property: {}", flagCancellationRequest);
		boolean isAPXCancellationRequest = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty(flagCancellationRequest));
		LOGGER.info("***** RBVDR011Impl - isAPXCancellationRequest - isAPXCancellationRequest: {}", isAPXCancellationRequest);
		return isAPXCancellationRequest;
	}

	private Map<String, Object> executeGetRequestCancellationMovLast(String contractId) {
		Map<String, Object> argumentsRequest = new HashMap<>();
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), contractId.substring(0, 4));
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), contractId.substring(4, 8));
		argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), contractId.substring(10));
		List<Map<String, Object>> requestCancellationMovLast = this.pisdR103.executeGetRequestCancellationMovLast(argumentsRequest);
		if (requestCancellationMovLast != null && !requestCancellationMovLast.isEmpty()) {
			return requestCancellationMovLast.get(requestCancellationMovLast.size()-1);
		}
		return null;
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

	private void validateResponse(EntityOutPolicyCancellationDTO out, String policyId) {
		if (out.getId() == null) {
			out.setId(policyId);
		}
	}

	private EntityOutPolicyCancellationDTO validatePolicy(EntityOutPolicyCancellationDTO out) {
		if (!org.springframework.util.CollectionUtils.isEmpty(this.getAdviceList())
				&& this.getAdviceList().get(0).getCode().equals(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode())) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO NO ROYAL - Response = {} *****", out);
			this.getAdviceList().clear();
			return out;
		}
		return null;
	}

	private boolean executeCancellationRequestMov(InputParametersPolicyCancellationDTO input) {
		Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
		if(requestCancellationMovLast == null) return false;
		if (this.cancellationRequestImpl.isOpenOrNotExistsCancellationRequest(requestCancellationMovLast)) {
			BigDecimal requestCancellationId = new BigDecimal(requestCancellationMovLast.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()).toString());
			String reasonId = (String) requestCancellationMovLast.get(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue());
			input.getReason().setId(reasonId);
			Map<String, Object> argumentsForSaveRequestCancellationMov = this.cancellationRequestImpl.mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, new Integer(requestCancellationMovLast.get(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue()).toString()) + 1);
			LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
			int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
			LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - isInsertedMov: {}", isInsertedMov);
			return isInsertedMov == 1;
		}
		return false;
	}

	private Map<String, Object> getPolicyInsuranceData(boolean isRoyal, Map<String, Object> policy, ICF2Response icf2Response){
		String policyId;
		String insuranceProductId;
		String productCode;
		String productCompanyId;
		if(isRoyal){
			policyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
			insuranceProductId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");
			productCompanyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
			productCode = getProductCode(insuranceProductId, productCompanyId);
		}else{
			insuranceProductId = icf2Response.getIcmf1S2().getCODPROD();
			policyId = "";
			productCode = "";
		}

		Map<String, Object> policyMap = new HashMap<>();
		policyMap.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), policyId);
		policyMap.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(), insuranceProductId);
		policyMap.put(PRODUCT_CODE, productCode);
		return policyMap;
	}

	private Map<String, Object> getProductByProductId(String productId) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
		return (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
	}

	private boolean isLifeProduct(String businessName){
		return Objects.nonNull(businessName) && (
				businessName.equals(ConstantsUtil.BUSINESS_NAME_VIDA) || businessName.equals(ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES));
	}

	private String getProductCode(String insuranceProductId, String companyProductCode){
		Map<String, Object> product = getProductByProductId(insuranceProductId);
		LOGGER.info("***** RBVDR011Impl - executeSimulateCancelation: product = {} *****", product);
		String businessName= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME), "");
		String shortDesc= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC), "");
		if(isLifeProduct(businessName)) return shortDesc;
		else return companyProductCode;
	}

	private void executeRimacCancellation(InputParametersPolicyCancellationDTO input, String policyId, String productCode,
										  String isChannelEndoso, String userCode, Map<String, Object> cancellationRequest, String email){
		InputRimacBO inputrimac = new InputRimacBO();
		inputrimac.setTraceId(input.getTraceId());
		inputrimac.setNumeroPoliza(Integer.parseInt(policyId));
		inputrimac.setCodProducto(productCode);
		PolicyCancellationPayloadBO inputPayload = new PolicyCancellationPayloadBO();
		PolizaBO poliza = new PolizaBO();

		if(!Strings.isNullOrEmpty(isChannelEndoso)){
			LOGGER.info("***** RBVDR011Impl - CANAL: {} ACCEPTED  *****", isChannelEndoso);
			AutorizadorDTO autorizadorDTO = new AutorizadorDTO();
			autorizadorDTO.setFlagAutorizador("S");
			autorizadorDTO.setAutorizador(userCode);
			inputPayload.setAutorizador(autorizadorDTO);
			LOGGER.info("***** RBVDR011Impl - isChannelEndoso END  *****");
		}

		Timestamp dateTimestamp = (Timestamp)cancellationRequest.get(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue());
		Date date = new Date(dateTimestamp.getTime());
		DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);
		String strDate = dateFormat.format(date);
		poliza.setFechaAnulacion(strDate);
		poliza.setCodigoMotivo(input.getReason().getId());
		ContratanteBO contratante = new ContratanteBO();
		contratante.setCorreo(email);
		contratante.setEnvioElectronico("S");
		inputPayload.setPoliza(poliza);
		inputPayload.setContratante(contratante);

		rbvdR311.executeCancelPolicyRimac(inputrimac, inputPayload);
	}

}
