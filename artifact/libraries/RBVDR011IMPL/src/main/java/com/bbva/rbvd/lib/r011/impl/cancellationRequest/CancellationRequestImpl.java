package com.bbva.rbvd.lib.r011.impl.cancellationRequest;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

import static java.util.Objects.nonNull;

public class CancellationRequestImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationRequestImpl.class);
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String PENDING_CANCELLATION_STATUS = "01";
    public static final String RETAINED_INSURANCE_STATUS = "02";
    protected RBVDR311 rbvdR311;
    protected PISDR103 pisdR103;
    protected PISDR100 pisdR100;
    protected ICR4Connection icr4Connection;
    protected ApplicationConfigurationService applicationConfigurationService;
    private static final String CONTRACT_STATUS_HOST_CANCELLATION_REQUEST = "contract.status.host.cancellation.request";

    public boolean validateNewCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
                                                   boolean isRoyal){

        //validar si se tiene registrada una solicitud de cancelación pendiente
        return isRoyal? validateNewCancellationRequestRoyal(input, policy) : validateNewCancellationRequestNoRoyal(input);
    }

    private boolean validateNewCancellationRequestRoyal(InputParametersPolicyCancellationDTO input, Map<String, Object> policy) {
        if (policy != null && isActiveStatusId(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()))) {
            Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
            LOGGER.info("***** RBVDR011Impl - validateNewCancellationRequestRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
            return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
        }
        return false;
    }

    private boolean isActiveStatusId(Object statusId) {
        return statusId != null && !RBVDConstants.TAG_ANU.equals(statusId.toString()) && !RBVDConstants.TAG_BAJ.equals(statusId.toString());
    }
    private boolean validateNewCancellationRequestNoRoyal(InputParametersPolicyCancellationDTO input) {
        Map<String, Object> requestCancellationMovLast = executeGetRequestCancellationMovLast(input.getContractId());
        LOGGER.info("***** RBVDR011Impl - validateNewCancellationRequestNoRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
        return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
    }

    private Map<String, Object> executeGetRequestCancellationMovLast(String contractId) {
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

    public boolean isOpenOrNotExistsCancellationRequest(Map<String, Object> requestCancellationMovLast) {
        if (requestCancellationMovLast != null) {
            String statusId = requestCancellationMovLast.get(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue()).toString();
            LOGGER.info("***** RBVDR011Impl - isOpenCancellationRequest - requestCancellationMovLast -> statusId: {}", statusId);
            return PENDING_CANCELLATION_STATUS.equals(statusId);
        }
        return true;
    }

    public boolean isRetainedOrNotExistsCancellationRequest(Map<String, Object> requestCancellationMovLast) {
        if (requestCancellationMovLast != null) {
            String statusId = requestCancellationMovLast.get(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue()).toString();
            LOGGER.info("***** RBVDR011Impl - isRetainedCancellationRequest - requestCancellationMovLast -> statusId: {}", statusId);
            return RETAINED_INSURANCE_STATUS.equals(statusId);
        }
        return true;
    }

    private void updateContractStatusToPending(InputParametersPolicyCancellationDTO input){
        Map<String, Object> arguments = RBVDUtils.getMapContractNumber(input.getContractId());
        arguments.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_PEN);
        arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(DATE_FORMAT).format(input.getCancellationDate().getTime()));
        pisdR100.executeUpdateContractStatus(arguments);
        LOGGER.info("***** RBVDR011Impl - updateContractStatusToPending - newContractStatus: {}", RBVDConstants.TAG_PEN);
    }
    public boolean executeFirstCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response,
                                                          String policyId, String productCodeForRimac) {
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - begin");
        if (!this.icr4Connection.executeICR4Transaction(input, this.applicationConfigurationService.getDefaultProperty(CONTRACT_STATUS_HOST_CANCELLATION_REQUEST,"PS"))) return false;

        CancelationSimulationPayloadBO cancellationSimulationResponse = null;

        if(isRoyal){
            InputRimacBO rimacSimulationRequest = buildRimacSimulationRequest(input, policyId, productCodeForRimac);
            cancellationSimulationResponse = rbvdR311.executeSimulateCancelationRimac(rimacSimulationRequest);
            if(cancellationSimulationResponse == null) return false;
        }

        Map<String, Object> responseGetRequestCancellationId = pisdR103.executeGetRequestCancellationId();
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
        BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());
        Map<String, Object> argumentsForSaveRequestCancellation = mapRequestCancellationArguments(requestCancellationId, input, policy, cancellationSimulationResponse, icf2Response, isRoyal);
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
        int isInserted = pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInserted: {}", isInserted);
        Map<String, Object> argumentsForSaveRequestCancellationMov = mapInRequestCancellationMov(requestCancellationId, input, PENDING_CANCELLATION_STATUS, 1);
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
        int isInsertedMov = pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
        LOGGER.info("***** RBVDR011Impl - executeFirstCancellationRequest - isInsertedMov: {}", isInsertedMov);

        if(isRoyal) updateContractStatusToPending(input);

        return true;
    }

    private Map<String, Object> mapRequestCancellationArguments(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
                                                         CancelationSimulationPayloadBO cancellationSimulationResponse, ICF2Response icf2Response, boolean isRoyal) {
        Map<String, Object> commonArguments = mapInRequestCancellationCommonFields(requestCancellationId, input);
        Map<String, Object> arguments = new HashMap<>(commonArguments);
        if(isRoyal){
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancellationSimulationResponse.getMonto());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));
            if(cancellationSimulationResponse.getExtornoComision() != null){
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), cancellationSimulationResponse.getExtornoComision());
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            }else{
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);
            }
        }else{
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), icf2Response.getIcmf1S2().getCODPROD());
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), null);
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), null);
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCLIE());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), icf2Response.getIcmf1S2().getCODCLI());
            arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCOMI());
            arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
        }

        return arguments;
    }

    private Map<String, Object> mapInRequestCancellationCommonFields(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
        arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
        arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
        arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
        arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
        arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat(DATE_FORMAT).format(input.getCancellationDate().getTime()));
        arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), null);
        arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat("dd/MM/yy").format(input.getCancellationDate().getTime()));
        arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
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
        LOGGER.info("***** RBVDR011Impl - buildRimacSimulationRequest - rimacSimulationRequest : {} *****", rimacSimulationRequest);
        return rimacSimulationRequest;
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
