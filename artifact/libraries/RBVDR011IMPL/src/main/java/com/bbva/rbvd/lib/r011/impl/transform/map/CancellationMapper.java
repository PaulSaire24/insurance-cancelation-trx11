package com.bbva.rbvd.lib.r011.impl.transform.map;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.*;
import static java.util.Objects.nonNull;

public class CancellationMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationMapper.class);

    public static Map<String, Object> mapRequestCancellationArguments(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Map<String, Object> policy,
                                                                CancelationSimulationPayloadBO cancellationSimulationResponse, ICF2Response icf2Response, boolean isRoyal,
                                                                String massiveProductsParameter, String defaultProductId) {

        Map<String, Object> commonArguments = mapInRequestCancellationCommonFields(requestCancellationId, input, isRoyal, policy, massiveProductsParameter);

        Map<String, Object> arguments = new HashMap<>(commonArguments);

        if (!isStartDateTodayOrAfterToday(isRoyal, policy)) {
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()));
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()));
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), cancellationSimulationResponse.getMonto());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()));

            if (cancellationSimulationResponse.getExtornoComision() != null) {
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), cancellationSimulationResponse.getExtornoComision());
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), cancellationSimulationResponse.getMoneda());
            } else {
                arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
                arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), null);
            }

        } else {
            Object productId = Optional.ofNullable(policy).map(x -> x.getOrDefault(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), defaultProductId)).orElse(defaultProductId);
            arguments.put(RBVDProperties.FIELD_INSURANCE_PRODUCT_ID.getValue(), productId);
            arguments.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), icf2Response.getIcmf1S2().getPLANELE());
            arguments.put(RBVDProperties.FIELD_POLICY_ID.getValue(), icf2Response.getIcmf1S2().getNUMPOL());
            arguments.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCLIE());
            arguments.put(RBVDProperties.FIELD_PREMIUM_CURRENCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
            arguments.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), icf2Response.getIcmf1S2().getCODCLI());
            arguments.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), icf2Response.getIcmf1S2().getIMPCOMI());
            arguments.put(RBVDProperties.FIELD_INSRC_CO_RTURN_AMOUNT_CCY_ID.getValue(), icf2Response.getIcmf1S2().getDIVIMC());
        }

        arguments.put(RBVDProperties.FIELD_REQUEST_STATUS_NAME.getValue(), input.getCancellationType());
        arguments.put(RBVDProperties.FIELD_REQUEST_TYPE.getValue(), "000");

        return arguments;
    }

    public static Map<String, Object> mapInRequestCancellationRescue(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, Boolean isRoyal, Map<String, Object> policyInvestment, String massiveProductsParameter) {
        Map<String, Object> argumentsInvestment = CancellationMapper.mapInRequestCancellationCommonFields(requestCancellationId, input,isRoyal ,policyInvestment, massiveProductsParameter);
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

    public static Map<String, Object> mapInRequestCancellationMov(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, String statusId, Integer requestCancellationMov) {
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

    public static Map<String, Object> mapInRequestCancellationCommonFields(BigDecimal requestCancellationId, InputParametersPolicyCancellationDTO input, boolean isRoyal, Map<String, Object> policy, String massiveProductsParameter) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), requestCancellationId);
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), input.getContractId().substring(0, 4));
        arguments.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), input.getContractId().substring(4, 8));
        arguments.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), input.getContractId().substring(10));
        arguments.put(RBVDProperties.FIELD_CONTRACT_FIRST_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(8, 9));
        arguments.put(RBVDProperties.FIELD_CONTRACT_SECOND_VERFN_DIGIT_ID.getValue(), input.getContractId().substring(9, 10));
        arguments.put(RBVDProperties.FIELD_CHANNEL_ID.getValue(), input.getChannelId());
        arguments.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(new Date()));
        arguments.put(RBVDProperties.FIELD_CREATION_USER_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.FIELD_USER_AUDIT_ID.getValue(), input.getUserId());
        arguments.put(RBVDProperties.FIELD_RL_ACCOUNT_ID.getValue(), obtainInsurerRefundAccountOrCard(input));

        arguments.put(RBVDProperties.FIELD_CANCEL_BRANCH_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_PAYMENT_FREQUENCY_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_COLECTIVE_CERTIFICATE_ID.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CONTACT_EMAIL_DESC.getValue(), null);
        arguments.put(RBVDProperties.FIELD_CUSTOMER_PHONE_DESC.getValue(), null);

        if (input.getCancellationType().equals(END_OF_VALIDATY.name())) {
            arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_REQUEST_ANNULATION_DATE).format(input.getCancellationDate().getTime()));
        } else {
            arguments.put(RBVDProperties.FIELD_POLICY_ANNULATION_DATE.getValue(), new SimpleDateFormat(RBVDConstants.DATEFORMAT_REQUEST_ANNULATION_DATE).format(new Date()));
        }

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

        return arguments;
    }
}
