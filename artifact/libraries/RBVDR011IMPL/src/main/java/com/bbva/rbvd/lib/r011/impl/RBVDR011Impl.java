package com.bbva.rbvd.lib.r011.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.joda.time.LocalDate;
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
import com.bbva.rbvd.dto.insurancecancelation.aso.cancelationsimulation.CancelationSimulationASO;
import com.bbva.rbvd.dto.insurancecancelation.commons.*;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.aso.cypher.CypherASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
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

		String xcontractNumber = this.rbvdR003.executeCypherService(new CypherASO(input.getContractId(), CypherASO.KINDAPXCYPHER_CONTRACTID));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation xcontractNumber: {} *****", xcontractNumber);

		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		if (validateNewCancellationRequest(policy, input)) {
			return executeFirstCancellationRequest(xcontractNumber, input, policy);
		}

		EntityOutPolicyCancellationDTO out = isCancellationTypeValidaty(input);
		if (out == null) {
			return null;
		}

		if (policy == null) {
			return validatePolicy(out);
		}
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: policy = {} *****", policy);


		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}

		executeCancellationRequestMov(input, policy, RBVDConstants.MOV_BAJ);

		String policyid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
		String productid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
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
		arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat("dd/MM/yyyy").format(input.getCancellationDate() != null ? input.getCancellationDate().getTime() : Calendar.getInstance().getTime()));
		this.pisdR100.executeUpdateContractStatus(arguments);

		String listCancellation = this.applicationConfigurationService.getProperty("cancellation.list.endoso");

		String[] channelCancelation = listCancellation.split(",");

		String channelCode = input.getChannelId();
		String isChannelEndoso = Arrays.stream(channelCancelation).filter(channel -> channel.equals(channelCode)).findFirst().orElse(null);
		String userCode = input.getUserId();


		InputRimacBO inputrimac = new InputRimacBO();
		inputrimac.setTraceId(input.getTraceId());
		inputrimac.setNumeroPoliza(Integer.parseInt(policyid));
		inputrimac.setCodProducto(productid);
		PolicyCancellationPayloadBO inputPayload = new PolicyCancellationPayloadBO();
		PolizaBO poliza = new PolizaBO();
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
		ContratanteBO contratante = new ContratanteBO();
		contratante.setCorreo(email);
		contratante.setEnvioElectronico("S");
		inputPayload.setPoliza(poliza);
		inputPayload.setContratante(contratante);
		this.rbvdR012.executeCancelPolicyRimac(inputrimac, inputPayload);

		validateResponse(out, policyid);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation END *****");
		return out;
	}
	private EntityOutPolicyCancellationDTO isCancellationTypeValidaty(InputParametersPolicyCancellationDTO input){
		if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			return executeCancelPolicyHost(input);
		}
		return mapRetentionResponse(null, input, null, input.getCancellationType(), input.getCancellationType());
	}

	private EntityOutPolicyCancellationDTO executeCancelPolicyHost (InputParametersPolicyCancellationDTO input){
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - Start");
		ICF3Request icf3DTORequest = buildICF3Request(input);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - ICF3Request: {}", icf3DTORequest);
		ICF3Response icf3Response = this.rbvdR051.executePolicyCancellation(icf3DTORequest);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - ICF3Response: {}", icf3Response);

		if (icf3Response.getHostAdviceCode() != null) {
			LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - Error at icf3 execution - Host advice code: {}", icf3Response.getHostAdviceCode());
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return null;
		}
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - End");
		return mapICF3Response(input, icf3Response);
	}

	private ICF3Request buildICF3Request(InputParametersPolicyCancellationDTO input){
		ICF3Request icf3DTORequest = new ICF3Request();
		icf3DTORequest.setNUMCER(input.getContractId());
		String startDate = null;
		if (input.getCancellationDate() != null){
			Date date = input.getCancellationDate().getTime();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			startDate = dateFormat.format(date);
		}
		icf3DTORequest.setFECCANC(startDate);
		icf3DTORequest.setCODMOCA(input.getReason().getId());
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHost - starDate: {}", startDate);
		if(input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
				&& input.getNotifications().getContactDetails().get(0).getContact() != null
				&& input.getNotifications().getContactDetails().get(0).getContact().getContactDetailType().equals("EMAIL")){
			icf3DTORequest.setTIPCONT("001");
			if(input.getNotifications().getContactDetails().get(0).getContact().getAddress() != null){
				icf3DTORequest.setDESCONT(input.getNotifications().getContactDetails().get(0).getContact().getAddress());
			}
		}
		return icf3DTORequest;
	}
	private EntityOutPolicyCancellationDTO mapICF3Response(InputParametersPolicyCancellationDTO input, ICF3Response icf3Response){
		EntityOutPolicyCancellationDTO output = new EntityOutPolicyCancellationDTO();
		GenericStatusDTO status = new GenericStatusDTO();
		status.setId(icf3Response.getIcmf3s0().getDESSTCA());
		status.setDescription(icf3Response.getIcmf3s0().getDESSTCA());
		output.setStatus(status);
		output.setCancellationDate(input.getCancellationDate());
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId(icf3Response.getIcmf3s0().getCODMOCA());
		reason.setDescription(icf3Response.getIcmf3s0().getDESMOCA());
		output.setReason(reason);
		output.setNotifications(input.getNotifications());
		GenericAmountDTO insurerRefund = new GenericAmountDTO();
		insurerRefund.setAmount(Double.valueOf(icf3Response.getIcmf3s0().getIMDECIA()));
		insurerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVDCIA());
		output.setInsurerRefund(insurerRefund);
		GenericAmountDTO customerRefund = new GenericAmountDTO();
		customerRefund.setAmount(Double.valueOf(icf3Response.getIcmf3s0().getIMPCLIE()));
		customerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVIMC());
		output.setCustomerRefund(customerRefund);
		ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
		exchangeRateDTO.setTargetCurrency(icf3Response.getIcmf3s0().getDIVDEST());
		exchangeRateDTO.setCalculationDate(convertStringToDate(icf3Response.getIcmf3s0().getFETIPCA()));
		exchangeRateDTO.setValue(Double.valueOf(icf3Response.getIcmf3s0().getTIPCAMB()));
		exchangeRateDTO.setBaseCurrency(icf3Response.getIcmf3s0().getDIVORIG());
		output.setExchangeRate(exchangeRateDTO);
		return output;
	}

	private Date convertStringToDate(String date){
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		java.time.LocalDate localdate = java.time.LocalDate.parse(date, formatter);
		return Date.from(localdate.atStartOfDay(ZoneId.of("America/Lima")).toInstant());
	}

	private String updateContractStatusIfEndOfValidity(InputParametersPolicyCancellationDTO input, String statusId) {
		if (END_OF_VALIDATY.name().equals(input.getCancellationType())) {
			 return  RBVDConstants.TAG_PEN;
		}
		return statusId;
	}

	private boolean isActiveStatusId(Object statusId) {
		return statusId != null && !RBVDConstants.TAG_ANU.equals(statusId.toString()) && !RBVDConstants.TAG_BAJ.equals(statusId.toString());
	}

	private boolean isTodayStartDate(Object date) {
		LocalDate localDate = new LocalDate(StringUtils.substring(String.valueOf(date), 0, 10));
		LOGGER.info("***** RBVDR011Impl - isTodayStartDate LocalDate: {} *****", new LocalDate());
		LOGGER.info("***** RBVDR011Impl - isTodayStartDate date: {} *****", localDate);
		return new LocalDate().equals(localDate);
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
		if (policy != null && isActiveStatusId(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue())) && !isTodayStartDate(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue()))) {
			String insuranceProductId = policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()).toString();

			if (isAPXCancellationRequest(insuranceProductId, input.getChannelId())) {
				Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
				LOGGER.info("***** RBVDR011Impl - validateNewCancellationRequest - requestCancellationMovLast: {}", requestCancellationMovLast);
				return !isOpenCancellationRequest(requestCancellationMovLast);
			}
		}
		return false;
	}

	private EntityOutPolicyCancellationDTO executeFirstCancellationRequest(String xcontractNumber, InputParametersPolicyCancellationDTO input, Map<String, Object> policy) {
		ICR4DTO icr4Dto = new ICR4DTO();
		icr4Dto.setNUMCON(input.getContractId());
		icr4Dto.setICSITU("01");
		String result = this.rbvdR042.executeICR4(icr4Dto);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - result: {}", result);
		if (!result.equals(OK) && !result.equals(OK_WARN)) {
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return null;
		}

		CancelationSimulationASO cancelationSimulationASO = this.rbvdR012.executeSimulateInsuranceContractCancellations(xcontractNumber);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - cancelationSimulationASO: {}", cancelationSimulationASO);
		if (cancelationSimulationASO == null) {
			this.addAdvice(RBVDErrors.ERROR_TO_CONNECT_SERVICE_CANCELATIONSIMULATION_RIMAC.getAdviceCode());
			return null;
		}
			Map<String, Object> responseGetRequestCancellationId = this.pisdR103.executeGetRequestCancellationId();
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
		BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
		Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellation(requestCancellationId, input, policy, cancelationSimulationASO);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
			int isInserted = this.pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInserted: {}", isInserted);
		Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, "01", 1);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
		int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
		LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInsertedMov: {}", isInsertedMov);
		return mapRetentionResponse((String) policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), input, cancelationSimulationASO, "01", CODE_PENDING);
	}

	private EntityOutPolicyCancellationDTO executeCancellationRequestMov(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, String statusId) {
		String insuranceProductId = policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()).toString();
		if (isAPXCancellationRequest(insuranceProductId, input.getChannelId())) {
			Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());

			if (isOpenCancellationRequest(requestCancellationMovLast)) {
				BigDecimal requestCancellationId = new BigDecimal(requestCancellationMovLast.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()).toString());
				String reasonId = (String) requestCancellationMovLast.get(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue());
				input.getReason().setId(reasonId);
				Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, statusId, new Integer(requestCancellationMovLast.get(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue()).toString()) + 1);
				LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
				int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
				LOGGER.info("***** RBVDR011Impl - executeCancellationRequestMov - isInsertedMov: {}", isInsertedMov);
			}
		}
		return null;
	}

	private Map<String, Object> mapInRequestCancellation(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy, CancelationSimulationASO cancelationSimulationASO) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
		arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
		arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
		arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
		arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
		arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat("dd/MM/yyyy").format(cancelationSimulationASO.getData().getCancelationDate()));
		arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
		arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
		arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
		arguments.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancelationSimulationASO.getData().getCustomerRefund().getAmount());
		arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), cancelationSimulationASO.getData().getCustomerRefund().getCurrency());

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
		arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
		arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
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
																CancelationSimulationASO cancelationSimulationASO, String statusId, String statusDescription) {
		EntityOutPolicyCancellationDTO entityOutPolicyCancellationDTO = new EntityOutPolicyCancellationDTO();
		entityOutPolicyCancellationDTO.setId(policyId);
		if (cancelationSimulationASO != null && cancelationSimulationASO.getData() != null) {
			Calendar cancellationDate = Calendar.getInstance();
			cancellationDate.setTime(cancelationSimulationASO.getData().getCancelationDate());
			entityOutPolicyCancellationDTO.setCancellationDate(cancellationDate);
		} else {
			entityOutPolicyCancellationDTO.setCancellationDate(input.getCancellationDate());
		}
		entityOutPolicyCancellationDTO.setReason(new GenericIndicatorDTO());
		entityOutPolicyCancellationDTO.getReason().setId(input.getReason().getId());
		entityOutPolicyCancellationDTO.setStatus(new GenericStatusDTO());
		entityOutPolicyCancellationDTO.getStatus().setId(statusId);
		entityOutPolicyCancellationDTO.getStatus().setDescription(statusDescription);
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
}
