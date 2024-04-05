package com.bbva.rbvd.lib.r011.impl.business;

import com.bbva.apx.exception.business.BusinessException;
import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.library.AbstractLibrary;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.config.PropertiesConf;
import com.bbva.rbvd.lib.r011.impl.service.api.RimacApi;
import com.bbva.rbvd.lib.r011.impl.service.dao.BaseDAO;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF3Connection;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r011.impl.transform.bean.CancellationBean;
import com.bbva.rbvd.lib.r011.impl.transform.map.NotificationMapper;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r305.RBVDR305;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isOpenCancellationRequest;

public class CancellationBusiness extends AbstractLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationBusiness.class);
    private final BaseDAO baseDAO;
    private final ApplicationConfigurationService applicationConfigurationService;
    private final ICF3Connection icf3Connection;
    private final ICR4Connection icr4Connection;
    private final RBVDR311 rbvdR311;
    private final PISDR401 pisdR401;
    private final RBVDR305 rbvdR305;
    private final CancellationRequestImpl cancellationRequestImpl;
    private boolean isCancellationLegacyFlow;
    private CancelationSimulationPayloadBO cancellationSimulationResponse;

    public CancellationBusiness(PISDR103 pisdR103, PISDR100 pisdR100, RBVDR311 rbvdR311, PISDR401 pisdR401, ApplicationConfigurationService applicationConfigurationService,
                                ICF3Connection icf3Connection, ICR4Connection icr4Connection, CancellationRequestImpl cancellationRequestImpl, RBVDR305 rbvdR305) {
        this.baseDAO = new BaseDAO(pisdR103, pisdR100, cancellationRequestImpl, applicationConfigurationService);
        this.applicationConfigurationService = applicationConfigurationService;
        this.icf3Connection = icf3Connection;
        this.icr4Connection = icr4Connection;
        this.rbvdR311 = rbvdR311;
        this.pisdR401 = pisdR401;
        this.rbvdR305 = rbvdR305;
        this.cancellationRequestImpl = cancellationRequestImpl;
    }

    public EntityOutPolicyCancellationDTO cancellationPolicy(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, String policyId, String productCode,
                                                             ICF2Response icf2Response, boolean isRoyal, String massiveProductsParameter){
        LOGGER.info("***** RBVDR011Impl - cancellationPolicy: Policy cancellation start");

        CancellationBean cancellationBean = new CancellationBean(this.pisdR401, applicationConfigurationService);

        // Configuraciones Iniciales del Envío de correos
        PropertiesConf properties = new PropertiesConf(this.applicationConfigurationService);
        Map<Object, String> propertiesEmail = properties.emailProperties();

        // CONSULTA PARA OBTENER DATOS DE LA TABLA DE SOLICITUDES DE CANCELACIÓN
        Map<String, Object> cancellationRequest = getRequestCancellationRequest(input, policy, isRoyal, productCode, icf2Response, massiveProductsParameter);

        String listCancellation = this.applicationConfigurationService.getProperty(RBVDConstants.CANCELLATION_LIST_ENDOSO);
        String[] channelCancellation = listCancellation.split(",");

        String channelCode = input.getChannelId();
        String isChannelEndoso = Arrays.stream(channelCancellation).filter(channel -> channel.equals(channelCode)).findFirst().orElse(null);
        String userCode = input.getUserId();
        String email = cancellationBean.getEmailFromInput(input, null, icf2Response);
        String authorizeReturnFlag = RBVDConstants.TAG_S;

        // CANCELACIÓN DE PRODUCTOS NO ROYAL
        if (!isRoyal) {
            LOGGER.info("***** RBVDR011Impl - cancellationPolicy: No royal rimac cancellation");
            authorizeReturnFlag = executeRimacCancellationTypeNoRoyal(input, icf2Response, isChannelEndoso, userCode, cancellationRequest, email, false);
        }

        // Dependiendo del tipo de cancelación elegimos si ejecutar la icf3 o la icr4
        EntityOutPolicyCancellationDTO out = validateCancellationType(input, cancellationRequest, policy, icf2Response, productCode, authorizeReturnFlag);
        if (out == null) return null;


        if(isRoyal) {
            String statusIdd= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
            HashSet<String> canceledStatus = new HashSet<>(Arrays.asList(RBVDConstants.TAG_ANU, RBVDConstants.TAG_BAJ));
            if (canceledStatus.contains(statusIdd)) {
                this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
                return null;
            }
        }


        // Se inserta un registro en la tabla de movimiento de cancelaciones
        Map<String, Object> requestCancellationMovLast = baseDAO.executeGetRequestCancellationMovLast(input.getContractId());
        // Dato que se saca para enviar en el correo - el id de la secuencia de un seguro en solicitud de cancelacion
        if(requestCancellationMovLast==null){
            return null;
        }
        String requestCancellationId = requestCancellationMovLast.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()).toString();
        LOGGER.info("***** RBVDR011Impl - executePolicyCancellation requestCancellationId: {} *****", requestCancellationId);

        boolean isInsertMovCancel = executeCancellationRequestMov(input, requestCancellationMovLast);

        if(!isCancellationLegacyFlow && !ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT.equals(productCode) && !isInsertMovCancel){
            this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
            return null;
        }

        // Se mapeo el correo del cliente
        email = cancellationBean.getEmailFromInput(input, out, icf2Response);

        // SI ES UN SEGURO NO ROYAL TERMINA EL FLUJO PQ YA EJECUTO LA
        // CANCELACIÓN EN RIMAC Y LA ICF3 Y YA INSERTO EN LA TABLA DE MOV CANCELACIÓN
        if (!isRoyal) {
            return validatePolicy(out, requestCancellationId, input, policy, icf2Response, email, propertiesEmail);
        }

        Double totalDebt = NumberUtils.toDouble(Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_TOTAL_DEBT_AMOUNT.getValue()), "0"));
        Double pendingAmount = NumberUtils.toDouble(Objects.toString(policy.get(RBVDProperties.KEY_REQUEST_CNCL_SETTLE_PENDING_PREMIUM_AMOUNT.getValue()), "0"));

        String statusId = RBVDConstants.TAG_BAJ;
        String movementType = RBVDConstants.MOV_BAJ;

        List<String> receiptStatusList = new ArrayList<>();
        receiptStatusList.add(RBVDConstants.TAG_SINCOBRAR);

        if(Objects.nonNull(out.getStatus()) && RBVDConstants.TAG_REFUND.equals(out.getStatus().getDescription())) {
            receiptStatusList.add(RBVDConstants.TAG_COBRADO);
            statusId = RBVDConstants.TAG_ANU;
            movementType = RBVDConstants.MOV_ANU;
        }

        // INSERTA UN REGISTRO DE MOVIMIENTO EN LA TABLA DE MOVIMIENTOS DE CONTRATO
        baseDAO.executeSaveContractMovement(input, movementType, statusId);

        if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
            // INSERTA UN REGISTRO EN LA TABLA DE CONTRATOS CANCELADOS
            baseDAO.executeSaveContractCancellation(input, email, totalDebt, pendingAmount);

            // ACTUALIZA EL ESTADO DEL RECIBO A BAJA O ANULADO
            baseDAO.executeUpdateReceiptsStatusV2(input, statusId, receiptStatusList);
        }

        LOGGER.info("***** RBVDR011Impl - cancellationPolicy: input - {}", input);
        LOGGER.info("***** RBVDR011Impl - cancellationPolicy: statusId - {}", statusId);
        LOGGER.info("***** RBVDR011Impl - cancellationPolicy: cancellationRequest - {}", cancellationRequest);

        // ACTUALIZA EL ESTADO DEL CONTRATO Y FECHA DE ANULACIÓN EN LA TABLA DE CONTRATOS
        // sI ES PEN O PEB LA FECHA DE ANULACIÓN SE SETEA CON LA QUE LE ENVIAMOS Y SINO SETEA LA FECHA DE ANULACIÓN DEL CONTRATO
        baseDAO.executeUpdateContractStatusAndAnnulationDate(input, statusId, cancellationRequest);

        // ACTUALIZA EL TIPO DE CANCELACIÓN Y LA FECHA DE ANULACIÓN EN LA TABLA DE SOLICITUDES DE CANCELACIÓN
        if (!isCancellationLegacyFlow) {
            baseDAO.executeUpdateCancellationRequest(input, cancellationRequest);
        }

        executeRimacCancellationType(input, policyId, productCode, isChannelEndoso, userCode, cancellationRequest, email);

        validateResponse(out, policyId);
        LOGGER.info("***** RBVDR011Impl - cancellationPolicy - PRODUCTO ROYAL ***** Response: {}", out);



        // Enviar correo por solicitud de cancelación
        int resultEvent = this.rbvdR305.executeSendingEmail(NotificationMapper.buildEmail(input, policy, isRoyal, icf2Response, email,
                requestCancellationId, propertiesEmail));
        LOGGER.info("***** RBVDR011Impl - executePolicyCancellation resultEvent: {} *****", resultEvent);


        LOGGER.info("***** RBVDR011Impl - cancellationPolicy END *****");
        return out;
    }

    private Map<String, Object> getRequestCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal,
                                                              String productCode, ICF2Response icf2Response, String massiveProductsParameter) {

        LOGGER.info("***** RBVDR011Impl - executePolicyCancellation ICF2Response: {} *****", icf2Response);
        if(productCode.equals(ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT)){
            cancellationRequestImpl.executeRescueCancellationRequest(input, policy, isRoyal, massiveProductsParameter);
        }
        if (isCancellationLegacyFlow) {
            HashMap<String, Object> cancellationRequest = new HashMap<>();
            // For legacy flow, request cancellation is the same day as cancellation
            if (cancellationSimulationResponse != null) {
                cancellationRequest.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), cancellationSimulationResponse.getExtornoComision());
                cancellationRequest.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancellationSimulationResponse.getMonto());
            }else if (icf2Response.getIcmf1S2() != null) {
                cancellationRequest.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCOMI());
                cancellationRequest.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCLIE());
            }

            cancellationRequest.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new Timestamp(System.currentTimeMillis()));
            return cancellationRequest;
        }
        return baseDAO.executeGetRequestCancellation(input);
    }

    private EntityOutPolicyCancellationDTO validateCancellationType(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest,
                                                                    Map<String, Object> policy, ICF2Response icf2Response, String productCode, String authorizeReturnFlag) {
        CancellationBean cancellationBean = new CancellationBean(this.pisdR401, applicationConfigurationService);

        if (!END_OF_VALIDATY.name().equals(input.getCancellationType()) || productCode.equals(ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT)) {
            return this.icf3Connection.executeICF3Transaction(input, cancellationRequest, policy, icf2Response, productCode, authorizeReturnFlag);
        }else{
            boolean icr4RespondsOk = this.icr4Connection.executeICR4Transaction(input, this.applicationConfigurationService.getDefaultProperty(RBVDConstants.CONTRACT_STATUS_HOST_END_OF_VALIDITY,"08"));
            if(!icr4RespondsOk) {
                this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
                return null;
            }
        }

        return cancellationBean.mapRetentionResponse(icf2Response.getIcmf1S2().getNUMPOL(), input, input.getCancellationType(), input.getCancellationType(), input.getCancellationDate());
    }

    private EntityOutPolicyCancellationDTO validatePolicy(EntityOutPolicyCancellationDTO out, String requestCancellationId, InputParametersPolicyCancellationDTO input,
                                                          Map<String, Object> policy, ICF2Response icf2Response, String email, Map<Object, String> propertiesEmail) {
        if (CollectionUtils.isEmpty(this.getAdviceList()) || this.getAdviceList().get(0).getCode().equals(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode())) {
            LOGGER.info("***** RBVDR011Impl - validatePolicy - PRODUCTO NO ROYAL - Response = {} *****", out);
            this.getAdviceList().clear();

            // Enviar correo por solicitud de cancelación
            int resultEvent = this.rbvdR305.executeSendingEmail(NotificationMapper.buildEmail(input, policy, false, icf2Response, email,
                    requestCancellationId, propertiesEmail));
            LOGGER.info("***** RBVDR011Impl - executePolicyCancellation resultEvent: {} *****", resultEvent);

            return out;
        }
        return null;
    }

    private boolean executeCancellationRequestMov(InputParametersPolicyCancellationDTO input, Map<String, Object> requestCancellationMovLast) {
        if (isOpenCancellationRequest(requestCancellationMovLast)) {
            int isInsertedMov = baseDAO.executeSaveInsuranceRequestCancellationMov(requestCancellationMovLast, input);
            return isInsertedMov == 1;
        }
        return false;
    }

    private String executeRimacCancellationTypeNoRoyal(InputParametersPolicyCancellationDTO input, ICF2Response icf2Response, String isChannelEndoso, String userCode, Map<String, Object> cancellationRequest, String email, boolean isRoyal){
        RimacApi rimacApi = new RimacApi(this.rbvdR311, this.applicationConfigurationService);

        if (icf2Response == null || icf2Response.getIcmf1S2() == null || icf2Response.getIcmf1S2().getPRODRI() == null || icf2Response.getIcmf1S2().getNUMPOL() == null){
            LOGGER.info("***** RBVDR011Impl - executeRimacCancellationType - icf2Response has missing data {}", icf2Response);
            this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_SIMULATECANCELATION.getAdviceCode());
            return null;
        }

        String policyId = icf2Response.getIcmf1S2().getNUMPOL();
        String productCode = icf2Response.getIcmf1S2().getPRODRI();

        PolicyCancellationPayloadBO responseCancellationRimac = rimacApi.executeRimacCancellation(input, policyId, productCode, isChannelEndoso, userCode, cancellationRequest, email, isRoyal, icf2Response);

        String authorizationFlag = getAuthorizeReturnFlag(responseCancellationRimac, policyId);

        if (authorizationFlag == null) {
            this.addAdvice(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_RIMAC.getAdviceCode()); // RBVD00000134
        }

        return authorizationFlag;
    }

    private String executeRimacCancellationType(InputParametersPolicyCancellationDTO input, String policyId, String productCode, String isChannelEndoso, String userCode, Map<String, Object> cancellationRequest, String email) {
        RimacApi rimacApi = new RimacApi(this.rbvdR311, this.applicationConfigurationService);

        try {
            if(ConstantsUtil.BUSINESS_NAME_FAKE_INVESTMENT.equals(productCode)){
                rimacApi.executeRescueCancellationRimac(input,policyId, productCode);
                return null;
            }

            PolicyCancellationPayloadBO responseCancellationRimac = rimacApi.executeRimacCancellation(input, policyId, productCode, isChannelEndoso, userCode, cancellationRequest, email, true, null);

            return getAuthorizeReturnFlag(responseCancellationRimac, policyId);
        } catch (BusinessException exception) {
            this.addAdviceWithDescription(exception.getAdviceCode(), exception.getMessage());
            return null;
        }
    }

    private String getAuthorizeReturnFlag(PolicyCancellationPayloadBO policyCancellationPayloadBO, String policyId){
        if (policyCancellationPayloadBO == null) {
            LOGGER.info("***** RBVDR011Impl - cancellationPolicy: Policy {} could not be cancelled by Rimac. Response was null", policyId);
            return null;
        }

        return policyCancellationPayloadBO.getAutorizarRetiro();
    }

    private void validateResponse(EntityOutPolicyCancellationDTO out, String policyId) {
        if (out.getId() == null) {
            out.setId(policyId);
        }
    }

    public void setCancellationLegacyFlow(boolean cancellationLegacyFlow) {
        isCancellationLegacyFlow = cancellationLegacyFlow;
    }
    public void setCancellationSimulationResponse(CancelationSimulationPayloadBO cancellationSimulationResponse) {
        this.cancellationSimulationResponse = cancellationSimulationResponse;
    }
}
