package com.bbva.rbvd.lib.r011.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.bbva.rbvd.dto.insurancecancelation.bo.*;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Request;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.cicsconnection.utils.ICR4DTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.*;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;

import static java.util.Objects.nonNull;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);
	private static final String CODE_REFUND = "REFUND";
	private static final String CODE_PENDING = "PENDING";
	private static final String RECEIPT_STATUS_TYPE_LIST = "RECEIPT_STATUS_TYPE_LIST";
	private static final String OK = "OK";
	private static final String OK_WARN = "OK_WARN";

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);

		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());

		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		EntityOutPolicyCancellationDTO out;
		if(policy == null){ //producto no royal
			out = isCancellationTypeValidaty(input, null, policy);
			return validatePolicy(out);
		}
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: policy = {} *****", policy);
		String policyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
		String insuranceProductId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");
		String productCompanyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
		String productCode = getProductCode(insuranceProductId, productCompanyId);
		CancelationSimulationPayloadBO cancellationSimulationResponse;

		if (validateNewCancellationRequest(policy, input)) {
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: New cancellation request");
			InputRimacBO rimacSimulationRequest = buildRimacSimulationRequest(input, policyId, productCode);
			cancellationSimulationResponse = rbvdR311.executeSimulateCancelationRimac(rimacSimulationRequest);
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: cancellationSimulationResponse = {} *****", cancellationSimulationResponse);
			if (cancellationSimulationResponse == null || !executeFirstCancellationRequest(input, policy, cancellationSimulationResponse)) {
				return null;
			}
		}else{
			return cancelPolicy( input, policy, policyId, productCode);
		}

		//Si el producto y el canal se encuentran en la parametría de la consola de operaciones, solo se debe insertar la solicitud de cancelación
		if(isAPXCancellationRequest(insuranceProductId, input.getChannelId())){
			return mapRetentionResponse((String) policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), input, "01", CODE_PENDING,input.getCancellationDate());
		}else{ //Seguir el flujo de cancelación
			return cancelPolicy( input, policy, policyId,  productCode);
		}
	}

	private EntityOutPolicyCancellationDTO cancelPolicy(InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
												String policyId, String productCode){
		//se recupera de bd los datos guardados de la simulación
		LOGGER.info("***** RBVDR011Impl - cancelPolicy: Policy cancellation start");
		EntityOutPolicyCancellationDTO out;
		Map<String, Object> argumentsRequest = new HashMap<>();
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
		argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
		argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
		Map<String, Object> cancellationRequest = this.pisdR103.executeGetRequestCancellation(argumentsRequest);
		out = isCancellationTypeValidaty(input, cancellationRequest, policy);
		if (out == null) return null;

		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}

		executeCancellationRequestMov(input);

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
		arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat("dd/MM/yyyy").format(input.getCancellationDate().getTime()));
		this.pisdR100.executeUpdateContractStatus(arguments);

		String listCancellation = this.applicationConfigurationService.getProperty("cancellation.list.endoso");

		String[] channelCancelation = listCancellation.split(",");

		String channelCode = input.getChannelId();
		String isChannelEndoso = Arrays.stream(channelCancelation).filter(channel -> channel.equals(channelCode)).findFirst().orElse(null);
		String userCode = input.getUserId();

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

		validateResponse(out, policyId);
		LOGGER.info("***** RBVDR011Impl - cancelPolicy - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - cancelPolicy END *****");
		return out;
	}
	private EntityOutPolicyCancellationDTO isCancellationTypeValidaty(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy){
		if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			return executeCancelPolicyHost(input, cancellationRequest, policy);
		}
		return mapRetentionResponse(null, input, input.getCancellationType(), input.getCancellationType(), input.getCancellationDate());
	}

	private EntityOutPolicyCancellationDTO executeCancelPolicyHost (InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy){
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - Start");
		ICF3Request icf3DTORequest = buildICF3Request(input, cancellationRequest, policy);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - ICF3Request: {}", icf3DTORequest);
		ICF3Response icf3Response = this.rbvdR051.executePolicyCancellation(icf3DTORequest);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - ICF3Response: {}", icf3Response);

		if (icf3Response.getHostAdviceCode() != null) {
			LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - Error at icf3 execution - Host advice code: {}", icf3Response.getHostAdviceCode());
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return null;
		}
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - End");
		return mapICF3Response(input, icf3Response, cancellationRequest);
	}

	private Date getCancellationDate(Map<String, Object> cancellationRequest, InputParametersPolicyCancellationDTO input){
		Date date;
		if(cancellationRequest!=null){
			Timestamp dateTimestamp = (Timestamp)cancellationRequest.get(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue());
			date = new Date(dateTimestamp.getTime());
		}else{
			date = input.getCancellationDate().getTime();
		}
		return date;
	}
	private ICF3Request buildICF3Request(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy){
		ICF3Request icf3DTORequest = new ICF3Request();
		icf3DTORequest.setNUMCER(input.getContractId());
		Date date = getCancellationDate(cancellationRequest, input);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		icf3DTORequest.setFECCANC(dateFormat.format(date));
		icf3DTORequest.setCODMOCA(input.getReason().getId());
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - cancellationDate: {}", dateFormat.format(date));
		if(input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
				&& input.getNotifications().getContactDetails().get(0).getContact() != null
				&& input.getNotifications().getContactDetails().get(0).getContact().getContactDetailType().equals("EMAIL")){
			icf3DTORequest.setTIPCONT("001");
			if(input.getNotifications().getContactDetails().get(0).getContact().getAddress() != null){
				icf3DTORequest.setDESCONT(input.getNotifications().getContactDetails().get(0).getContact().getAddress());
			}
		}
		if(cancellationRequest != null && cancellationRequest.get(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue()) != null){
			icf3DTORequest.setCOMRIMA(((BigDecimal)cancellationRequest.get(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue())).doubleValue());
		}
		if(cancellationRequest != null && cancellationRequest.get(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue()) != null){
			icf3DTORequest.setMONTDEV(((BigDecimal)cancellationRequest.get(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue())).doubleValue());
		}

		if(policy != null && policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()) != null){
			icf3DTORequest.setNUMPOL(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue())));
		}

		if(policy != null && policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()) != null){
			icf3DTORequest.setPRODRI(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue())));
		}

		if(input.getIsRefund()){
			icf3DTORequest.setINDDEV("S");
		}else{
			icf3DTORequest.setINDDEV("N");
		}

		return icf3DTORequest;
	}
	private EntityOutPolicyCancellationDTO mapICF3Response(InputParametersPolicyCancellationDTO input, ICF3Response icf3Response, Map<String, Object> cancellationRequest){
		EntityOutPolicyCancellationDTO output = new EntityOutPolicyCancellationDTO();
		output.setId(icf3Response.getIcmf3s0().getIDCANCE());
		GenericStatusDTO status = new GenericStatusDTO();
		status.setId(icf3Response.getIcmf3s0().getDESSTCA());
		status.setDescription(icf3Response.getIcmf3s0().getDESSTCA());
		output.setStatus(status);
		Calendar calendarTime = Calendar.getInstance();
		Date date = getCancellationDate(cancellationRequest, input);
		calendarTime.setTime(date);
		output.setCancellationDate(calendarTime);
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId(icf3Response.getIcmf3s0().getCODMOCA());
		reason.setDescription(icf3Response.getIcmf3s0().getDESMOCA());
		output.setReason(reason);
		output.setNotifications(input.getNotifications());
		GenericAmountDTO insurerRefund = new GenericAmountDTO();
		insurerRefund.setAmount(icf3Response.getIcmf3s0().getIMDECIA());
		insurerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVDCIA());
		output.setInsurerRefund(insurerRefund);
		GenericAmountDTO customerRefund = new GenericAmountDTO();
		customerRefund.setAmount(icf3Response.getIcmf3s0().getIMPCLIE());
		customerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVIMC());
		output.setCustomerRefund(customerRefund);
		ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
		exchangeRateDTO.setTargetCurrency(icf3Response.getIcmf3s0().getDIVDEST());
		exchangeRateDTO.setCalculationDate(convertStringToDate(icf3Response.getIcmf3s0().getFETIPCA()));
		exchangeRateDTO.setValue(icf3Response.getIcmf3s0().getTIPCAMB());
		exchangeRateDTO.setBaseCurrency(icf3Response.getIcmf3s0().getDIVORIG());
		output.setExchangeRate(exchangeRateDTO);
		return output;
	}

	private Date convertStringToDate(String date){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		java.time.LocalDate localdate = java.time.LocalDate.parse(date, formatter);
		return Date.from(localdate.atStartOfDay(ZoneId.of("UTC")).toInstant());
	}

	private String updateContractStatusIfEndOfValidity(InputParametersPolicyCancellationDTO input, String statusId) {
		if (END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			 return  RBVDConstants.TAG_PEB;
		}
		return statusId;
	}

	private boolean isActiveStatusId(Object statusId) {
		return statusId != null && !RBVDConstants.TAG_ANU.equals(statusId.toString()) && !RBVDConstants.TAG_BAJ.equals(statusId.toString());
	}

	private boolean isAPXCancellationRequest(String insuranceProductId, String channelId) {
		String flagCancellationRequest = "cancellation.request.".concat(StringUtils.defaultString(insuranceProductId)).concat(".").concat(channelId.toLowerCase());
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

	private boolean isOpenCancellationRequest(Map<String, Object> requestCancellationMovLast) {
		if (requestCancellationMovLast != null) {
			String statusId = requestCancellationMovLast.get(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue()).toString();
			LOGGER.info("***** RBVDR011Impl - isOpenCancellationRequest - statusId: {}", statusId);
			return !RBVDConstants.MOV_BAJ.equals(statusId);
		}
		return false;
	}

	private boolean validateNewCancellationRequest(Map<String, Object> policy, InputParametersPolicyCancellationDTO input) {
		if (policy != null && isActiveStatusId(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()))) {
			Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
			LOGGER.info("***** RBVDR011Impl - validateNewCancellationRequest - requestCancellationMovLast: {}", requestCancellationMovLast);
			return !isOpenCancellationRequest(requestCancellationMovLast);
		}
		return false;
	}

	private boolean executeFirstCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
																	 CancelationSimulationPayloadBO cancellationSimulationResponse) {
		ICR4DTO icr4Dto = new ICR4DTO();
		icr4Dto.setNUMCON(input.getContractId());
		icr4Dto.setICSITU("01");
		String result = this.rbvdR042.executeICR4(icr4Dto);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - ICR4 result: {}", result);
		if (!result.equals(OK) && !result.equals(OK_WARN)) {
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return false;
		}

		Map<String, Object> responseGetRequestCancellationId = this.pisdR103.executeGetRequestCancellationId();
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
		BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
		Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellation(requestCancellationId, input, policy, cancellationSimulationResponse);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
		int isInserted = this.pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInserted: {}", isInserted);
		Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, "01", 1);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
		int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInsertedMov: {}", isInsertedMov);

		Map<String, Object> arguments = RBVDUtils.getMapContractNumber(input.getContractId());
		arguments.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_PEN);
		this.pisdR100.executeUpdateContractStatus(arguments);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - updateContractStatus: {}", RBVDConstants.TAG_PEN);

		return true;
	}

	private void executeCancellationRequestMov(InputParametersPolicyCancellationDTO input) {
		Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());

		if (isOpenCancellationRequest(requestCancellationMovLast)) {
			BigDecimal requestCancellationId = new BigDecimal(requestCancellationMovLast.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()).toString());
			String reasonId = (String) requestCancellationMovLast.get(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue());
			input.getReason().setId(reasonId);
			Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, new Integer(requestCancellationMovLast.get(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue()).toString()) + 1);
			LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
			int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
			LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - isInsertedMov: {}", isInsertedMov);
		}
	}

	private Map<String, Object> mapInRequestCancellation(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
														 CancelationSimulationPayloadBO cancellationSimulationResponse) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
		arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
		arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
		arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
		arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
		arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat("dd/MM/yyyy").format(input.getCancellationDate().getTime()));
		arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
		arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
		arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		arguments.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancellationSimulationResponse.getMonto());
		String currency = cancellationSimulationResponse.getMoneda();
		if (cancellationSimulationResponse.getCuenta() != null && cancellationSimulationResponse.getCuenta().getValor() != null) {
			String[] spliter = cancellationSimulationResponse.getCuenta().getValor().split("\\|\\|");
			if (spliter.length == 3) currency = spliter[2];
		}
		arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), currency);
		arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), null);
		NotificationsDTO notificationsDTO = input.getNotifications();
		if (nonNull(notificationsDTO)) {
			notificationsDTO.getContactDetails().stream().forEach(x -> {
				if (RBVDProperties.CONTACT_EMAIL_ID.getValue().equals(x.getContact().getContactDetailType())) {
					arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), x.getContact().getAddress());
				} else if (RBVDProperties.CONTACT_MOBILE_ID.getValue().equals(x.getContact().getContactDetailType())) {
					arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), x.getContact().getNumber());
				}
			});
		}

		arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));
		arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat("dd/MM/yy").format(input.getCancellationDate().getTime()));
		arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
		arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
		if(cancellationSimulationResponse.getExtornoComision() != null){
			arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), cancellationSimulationResponse.getExtornoComision());
			arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
		}else{
			arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
			arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);

		}

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

	private Map<String, Object> getProductByProductId(String productId) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
		return (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
	}

	private InputRimacBO buildRimacSimulationRequest(InputParametersPolicyCancellationDTO input, String policyId,
													 String productCode){
		InputRimacBO rimacSimulationRequest = new InputRimacBO();
		rimacSimulationRequest.setTraceId(input.getTraceId());
		ZoneId zone = ZoneId.of("UTC");
		java.time.LocalDate cancellationDate = input.getCancellationDate().toInstant().atZone(zone).toLocalDate();
		rimacSimulationRequest.setFechaAnulacion(cancellationDate);
		rimacSimulationRequest.setNumeroPoliza(Integer.parseInt(policyId));
		rimacSimulationRequest.setCodProducto(productCode);
		LOGGER.info("***** RBVDR011Impl - buildRimacSimulationRequest - {} *****", rimacSimulationRequest);
		return rimacSimulationRequest;
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
}
