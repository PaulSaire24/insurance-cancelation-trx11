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

import com.bbva.rbvd.dto.insurancecancelation.aso.cancelationsimulation.CancelationSimulationASO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
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

import static java.util.Objects.nonNull;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);
	private static final String CODE_REFUND = "REFUND";
	private static final String RECEIPT_STATUS_TYPE_LIST = "RECEIPT_STATUS_TYPE_LIST";

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);
		
		String xcontractNumber = this.rbvdR003.executeCypherService(new CypherASO(input.getContractId(), CypherASO.KINDAPXCYPHER_CONTRACTID));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation xcontractNumber: {} *****", xcontractNumber);

		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		String productid = null;
		if (policy != null) {
			 productid = java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
		}

		boolean isCancellationRequest = BooleanUtils.toBoolean(this.applicationConfigurationService.getProperty("cancellation.request.".concat(StringUtils.defaultString(productid)).concat(".").concat(input.getChannelId())));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - isCancellationRequest: {}", isCancellationRequest);
		if (isCancellationRequest) {
			CancelationSimulationASO cancelationSimulationASO = this.rbvdR012.executeSimulateInsuranceContractCancellations(xcontractNumber);
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - cancelationSimulationASO: {}", cancelationSimulationASO);
			Map<String, Object> responseGetRequestCancellationId = this.pisdR103.executeGetRequestCancellationId();
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
			BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
			Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellation(requestCancellationId, input, cancelationSimulationASO);
			LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
			this.pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
			return mapRetentionResponse(input, cancelationSimulationASO);
		}
		
		EntityOutPolicyCancellationDTO out = this.rbvdR012.executeCancelPolicyHost(xcontractNumber
				, input.getCancellationDate()
				, input.getReason()
				, input.getNotifications());
		if (out == null) { return null; }

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
		String policyid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
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
		
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation END *****");
		return out;
	}

	public Map<String, Object> mapInRequestCancellation(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, CancelationSimulationASO cancelationSimulationASO) {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
		arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat("dd/MM/yyyy").format(cancelationSimulationASO.getData().getCancelationDate()));
		arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), null);
		arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), null);
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

		arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), null);
		arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), null);
		arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
		arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
		return arguments;
	}

	private EntityOutPolicyCancellationDTO mapRetentionResponse(InputParametersPolicyCancellationDTO input, CancelationSimulationASO cancelationSimulationASO) {
		Calendar cancellationDate = Calendar.getInstance();
		cancellationDate.setTime(cancelationSimulationASO.getData().getCancelationDate());

		EntityOutPolicyCancellationDTO entityOutPolicyCancellationDTO = new EntityOutPolicyCancellationDTO();
		entityOutPolicyCancellationDTO.setId("ID");
		entityOutPolicyCancellationDTO.setCancellationDate(cancellationDate);
		entityOutPolicyCancellationDTO.setReason(new GenericIndicatorDTO());
		entityOutPolicyCancellationDTO.getReason().setId(input.getReason().getId());
		entityOutPolicyCancellationDTO.setStatus(new GenericStatusDTO());
		entityOutPolicyCancellationDTO.getStatus().setId("STATUS");
		return entityOutPolicyCancellationDTO;
	}
}
