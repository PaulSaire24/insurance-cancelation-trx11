package com.bbva.rbvd.lib.r011.impl.business;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isActiveStatusId;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isRetainedOrNotExistsCancellationRequest;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.validateDaysOfRightToRepent;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isStartDateTodayOrAfterToday;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.obtainInsurerRefundAccountOrCard;
import static java.util.Objects.nonNull;

public class CancellationRequestImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationRequestImpl.class);
    protected RBVDR311 rbvdR311;
    protected PISDR103 pisdR103;
    protected PISDR100 pisdR100;
    protected ICR4Connection icr4Connection;
    protected ApplicationConfigurationService applicationConfigurationService;

    public boolean validateNewCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
                                                  boolean isRoyal){

        //validar si se tiene registrada una solicitud de cancelación pendiente
        return isRoyal? validateNewCancellationRequestRoyal(input, policy) : validateNewCancellationRequestNoRoyal(input);
    }

    private boolean validateNewCancellationRequestRoyal(InputParametersPolicyCancellationDTO input, Map<String, Object> policy) {
        if (policy != null && isActiveStatusId(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()))) {
            Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
            LOGGER.info("***** CancellationRequestImpl - validateNewCancellationRequestRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
            return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
        }
        return false;
    }


    private boolean validateNewCancellationRequestNoRoyal(InputParametersPolicyCancellationDTO input) {
        Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
        LOGGER.info("***** CancellationRequestImpl - validateNewCancellationRequestNoRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
        return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
    }

    public Map<String, Object> executeGetRequestCancellationMovLast(String contractId) {
        Map<String, Object> argumentsRequest = new HashMap<>();
        argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), contractId.substring(0, 4));
        argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), contractId.substring(4, 8));
        argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), contractId.substring(10));
        List<Map<String, Object>> requestCancellationMovLast = pisdR103.executeGetRequestCancellationMovLast(argumentsRequest);
        if (requestCancellationMovLast != null && !requestCancellationMovLast.isEmpty()) {
            return requestCancellationMovLast.get(requestCancellationMovLast.size()-1);
        }
        return null;
    }

    private void updateContractStatusToPendingAndPolicyAnnulationDate(InputParametersPolicyCancellationDTO input, Map<String, Object> policy){
        Map<String, Object> arguments = RBVDUtils.getMapContractNumber(input.getContractId());
        arguments.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_PEN);
        if(input.getCancellationType().equals(END_OF_VALIDATY.name()) || (validateMassiveProduct(policy,  applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST, ",")) && !validateDaysOfRightToRepent(policy))) {
            arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
        }
        else arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(new Date()));
        pisdR100.executeUpdateContractStatusAndAnnulationDate(arguments);
        LOGGER.info("***** CancellationRequestImpl - updateContractStatusToPendingAndPolicyAnnulationDate - newContractStatus: {}", RBVDConstants.TAG_PEN);
    }
    public boolean executeFirstCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response,
                                                   String policyId, String productCodeForRimac) {
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - begin");
        if (!this.icr4Connection.executeICR4Transaction(input, this.applicationConfigurationService.getDefaultProperty(RBVDConstants.CONTRACT_STATUS_HOST_CANCELLATION_REQUEST,RBVDConstants.TAG_CANCELLATION_PENDING_HOST_STATUS))) return false;

        CancelationSimulationPayloadBO cancellationSimulationResponse = null;

        if(!isStartDateTodayOrAfterToday(isRoyal, policy)){
            InputRimacBO rimacSimulationRequest = buildRimacSimulationRequest(input, policyId, productCodeForRimac);
            cancellationSimulationResponse = rbvdR311.executeSimulateCancelationRimac(rimacSimulationRequest);
            if(cancellationSimulationResponse == null) return false;
            cancellationSimulationResponse.setMoneda(conversor(cancellationSimulationResponse.getMoneda()));
        }

        Map<String, Object> responseGetRequestCancellationId = pisdR103.executeGetRequestCancellationId();
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
        BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
        Map<String, Object> argumentsForSaveRequestCancellation = mapRequestCancellationArguments(requestCancellationId, input, policy, cancellationSimulationResponse, icf2Response, isRoyal);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
        int isInserted = pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - isInserted: {}", isInserted);
        Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_PEN, 1);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
        int isInsertedMov = pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - isInsertedMov: {}", isInsertedMov);

        if(isRoyal) updateContractStatusToPendingAndPolicyAnnulationDate(input, policy);

        return true;
    }
    private Map<String, Object> mapInRequestCancellationRescue(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Boolean isRoyal, Map<String, Object> policyInvestment) {
        Map<String, Object> argumentsInvestment = mapInRequestCancellationCommonFields(requestCancellationId, input,isRoyal ,policyInvestment);
        argumentsInvestment.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policyInvestment.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));
        argumentsInvestment.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policyInvestment.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
        argumentsInvestment.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policyInvestment.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
        argumentsInvestment.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policyInvestment.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
        argumentsInvestment.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), null);
        argumentsInvestment.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), null);
        argumentsInvestment.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
        argumentsInvestment.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);
        InsurerRefundCancellationDTO insurerRefundDTO = input.getInsurerRefund();
        if (nonNull(insurerRefundDTO)) {
            if (RBVDProperties.CONTRACT_TYPE_INTERNAL_ID.getValue().equals(insurerRefundDTO.getPaymentMethod().getContract().getContractType())) {
                argumentsInvestment.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getId());
            }
            else if(RBVDProperties.CONTRACT_TYPE_EXTERNAL_ID.getValue().equals(insurerRefundDTO.getPaymentMethod().getContract().getContractType())) {
                argumentsInvestment.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), insurerRefundDTO.getPaymentMethod().getContract().getNumber());
            }
        }
        else {
            argumentsInvestment.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), null);
        }
        argumentsInvestment.put(RBVDProperties.FIELD_REQUEST_TYPE.getValue(), "001");
        return argumentsInvestment;
    }

    public boolean executeRescueCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, Boolean isRoyal) {
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - begin");
        Map<String, Object> responseGetRequestCancellationId = pisdR103.executeGetRequestCancellationId();
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
        BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
        Map<String, Object> argumentsForSaveRequestCancellation = mapInRequestCancellationRescue(requestCancellationId, input, isRoyal, policy);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
        int isInserted = pisdR103.executeSaveRequestCancellationInvestment(argumentsForSaveRequestCancellation);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - isInserted: {}", isInserted);
        Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, 1);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
        int isInsertedMov = pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - isInsertedMov: {}", isInsertedMov);
        return true;
    }

    private Map<String, Object> mapRequestCancellationArguments(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
                                                                CancelationSimulationPayloadBO cancellationSimulationResponse, ICF2Response icf2Response, boolean isRoyal) {
        Map<String, Object> commonArguments = mapInRequestCancellationCommonFields(requestCancellationId, input, isRoyal, policy);
        Map<String, Object> arguments = new HashMap<>(commonArguments);
        if(!isStartDateTodayOrAfterToday(isRoyal, policy)){
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancellationSimulationResponse.getMonto());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_REQUEST_TYPE.getValue(), "000");

            if(cancellationSimulationResponse.getExtornoComision() != null){
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), cancellationSimulationResponse.getExtornoComision());
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            }else{
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);
            }
            if(input.getCancellationType().equals(END_OF_VALIDATY.name()) || input.getCancellationType().equals(APPLICATION_DATE.name()) && validateMassiveProduct(policy,  applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",")) && !validateDaysOfRightToRepent(policy)) {
                arguments.put(RBVDProperties.FIELD_REQUEST_STATUS_NAME.getValue(), END_OF_VALIDATY.name());
            }else arguments.put(RBVDProperties.FIELD_REQUEST_STATUS_NAME.getValue(), input.getCancellationType());
        }else{
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), icf2Response.getIcmf1S2().getCODPROD());
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), null);
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), null);
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCLIE());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), icf2Response.getIcmf1S2().getCODCLI());
            arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCOMI());
            arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
            arguments.put(RBVDProperties.FIELD_REQUEST_STATUS_NAME.getValue(), input.getCancellationType());
            arguments.put(RBVDProperties.FIELD_REQUEST_TYPE.getValue(), "000");
        }

        return arguments;
    }

    private Map<String, Object> mapInRequestCancellationCommonFields(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, boolean isRoyal, Map<String, Object> policy){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
        arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
        arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
        arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
        arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
        arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(new Date()));
        arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), null);
        if(input.getCancellationType().equals(END_OF_VALIDATY.name()) || (isRoyal && input.getCancellationType().equals(APPLICATION_DATE.name()) && validateMassiveProduct(policy,  applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",")) && !validateDaysOfRightToRepent(policy))) {
            arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_REQUEST_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
        }else arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_REQUEST_ANNULATION_DATE).format(new Date()));
        arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
        NotificationsDTO notificationsDTO = input.getNotifications();
        if (nonNull(notificationsDTO)) {
            notificationsDTO.getContactDetails().stream().forEach(contactDetailDTO -> {
                if (RBVDProperties.CONTACT_EMAIL_ID.getValue().equals(contactDetailDTO.getContact().getContactDetailType())) {
                    arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), contactDetailDTO.getContact().getAddress());
                }
                if (RBVDProperties.CONTACT_MOBILE_ID.getValue().equals(contactDetailDTO.getContact().getContactDetailType())) {
                    arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), contactDetailDTO.getContact().getNumber());
                }
            });
        }
        arguments.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), obtainInsurerRefundAccountOrCard(input));

        return arguments;
    }

    public Map<String, Object> mapInRequestCancellationMov(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, String statusId, Integer requestCancellationMov) {
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

    private InputRimacBO buildRimacSimulationRequest(InputParametersPolicyCancellationDTO input, String policyId, String productCodeForRimac){
        LOGGER.info("***** RBVDR011Impl - buildRimacSimulationRequest - Begin *****");

        InputRimacBO rimacSimulationRequest = new InputRimacBO();
        rimacSimulationRequest.setTraceId(input.getTraceId());
        ZoneId zone = ZoneId.of("UTC");
        java.time.LocalDate cancellationDate = input.getCancellationDate().toInstant().atZone(zone).toLocalDate();
        rimacSimulationRequest.setFechaAnulacion(cancellationDate);
        rimacSimulationRequest.setNumeroPoliza(Integer.parseInt(policyId));
        rimacSimulationRequest.setCodProducto(productCodeForRimac);
        LOGGER.info("***** CancellationRequestImpl - buildRimacSimulationRequest - rimacSimulationRequest : {} *****", rimacSimulationRequest);
        return rimacSimulationRequest;
    }

    public boolean validateMassiveProduct(Map<String, Object> policy, String massiveProductsParameter){
        String[] massiveProductsList = massiveProductsParameter.split(",");
        String massiveProduct = Arrays.stream(massiveProductsList).filter(product ->
                product.equals(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()))).findFirst().orElse(null);
        return massiveProduct != null;
    }
    public String conversor(String currency) {
        if(currency.equalsIgnoreCase(RBVDConstants.CURRENCY_SOL)) {
            currency = ConstantsUtil.CURRENCY_PEN;
        }
        return currency;
    }

    public void setRbvdR311(RBVDR311 rbvdR311){
        this.rbvdR311 = rbvdR311;
    }

    public void setPisdR103(PISDR103 pisdR103){
        this.pisdR103 = pisdR103;
    }

    public void setPisdR100(PISDR100 pisdR100){
        this.pisdR100 = pisdR100;
    }

    public void setIcr4Connection(ICR4Connection icr4Connection){
        this.icr4Connection = icr4Connection;
    }
    public void setApplicationConfigurationService(ApplicationConfigurationService applicationConfigurationService) {this.applicationConfigurationService = applicationConfigurationService;}
}
