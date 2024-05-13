package com.bbva.rbvd.lib.r011.impl.transform.map;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.notification.events.NotificationDTO;
import com.bbva.rbvd.dto.notification.events.ReceiverDTO;
import com.bbva.rbvd.dto.notification.events.SendNotificationsDTO;
import com.bbva.rbvd.dto.notification.events.ValueDTO;
import com.bbva.rbvd.dto.notification.utils.DeliveryChannelEnum;
import com.bbva.rbvd.dto.notification.utils.UserTypeEnum;
import com.bbva.rbvd.lib.r011.impl.utils.ConvertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class NotificationMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMapper.class);

    private static final String CUSTOMER_NAME_EMAIL = "name";
    private static final String INSURANCE_NAME_EMAIL = "insuranceName";
    private static final String CANCELLATION_DATE_VALUE_EMAIL = "cancellationDateValue";
    private static final String CANCELLATION_HOUR_VALUE_EMAIL = "cancellationHourValue";
    private static final String APPLICATION_NUMBER_VALUE_EMAIL = "applicationNumberValue";
    private static final String CERTIFICATE_NUMBER_VALUE_EMAIL = "certificateNumberValue";
    private static final String CANCELLATION_REASON_VALUE_EMAIL = "cancellationReasonValue";
    private static final String PLAN_NAME_EMAIL = "planName";
    private static final String CLIENTE_DEFAULT_NAME = "CLIENTE";
    private static final String AMOUNT_RETURNED_EMAIL = "amountReturned";
    private static final String DOMICILIE_ACCOUNT_EMAIL = "domicilieAccount";
    private static final String DATE_FORMAT_HH_MM_SS = "HH:mm:ss";

    private NotificationMapper() {}

    public static SendNotificationsDTO buildEmail(String typeCancellation, InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal,
                                                  ICF2Response icf2Response, String email, String requestCancellationId, Map<Object, String> propertiesEmail,
                                                  Map<String, Object> cancellationRequest, boolean contactEmailTest) {
        LOGGER.info("RBVDR011Impl - buildEmail() - START :: email - {}", email);

        NotificationDTO notification = new NotificationDTO();

        ReceiverDTO receiver = new ReceiverDTO();
        receiver.setUserType(UserTypeEnum.CUSTOMER.getValue());
        receiver.setUserId("PE00150".concat(isRoyal
                ? policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()).toString()
                : icf2Response.getIcmf1S2().getCODCLI()));
        receiver.setRecipientType("TO");
        receiver.setEmail(contactEmailTest ? "SACARBAJAL@BBVA.COM" : email);
        receiver.setContractId(input.getContractId());

        List<ReceiverDTO> receivers = new ArrayList<>();
        receivers.add(receiver);

        List<ValueDTO> values = new ArrayList<>();
        if(typeCancellation.equalsIgnoreCase("REQUEST_CANCELLATION")) {
            values = valuesRequestCancellation(input, policy, isRoyal, icf2Response, requestCancellationId);

            notification.setNotificationTypeId(propertiesEmail.get("notificationTypeRequestCancellationId"));

        } else if (typeCancellation.equalsIgnoreCase("CANCELLATION_IMMEDIATE")) {
            values = valuesCancellationImmediate(input, policy, isRoyal, icf2Response, requestCancellationId, cancellationRequest);

            notification.setNotificationTypeId(propertiesEmail.get("notificationTypeCancellationInmediateId"));

        } else if (typeCancellation.equalsIgnoreCase("CANCELLATION_END_OF_VALIDITY")) {
            values = valuesCancellationEndOfValidity(input, policy, isRoyal, icf2Response, requestCancellationId, cancellationRequest);

            notification.setNotificationTypeId(propertiesEmail.get("notificationTypeCancellationEndOfValidityId"));
        }

        notification.setApplicationCode("RBVD"); //RBVD

        notification.setReceivers(receivers);
        notification.setDeliveryChannel(DeliveryChannelEnum.EMAIL.getValue());
        notification.setValues(values);

        ArrayList<NotificationDTO> notifications = new ArrayList<>();
        notifications.add(notification);

        SendNotificationsDTO sendNotifications = new SendNotificationsDTO();
        sendNotifications.setNotifications(notifications);

        LOGGER.info("RBVDR011Impl - executeFirstCancellationRequest() - sendNotifications: {}", sendNotifications);

        return sendNotifications;
    }

    public static List<ValueDTO> valuesRequestCancellation(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response, String requestCancellationId) {

        List<ValueDTO> values = new ArrayList<>();

        ValueDTO value1 = new ValueDTO();
        value1.setId(CUSTOMER_NAME_EMAIL);

        if(input.getNotifications() != null
                && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().size() > 2
                && input.getNotifications().getContactDetails().get(2).getContact() != null
                && input.getNotifications().getContactDetails().get(2).getContact().getUsername() != null
        ) {
            value1.setName(ConvertUtil.escapeSpecialCharacters(input.getNotifications().getContactDetails().get(2).getContact().getUsername()));
        } else {
            value1.setName(CLIENTE_DEFAULT_NAME);
        }

        ValueDTO value2 = new ValueDTO();
        value2.setId(INSURANCE_NAME_EMAIL);
        value2.setName(ConvertUtil.escapeSpecialCharacters( isRoyal
                ? policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_DESC.getValue()).toString().toUpperCase()
                : icf2Response.getIcmf1S2().getNOMSEGU().toUpperCase()));

        ValueDTO value3 = new ValueDTO();
        value3.setId(PLAN_NAME_EMAIL);
        value3.setName(isRoyal
                ? policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()).toString()
                : icf2Response.getIcmf1S2().getPLANELE());

        Date date = new Date();
        ValueDTO value4 = new ValueDTO();
        value4.setId(CANCELLATION_DATE_VALUE_EMAIL);
        value4.setName(new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD).format(date)); //"05 de marzo del 2024"

        ValueDTO value5 = new ValueDTO();
        value5.setId(CANCELLATION_HOUR_VALUE_EMAIL);
        value5.setName(new SimpleDateFormat(DATE_FORMAT_HH_MM_SS).format(date)); // 11:08:48

        ValueDTO value6 = new ValueDTO();
        value6.setId(APPLICATION_NUMBER_VALUE_EMAIL);
        value6.setName(requestCancellationId);

        ValueDTO value7 = new ValueDTO();
        value7.setId(CERTIFICATE_NUMBER_VALUE_EMAIL);
        value7.setName((input.getContractId().substring(0, 4))
                .concat("-")
                .concat(input.getContractId().substring(4, 8))
                .concat("-")
                .concat(input.getContractId().substring(10))); //"0011-0176-4000188860"

        ValueDTO value8 = new ValueDTO();
        value8.setId(CANCELLATION_REASON_VALUE_EMAIL);
        value8.setName(ConvertUtil.escapeSpecialCharacters(ConvertUtil.convertReasonCancellation(input.getReason().getId())));


        values.add(value1);
        values.add(value2);
        values.add(value3);
        values.add(value4);
        values.add(value5);
        values.add(value6);
        values.add(value7);
        values.add(value8);

        return values;

    }

    public static List<ValueDTO> valuesCancellationImmediate(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response, String requestCancellationId, Map<String, Object> cancellationRequest) {

        List<ValueDTO> values = new ArrayList<>();

        ValueDTO value1 = new ValueDTO();
        value1.setId(CUSTOMER_NAME_EMAIL);

        if(input.getNotifications() != null
                && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().size() > 2
                && input.getNotifications().getContactDetails().get(2).getContact() != null
                && input.getNotifications().getContactDetails().get(2).getContact().getUsername() != null
        ) {
            value1.setName(ConvertUtil.escapeSpecialCharacters(input.getNotifications().getContactDetails().get(2).getContact().getUsername()));
        } else {
            value1.setName(CLIENTE_DEFAULT_NAME);
        }

        ValueDTO value2 = new ValueDTO();
        value2.setId(INSURANCE_NAME_EMAIL);
        value2.setName(ConvertUtil.escapeSpecialCharacters( isRoyal
                ? policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_DESC.getValue()).toString().toUpperCase()
                : icf2Response.getIcmf1S2().getNOMSEGU().toUpperCase()));

        ValueDTO value3 = new ValueDTO();
        value3.setId(PLAN_NAME_EMAIL);
        value3.setName(isRoyal
                ? policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()).toString()
                : icf2Response.getIcmf1S2().getPLANELE());

        ValueDTO value4 = new ValueDTO();
        value4.setId(AMOUNT_RETURNED_EMAIL);
        value4.setName(isRoyal
                ? cancellationRequest.get(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue()).toString()
                : String.valueOf(icf2Response.getIcmf1S2().getIMPCLIE()));

        ValueDTO value5 = new ValueDTO();
        value5.setId(DOMICILIE_ACCOUNT_EMAIL);
        value5.setName(input.getInsurerRefund().getPaymentMethod().getContract().getId()); // 00110130000210499196

        Date date = new Date();
        ValueDTO value6 = new ValueDTO();
        value6.setId(CANCELLATION_DATE_VALUE_EMAIL);
        value6.setName(new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD).format(date)); //"05 de marzo del 2024"

        ValueDTO value7 = new ValueDTO();
        value7.setId(CANCELLATION_HOUR_VALUE_EMAIL);
        value7.setName(new SimpleDateFormat(DATE_FORMAT_HH_MM_SS).format(date)); // 11:08:48

        ValueDTO value8 = new ValueDTO();
        value8.setId(APPLICATION_NUMBER_VALUE_EMAIL);
        value8.setName(requestCancellationId);

        ValueDTO value9 = new ValueDTO();
        value9.setId(CERTIFICATE_NUMBER_VALUE_EMAIL);
        value9.setName((input.getContractId().substring(0, 4))
                .concat("-")
                .concat(input.getContractId().substring(4, 8))
                .concat("-")
                .concat(input.getContractId().substring(10))); //"0011-0176-4000188860"

        ValueDTO value10 = new ValueDTO();
        value10.setId(CANCELLATION_REASON_VALUE_EMAIL);
        value10.setName(ConvertUtil.escapeSpecialCharacters(ConvertUtil.convertReasonCancellation(input.getReason().getId())));


        values.add(value1);
        values.add(value2);
        values.add(value3);
        values.add(value4);
        values.add(value5);
        values.add(value6);
        values.add(value7);
        values.add(value8);
        values.add(value9);
        values.add(value10);

        return values;

    }

    public static List<ValueDTO> valuesCancellationEndOfValidity(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response, String requestCancellationId, Map<String, Object> cancellationRequest) {

        List<ValueDTO> values = new ArrayList<>();

        ValueDTO value1 = new ValueDTO();
        value1.setId(CUSTOMER_NAME_EMAIL);

        if(input.getNotifications() != null
                && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().size() > 2
                && input.getNotifications().getContactDetails().get(2).getContact() != null
                && input.getNotifications().getContactDetails().get(2).getContact().getUsername() != null
        ) {
            value1.setName(ConvertUtil.escapeSpecialCharacters(input.getNotifications().getContactDetails().get(2).getContact().getUsername()));
        } else {
            value1.setName(CLIENTE_DEFAULT_NAME);
        }

        ValueDTO value2 = new ValueDTO();
        value2.setId(INSURANCE_NAME_EMAIL);
        value2.setName(ConvertUtil.escapeSpecialCharacters( isRoyal
                ? policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_DESC.getValue()).toString().toUpperCase()
                : icf2Response.getIcmf1S2().getNOMSEGU().toUpperCase()));

        ValueDTO value3 = new ValueDTO();
        value3.setId(PLAN_NAME_EMAIL);
        value3.setName(isRoyal
                ? policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()).toString()
                : icf2Response.getIcmf1S2().getPLANELE());

        Date date = new Date();
        ValueDTO value4 = new ValueDTO();
        value4.setId(CANCELLATION_DATE_VALUE_EMAIL);
        value4.setName(new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD).format(date)); //"05 de marzo del 2024"

        ValueDTO value5 = new ValueDTO();
        value5.setId(CANCELLATION_HOUR_VALUE_EMAIL);
        value5.setName(new SimpleDateFormat(DATE_FORMAT_HH_MM_SS).format(date)); // 11:08:48

        ValueDTO value6 = new ValueDTO();
        value6.setId(APPLICATION_NUMBER_VALUE_EMAIL);
        value6.setName(requestCancellationId);

        ValueDTO value7 = new ValueDTO();
        value7.setId(CERTIFICATE_NUMBER_VALUE_EMAIL);
        value7.setName((input.getContractId().substring(0, 4))
                .concat("-")
                .concat(input.getContractId().substring(4, 8))
                .concat("-")
                .concat(input.getContractId().substring(10))); //"0011-0176-4000188860"

        ValueDTO value8 = new ValueDTO();
        value8.setId(CANCELLATION_REASON_VALUE_EMAIL);
        value8.setName(ConvertUtil.escapeSpecialCharacters(ConvertUtil.convertReasonCancellation(input.getReason().getId())));


        values.add(value1);
        values.add(value2);
        values.add(value3);
        values.add(value4);
        values.add(value5);
        values.add(value6);
        values.add(value7);
        values.add(value8);

        return values;

    }


}
