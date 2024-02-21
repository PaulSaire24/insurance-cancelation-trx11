package com.bbva.rbvd.lib.r011.impl.business;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.library.AbstractLibrary;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.dao.BaseDAO;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF3Connection;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r311.RBVDR311;
import com.google.common.base.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isOpenCancellationRequest;

public class CancellationBusiness extends AbstractLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationBusiness.class);
    private final BaseDAO baseDAO;
    private final ApplicationConfigurationService applicationConfigurationService;
    private final ICF3Connection icf3Connection;
    private final ICR4Connection icr4Connection;
    private final RBVDR311 rbvdR311;
    private final CancellationRequestImpl cancellationRequestImpl;

    public CancellationBusiness(PISDR103 pisdR103, PISDR100 pisdR100, RBVDR311 rbvdR311, ApplicationConfigurationService applicationConfigurationService,
                                ICF3Connection icf3Connection, ICR4Connection icr4Connection, CancellationRequestImpl cancellationRequestImpl) {
        this.baseDAO = new BaseDAO(pisdR103, pisdR100, cancellationRequestImpl);
        this.applicationConfigurationService = applicationConfigurationService;
        this.icf3Connection = icf3Connection;
        this.icr4Connection = icr4Connection;
        this.rbvdR311 = rbvdR311;
        this.cancellationRequestImpl = cancellationRequestImpl;
    }

    public EntityOutPolicyCancellationDTO cancellationPolicy(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, String policyId, String productCode,
                                                             ICF2Response icf2Response, boolean isRoyal){
        LOGGER.info("***** RBVDR011Impl - cancelPolicy: Policy cancellation start");

        // CONSULTA PARA OBTENER DATOS DE LA TABLA DE SOLICITUDES DE CANCELACIÓN
        Map<String, Object> cancellationRequest = baseDAO.executeGetRequestCancellation(input);

        EntityOutPolicyCancellationDTO out = validateCancellationType(input, cancellationRequest, policy, icf2Response);
        if (out == null) return null;
        if (!isRoyal) return validatePolicy(out);

        String statusid= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
        if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
            this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
            return null;
        }

        if(!executeCancellationRequestMov(input)){
            this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
            return null;
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
        String email = "";

        // INSERTA UN REGISTRO DE MOVIMIENTO EN LA TABLA DE MOVIMIENTOS DE CONTRATO
        baseDAO.executeSaveContractMovement(input, movementType, statusId);

        if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().get(0).getContact() != null) {
            email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
        }

        if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
            // INSERTA UN REGISTRO EN LA TABLA DE CONTRATOS CANCELADOS
            baseDAO.executeSaveContractCancellation(input, email, totalDebt, pendingAmount);

            // ACTUALIZA EL ESTADO DEL RECIBO A BAJA O ANULADO
            baseDAO.executeUpdateReceiptsStatusV2(input, statusId, receiptStatusList);
        }

        LOGGER.info("***** RBVDR011Impl - cancelPolicy: input - {}", input);
        LOGGER.info("***** RBVDR011Impl - cancelPolicy: statusId - {}", statusId);
        LOGGER.info("***** RBVDR011Impl - cancelPolicy: cancellationRequest - {}", cancellationRequest);

        // ACTUALIZA EL ESTADO DEL CONTRATO Y FECHA DE ANULACIÓN EN LA TABLA DE CONTRATOS
        // sI ES PEN O PEB LA FECHA DE ANULACIÓN SE SETEA CON LA QUE LE ENVIAMOS Y SINO SETEA LA FECHA DE ANULACIÓN DEL CONTRATO
        baseDAO.executeUpdateContractStatusAndAnnulationDate(input, statusId, cancellationRequest);

        // ACTUALIZA EL TIPO DE CANCELACIÓN Y LA FECHA DE ANULACIÓN EN LA TABLA DE SOLICITUDES DE CANCELACIÓN
        baseDAO.executeUpdateCancellationRequest(input, cancellationRequest);

        String listCancellation = this.applicationConfigurationService.getProperty(RBVDConstants.CANCELLATION_LIST_ENDOSO);

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

    private EntityOutPolicyCancellationDTO validateCancellationType(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest,
                                                                    Map<String, Object> policy, ICF2Response icf2Response)
    {
        //if(input.getCancellationType().equals(APPLICATION_DATE.name()) &&
        //        policy != null && validateMassiveProduct(policy, applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,","))) {
        //    input.setCancellationType(END_OF_VALIDATY.name());
        //}

        if (!END_OF_VALIDATY.name().equals(input.getCancellationType())) {
            return this.icf3Connection.executeICF3Transaction(input, cancellationRequest, policy, icf2Response);
        }else{
            boolean icr4RespondsOk = this.icr4Connection.executeICR4Transaction(input, this.applicationConfigurationService.getDefaultProperty(RBVDConstants.CONTRACT_STATUS_HOST_END_OF_VALIDITY,"08"));
            if(!icr4RespondsOk) {
                this.addAdvice(RBVDErrors.ERROR_CICS_CONNECTION.getAdviceCode());
                return null;
            }
        }

        return mapRetentionResponse(null, input, input.getCancellationType(), input.getCancellationType(), input.getCancellationDate());
    }

    private EntityOutPolicyCancellationDTO validatePolicy(EntityOutPolicyCancellationDTO out) {
        if (CollectionUtils.isEmpty(this.getAdviceList()) || this.getAdviceList().get(0).getCode().equals(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode())) {
            LOGGER.info("***** RBVDR011Impl - validatePolicy - PRODUCTO NO ROYAL - Response = {} *****", out);
            this.getAdviceList().clear();
            return out;
        }
        return null;
    }

    private boolean executeCancellationRequestMov(InputParametersPolicyCancellationDTO input) {
        Map<String, Object> requestCancellationMovLast = cancellationRequestImpl.executeGetRequestCancellationMovLast(input.getContractId());
        if (isOpenCancellationRequest(requestCancellationMovLast)) {
            int isInsertedMov = baseDAO.executeSaveInsuranceRequestCancellationMov(requestCancellationMovLast, input);
            return isInsertedMov == 1;
        }
        return false;
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

    private void validateResponse(EntityOutPolicyCancellationDTO out, String policyId) {
        if (out.getId() == null) {
            out.setId(policyId);
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

    public boolean validateMassiveProduct(Map<String, Object> policy, String massiveProductsParameter){
        String[] massiveProductsList = massiveProductsParameter.split(",");
        String massiveProduct = Arrays.stream(massiveProductsList).filter(product ->
                product.equals(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()))).findFirst().orElse(null);
        return massiveProduct != null;
    }
}
