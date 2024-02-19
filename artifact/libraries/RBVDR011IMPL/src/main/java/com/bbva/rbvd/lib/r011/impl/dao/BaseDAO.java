package com.bbva.rbvd.lib.r011.impl.dao;

import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.rbvd.lib.r011.impl.business.CancellationRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;

public class BaseDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDAO.class);

    private final PISDR103 pisdR103;
    private final PISDR100 pisdR100;
    private final CancellationRequestImpl cancellationRequestImpl;

    public BaseDAO(PISDR103 pisdR103, PISDR100 pisdR100, CancellationRequestImpl cancellationRequestImpl) {
        this.pisdR103 = pisdR103;
        this.pisdR100 = pisdR100;
        this.cancellationRequestImpl = cancellationRequestImpl;
    }

    public Map<String, Object> executeGetRequestCancellation(InputParametersPolicyCancellationDTO input){
        LOGGER.info("[BaseDAO] executeGetRequestCancellation() :: Start");

        Map<String, Object> argumentsRequest = new HashMap<>();
        argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
        argumentsRequest.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
        argumentsRequest.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));

        Map<String, Object> cancellationRequest = this.pisdR103.executeGetRequestCancellation(argumentsRequest);

        LOGGER.info("[BaseDAO] executeGetRequestCancellation() :: End :: cancellationRequest - {}", cancellationRequest);

        return cancellationRequest;
    }

    public void executeSaveContractMovement(InputParametersPolicyCancellationDTO input, String movementType, String statusId){
        LOGGER.info("[BaseDAO] executeSaveContractMovement() :: Start");

        Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
        mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());

        Map<String, Object> arguments = new HashMap<>(mapContract);
        arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue(), input.getBranchId());
        arguments.put(RBVDProperties.KEY_REQUEST_MOVEMENT_TYPE.getValue(), movementType);
        arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), statusId);

        boolean result = this.pisdR100.executeSaveContractMovement(arguments);

        LOGGER.info("[BaseDAO] executeSaveContractMovement() :: End :: result - {}", result);
    }

    public void executeSaveContractCancellation(InputParametersPolicyCancellationDTO input, String email, Double totalDebt, Double pendingAmount){
        LOGGER.info("[BaseDAO] executeSaveContractCancellation() :: Start");

        Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
        mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());

        Map<String, Object> arguments = new HashMap<>(mapContract);
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

        boolean result = this.pisdR100.executeSaveContractCancellation(arguments);

        LOGGER.info("[BaseDAO] executeSaveContractCancellation() :: End :: result - {}", result);
    }

    public void executeUpdateReceiptsStatusV2(InputParametersPolicyCancellationDTO input, String statusId, List<String> receiptStatusList){
        LOGGER.info("[BaseDAO] executeUpdateReceiptsStatusV2() :: Start");

        Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
        mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());

        Map<String, Object> arguments = new HashMap<>(mapContract);
        arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RECEIPT_STATUS_TYPE.getValue(), statusId);
        arguments.put(RBVDConstants.RECEIPT_STATUS_TYPE_LIST, receiptStatusList);

        Integer result = this.pisdR100.executeUpdateReceiptsStatusV2(arguments);

        LOGGER.info("[BaseDAO] executeUpdateReceiptsStatusV2() :: End :: result - {}", result);
    }

    public void executeUpdateContractStatusAndAnnulationDate(InputParametersPolicyCancellationDTO input, String statusId, Map<String, Object> cancellationRequest){
        LOGGER.info("[BaseDAO] executeUpdateContractStatusAndAnnulationDate() :: Start");

        Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
        mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());

        Map<String, Object> arguments = new HashMap<>(mapContract);
        arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), updateContractStatusIfEndOfValidity(input, statusId));
        //actualizar el policy annulation date de acuerdo al tipo de cancelación escogido

        Integer result = this.pisdR100.executeUpdateContractStatusAndAnnulationDate(mappingPolicyAnnulationDate(input, arguments, cancellationRequest));

        LOGGER.info("[BaseDAO] executeUpdateContractStatusAndAnnulationDate() :: End :: result - {}", result);
    }

    public void executeUpdateCancellationRequest(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest){
        LOGGER.info("[BaseDAO] executeUpdateCancellationRequest() :: Start");

        Map<String, Object> arguments = new HashMap<>();
        //actualizar policy annulation date y request status name de la solicitud de acuerdo al tipo de cancelación escogido
        arguments.put(RBVDProperties.FIELD_REQUEST_STATUS_NAME.getValue(), input.getCancellationType());
        arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), cancellationRequest.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()));

        Integer result = this.pisdR103.executeUpdateCancellationRequest(mappingPolicyAnnulationDate(input, arguments, cancellationRequest));


        LOGGER.info("[BaseDAO] executeUpdateCancellationRequest() :: End :: result - {}", result);
    }



    private String updateContractStatusIfEndOfValidity(InputParametersPolicyCancellationDTO input, String statusId) {
        if (END_OF_VALIDATY.name().equals(input.getCancellationType())) {
            return  RBVDConstants.TAG_PEB;
        }
        return statusId;
    }

    private Map<String, Object> mappingPolicyAnnulationDate(InputParametersPolicyCancellationDTO input, Map<String, Object> arguments, Map<String, Object> cancellationRequest){
        if(input.getCancellationType().equals(APPLICATION_DATE.name())) {
            arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(cancellationRequest.get(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue())));
        }else if(input.getCancellationType().equals(END_OF_VALIDATY.name())) {
            arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
        }else{
            arguments.put(RBVDProperties.KEY_RESPONSE_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(new Date()));
        }
        return arguments;
    }

    public int executeSaveInsuranceRequestCancellationMov(Map<String, Object> requestCancellationMovLast, InputParametersPolicyCancellationDTO input){
        LOGGER.info("[BaseDAO] executeSaveInsuranceRequestCancellationMov() :: Start");

        BigDecimal requestCancellationId = new BigDecimal(requestCancellationMovLast.get(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue()).toString());

        String reasonId = (String) requestCancellationMovLast.get(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue());
        input.getReason().setId(reasonId);

        Map<String, Object> argumentsForSaveRequestCancellationMov = this.cancellationRequestImpl.mapInRequestCancellationMov(requestCancellationId, input, RBVDConstants.MOV_BAJ, new Integer(requestCancellationMovLast.get(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue()).toString()) + 1);
        LOGGER.info("***** BaseDAO - argumentsForSaveRequestCancellationMov: {}", argumentsForSaveRequestCancellationMov);

        int isInsertedMov = this.pisdR103.executeSaveInsuranceRequestCancellationMov(argumentsForSaveRequestCancellationMov);

        LOGGER.info("[BaseDAO] executeSaveInsuranceRequestCancellationMov() :: End :: result - {}", isInsertedMov);

        return isInsertedMov;
    }

}
