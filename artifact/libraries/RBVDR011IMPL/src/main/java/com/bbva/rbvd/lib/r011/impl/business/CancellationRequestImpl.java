package com.bbva.rbvd.lib.r011.impl.business;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.config.PropertiesConf;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r011.impl.service.dao.BaseDAO;
import com.bbva.rbvd.lib.r011.impl.transform.bean.CancellationBean;
import com.bbva.rbvd.lib.r011.impl.transform.map.CancellationMapper;
import com.bbva.rbvd.lib.r011.impl.transform.map.NotificationMapper;
import com.bbva.rbvd.lib.r305.RBVDR305;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isActiveStatusId;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.isRetainedOrNotExistsCancellationRequest;

public class CancellationRequestImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationRequestImpl.class);
    protected RBVDR311 rbvdR311;
    protected PISDR103 pisdR103;
    protected PISDR100 pisdR100;
    protected RBVDR305 rbvdR305;
    protected ICR4Connection icr4Connection;
    protected ApplicationConfigurationService applicationConfigurationService;

    public boolean validateNewCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal){
        BaseDAO baseDAO = new BaseDAO(pisdR103, pisdR100, new CancellationRequestImpl(), applicationConfigurationService);
        //validar si se tiene registrada una solicitud de cancelación pendiente
        return isRoyal? validateNewCancellationRequestRoyal(input, policy, baseDAO) : validateNewCancellationRequestNoRoyal(input, baseDAO);
    }

    private boolean validateNewCancellationRequestRoyal(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, BaseDAO baseDAO) {
        if (policy != null && isActiveStatusId(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()))) {
            Map<String, Object> requestCancellationMovLast = baseDAO.executeGetRequestCancellationMovLast(input.getContractId());
            LOGGER.info("***** CancellationRequestImpl - validateNewCancellationRequestRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
            return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
        }
        return false;
    }

    private boolean validateNewCancellationRequestNoRoyal(InputParametersPolicyCancellationDTO input, BaseDAO baseDAO) {
        Map<String, Object> requestCancellationMovLast = baseDAO.executeGetRequestCancellationMovLast(input.getContractId());
        LOGGER.info("***** CancellationRequestImpl - validateNewCancellationRequestNoRoyal - requestCancellationMovLast: {}", requestCancellationMovLast);
        return isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
    }

    public boolean executeFirstCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response, CancelationSimulationPayloadBO cancellationSimulationResponse, String massiveProductsParameter, String email) {
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - begin");

        boolean result = true;
        BaseDAO baseDAO = new BaseDAO(pisdR103, pisdR100, new CancellationRequestImpl(), applicationConfigurationService);

        // Valor del estado del contrato en Host -> PS
        String status = this.applicationConfigurationService.getDefaultProperty(RBVDConstants.CONTRACT_STATUS_HOST_CANCELLATION_REQUEST,RBVDConstants.TAG_CANCELLATION_PENDING_HOST_STATUS);
        LOGGER.info("RBVDR011Impl - executeFirstCancellationRequest() - status: {}", status);

        // Valor por defecto del tipo de producto del seguro <no royal>
        String defaultProductId = applicationConfigurationService.getDefaultProperty("cancellation.default.host.productId", RBVDConstants.TAG_0);
        LOGGER.info("RBVDR011Impl - executeFirstCancellationRequest() - defaultProductId: {}", defaultProductId);

        // Marcamos en Host el estado del contrato como pendiente
        if (!this.icr4Connection.executeICR4Transaction(input, status)) {
            result = false;
        }

        // Obtiene la secuencia de la tabla T_PISD_INSURANCE_REQUEST_CNCL
        Map<String, Object> responseGetRequestCancellationId = pisdR103.executeGetRequestCancellationId();
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
        BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());


        // Mapeando campos de la tabla T_PISD_INSURANCE_REQUEST_CNCL
        Map<String, Object> argumentsForSaveRequestCancellation = CancellationMapper.mapRequestCancellationArguments(requestCancellationId, input, policy, cancellationSimulationResponse, icf2Response, isRoyal, massiveProductsParameter, defaultProductId);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);
        // insertando en la tabla T_PISD_INSURANCE_REQUEST_CNCL
        int isInserted = pisdR103.executeSaveInsuranceRequestCancellation(argumentsForSaveRequestCancellation);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - isInserted: {}", isInserted);


        // Mapeando campos de la tabla T_PISD_INSURANCE_REQ_CNCL_MOV
        Map<String, Object> argumentsForSaveRequestCancellationMov = CancellationMapper.mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_PEN, 1);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);
        // insertando en la tabla T_PISD_INSURANCE_REQ_CNCL_MOV
        int isInsertedMov = pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
        LOGGER.info("***** CancellationRequestImpl - executeFirstCancellationRequest - isInsertedMov: {}", isInsertedMov);

        // Si es un seguro royal actualizamos la tabla de contratos
        if(isRoyal) baseDAO.updateContractStatusToPendingAndPolicyAnnulationDate(input, policy);

        PropertiesConf properties = new PropertiesConf(this.applicationConfigurationService);
        Map<Object, String> propertiesEmail = properties.emailProperties();

        // Enviar correo por solicitud de cancelación
        int resultEvent = this.rbvdR305.executeSendingEmail(NotificationMapper.buildEmail(input, policy, isRoyal, icf2Response, email, requestCancellationId.toString(), propertiesEmail));
        LOGGER.info("***** RBVDR011Impl - executePolicyCancellation resultEvent: {} *****", resultEvent);

        return result;
    }

    public boolean executeRescueCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, Boolean isRoyal, String massiveProductsParameter) {
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - begin");
        Map<String, Object> responseGetRequestCancellationId = pisdR103.executeGetRequestCancellationId();
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - responseGetRequestCancellationId: {}", responseGetRequestCancellationId);
        BigDecimal requestCancellationId = (BigDecimal) responseGetRequestCancellationId.get(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue());


        Map<String, Object> argumentsForSaveRequestCancellation = CancellationMapper.mapInRequestCancellationRescue(requestCancellationId, input, isRoyal, policy, massiveProductsParameter);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - argumentsForSaveRequestCancellation: {}", argumentsForSaveRequestCancellation);

        int isInserted = pisdR103.executeSaveRequestCancellationInvestment(argumentsForSaveRequestCancellation);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - isInserted: {}", isInserted);


        Map<String, Object> argumentsForSaveRequestCancellationMov = CancellationMapper.mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, 1);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);

        int isInsertedMov = pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);
        LOGGER.info("***** CancellationRequestImpl - executeRescueCancellationRequest - isInsertedMov: {}", isInsertedMov);
        return true;
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
    public void setApplicationConfigurationService(ApplicationConfigurationService applicationConfigurationService) {
        this.applicationConfigurationService = applicationConfigurationService;
    }

    public void setRbvdR305(RBVDR305 rbvdR305) {
        this.rbvdR305 = rbvdR305;
    }
}
