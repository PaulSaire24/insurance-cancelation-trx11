package com.bbva.rbvd.lib.r011.impl;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import com.bbva.apx.exception.business.BusinessException;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Request;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationRescuePayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.BankAccountBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.rescue.RescueInversionBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.ExchangeRateDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.ContractCancellationDTO;
import com.bbva.rbvd.lib.r011.impl.util.ConstantsUtil;
import com.google.common.base.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.aso.cypher.CypherASO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;

import static java.util.Objects.nonNull;
import static java.util.Objects.isNull;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);
	private static final String CODE_REFUND = "REFUND";
	private static final String RECEIPT_STATUS_TYPE_LIST = "RECEIPT_STATUS_TYPE_LIST";

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);

		if (input.getCancellationDate() == null) input.setCancellationDate(Calendar.getInstance());
		String xcontractNumber = this.rbvdR003.executeCypherService(new CypherASO(input.getContractId(), CypherASO.KINDAPXCYPHER_CONTRACTID));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation xcontractNumber: {} *****", xcontractNumber);

		EntityOutPolicyCancellationDTO out;
		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		if (policy == null) {
			if (!org.springframework.util.CollectionUtils.isEmpty(this.getAdviceList())
					&& this.getAdviceList().get(0).getCode().equals(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode())) {
				out = this.rbvdR012.executeCancelPolicyHost(xcontractNumber, input.getCancellationDate(), input.getReason(), input.getNotifications());
				LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO NO ROYAL - Response = {} *****", out);
				this.getAdviceList().clear();
				return out;
			}
			return null; 
		}
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation: policy = {} *****", policy);
		String policyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
		String productCompanyId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
		String productId= java.util.Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");

		Map<String, Object> product = getProductByProductId(productId);

		String businessName= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME), "");
		String shortDesc= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC), "");
		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}

		InputRimacBO inputRimac = new InputRimacBO();
		inputRimac.setTraceId(input.getTraceId());
		inputRimac.setNumeroPoliza(Integer.parseInt(policyId));
		inputRimac.setCodProducto(productCompanyId);

		if(isLifeProduct(businessName)){
			inputRimac.setCodProducto(shortDesc);
		}
		out = callCancelPolicyHost(xcontractNumber, input,policy ,shortDesc);

		if(isNull(out)) return null;

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
		arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(input.getCancellationDate().getTime()));

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
		try {
			cancelPolicyByProduct(inputRimac, inputPayload,input, shortDesc);
		} catch (BusinessException exception) {
			this.addAdviceWithDescription(exception.getAdviceCode(), exception.getMessage());
			return null;
		}

		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation - PRODUCTO ROYAL ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation END *****");
		return out;
	}



	private void cancelPolicyByProduct(InputRimacBO inputrimac,PolicyCancellationPayloadBO inputPayload, InputParametersPolicyCancellationDTO input, String shortDesc){
		if (!ConstantsUtil.BUSINESS_NAME_VIDAINVERSION.equals(shortDesc)){
			this.rbvdR012.executeCancelPolicyRimac(inputrimac, inputPayload);
		}
		else {
			inputrimac.setShortDesc(shortDesc);
			Date date = input.getCancellationDate().getTime();
			DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);
			String strDate = dateFormat.format(date);
			RescueInversionBO polizaRescue = new RescueInversionBO();
			CancelationRescuePayloadBO cancelPayload = new CancelationRescuePayloadBO();
			ContratanteBO contratante = new ContratanteBO();
			BankAccountBO cuentaBancaria = new BankAccountBO();

			polizaRescue.setFechaSolicitud(strDate);
			String email = "";
			if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
					&& input.getNotifications().getContactDetails().get(0).getContact() != null) {
				email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
			}
			contratante.setCorreo(email);
			contratante.setEnvioElectronico("S");
			if (input.getInsurerRefund()!=null) {
				ContractCancellationDTO contractReturn = input.getInsurerRefund().getPaymentMethod().getContract();
				if(contractReturn.getId()!=null){
					contractReturn.setNumber(contractReturn.getId());
				}
				cuentaBancaria.setTipoCuenta("A");
				if(contractReturn.getNumber().substring(10,12).equals("01")){
					cuentaBancaria.setTipoCuenta("C");
				}
				cuentaBancaria.setNumeroCuenta(contractReturn.getNumber());
				cuentaBancaria.setTipoMoneda(this.applicationConfigurationService.getProperty("cancellation.rescue.currency"));
				cuentaBancaria.setRazonSocialBanco(this.applicationConfigurationService.getProperty("cancellation.rescue.bank.name"));
				contratante.setCuentaBancaria(cuentaBancaria);
			}
			cancelPayload.setPoliza(polizaRescue);
			cancelPayload.setContratante(contratante);

			this.rbvdR311.executeRescueCancelationRimac(inputrimac, cancelPayload);
		}
	}

	private EntityOutPolicyCancellationDTO callCancelPolicyHost(String xcontractNumber, InputParametersPolicyCancellationDTO input, Map<String, Object> policy,String shortDesc){
		if (!ConstantsUtil.BUSINESS_NAME_VIDAINVERSION.equals(shortDesc)){
			return this.rbvdR012.executeCancelPolicyHost(xcontractNumber, input.getCancellationDate(), input.getReason(), input.getNotifications());
		}
		else {
			executeRescueCancellationRequest(input, policy);
			Map<String, Object> argumentsRequest = new HashMap<>();
			argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
			argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
			argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
			Map<String, Object> cancellationRequest = this.pisdR103.executeGetRequestCancellation(argumentsRequest);
			return executeCancelPolicyHostICF3(input,cancellationRequest, policy);
		}
	}

	private ICF3Request buildICF3Request(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy){
		ICF3Request icf3DTORequest = new ICF3Request();
		icf3DTORequest.setNUMCER(input.getContractId());

		Date date = input.getCancellationDate().getTime();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		icf3DTORequest.setFECCANC(dateFormat.format(date));
		icf3DTORequest.setCODMOCA(input.getReason().getId());
		if(input.getNotifications() != null
				&& !input.getNotifications().getContactDetails().isEmpty()
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

		if(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()) != null){
			icf3DTORequest.setNUMPOL(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue())));
		}

		if(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()) != null){
			icf3DTORequest.setPRODRI(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue())));
		}

		if(input.getIsRefund()){
			icf3DTORequest.setINDDEV("S");
		}else{
			icf3DTORequest.setINDDEV("N");
		}

		return icf3DTORequest;
	}

	private EntityOutPolicyCancellationDTO mapICF3Response(InputParametersPolicyCancellationDTO input, ICF3Response icf3Response){
		EntityOutPolicyCancellationDTO output = new EntityOutPolicyCancellationDTO();
		output.setId(icf3Response.getIcmf3s0().getIDCANCE());
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
		InsurerRefundCancellationDTO insurerRefund = new InsurerRefundCancellationDTO();
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
		return Date.from(localdate.atStartOfDay(ZoneId.of("UTC")).toInstant());
	}

	private EntityOutPolicyCancellationDTO executeCancelPolicyHostICF3 (InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy){
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHostICF3 - Start");
		ICF3Request icf3DTORequest = buildICF3Request(input, cancellationRequest, policy);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHostICF3 - ICF3Request: {}", icf3DTORequest);
		ICF3Response icf3Response = this.rbvdR051.executePolicyCancellation(icf3DTORequest);
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHostICF3 - ICF3Response: {}", icf3Response);

		if (icf3Response.getHostAdviceCode() != null) {
			LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHostICF3 - Error at icf3 execution - Host advice code: {}", icf3Response.getHostAdviceCode());
			this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
			return null;
		}
		LOGGER.info("***** RBVDR011Impl - executeCancelPolicyHostICF3 - End");
		return mapICF3Response(input, icf3Response);
	}

	private Map<String, Object> getProductByProductId(String productId) {
		Map<String,Object> arguments = new HashMap<>();
		arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
		return (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
	}

	private boolean isLifeProduct(String businessName){
		return Objects.nonNull(businessName) && (businessName.equals(ConstantsUtil.BUSINESS_NAME_VIDA) || businessName.equals(ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES));
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
		argumentsCommon.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
		argumentsCommon.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
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
				arguments.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getId());
			}
			else if(RBVDProperties.CONTRACT_TYPE_EXTERNAL_ID.getValue().equals(insurerRefundDTO.getPaymentMethod().getContract().getContractType())) {
				arguments.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getNumber());
			}
		}
		else {
			arguments.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), null);
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

	private Boolean executeRescueCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy) {
		LOGGER.info("***** RBVDR011Impl - executeRescueCancellationRequest - executeGetRequestCancellationId ");
		Map<String, Object> responseGetRequestCancellationId = this.pisdR103.executeGetRequestCancellationId();
		LOGGER.info("***** RBVDR011Impl - executeRescueCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
		BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
		Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellationRescue(requestCancellationId, input, policy);
		LOGGER.info("***s** RBVDR011Impl - executeRescueCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
		int isInserted = this.pisdR103.executeSaveRequestCancellationInvestment(argumentsForSaveRequestCancellation);
		LOGGER.info("***** RBVDR011Impl - isInserted: {}", isInserted);
		Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, 1);
		LOGGER.info("***** RBVDR011Impl - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
		int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
		LOGGER.info("***** RBVDR011Impl - isInsertedMov: {}", isInsertedMov);
		return true;
	}
}
