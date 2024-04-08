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

import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMapper.class);

    private static final String CUSTOMER_NAME_EMAIL = "name";
    private static final String DESCRIPTION_EMAIL = "description";
    private static final String ADD_DESCRIPTION_EMAIL = "additionalDescription";
    private static final String TITLE_EMAIL = "title";
    private static final String APPLICATION_DATE_EMAIL = "applicationDate";
    private static final String APPLICATION_DATE_VALUE_EMAIL = "applicationDateValue";
    private static final String APPLICATION_NUMBER_EMAIL = "applicationNumber";
    private static final String APPLICATION_NUMBER_VALUE_EMAIL = "applicationNumberValue";
    private static final String CERTIFICATE_NUMBER_EMAIL = "certificateNumber";
    private static final String CERTIFICATE_NUMBER_VALUE_EMAIL = "certificateNumberValue";
    private static final String PLAN_TYPE_EMAIL = "planType";
    private static final String PLAN_TYPE_VALUE_EMAIL = "planTypeValue";
    private static final String ADVICE_EMAIL = "advice";
    private static final String ADDITIONAL_INFORMATION_EMAIL = "additionalInformation";

    public static SendNotificationsDTO buildEmail(InputParametersPolicyCancellationDTO input, Map<String, Object> policy, boolean isRoyal, ICF2Response icf2Response,
                                                  String email, String requestCancellationId, Map<Object, String> propertiesEmail) {
        LOGGER.info("RBVDR011Impl - buildEmail() - START :: email - {}", email);

        ReceiverDTO receiver = new ReceiverDTO();
        receiver.setUserType(UserTypeEnum.CUSTOMER.getValue());
        receiver.setUserId("PE00150".concat(isRoyal
                ? policy.get(RBVDProperties.FIELD_CUSTOMER_ID.getValue()).toString()
                : icf2Response.getIcmf1S2().getCODCLI()));
        receiver.setRecipientType("TO");
        receiver.setEmail("SACARBAJAL@BBVA.COM");
        receiver.setContractId(input.getContractId());

        List<ReceiverDTO> receivers = new ArrayList<>();
        receivers.add(receiver);

        ValueDTO value2 = new ValueDTO();
        value2.setId(CUSTOMER_NAME_EMAIL);

        if(input.getNotifications() != null
                && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().size() > 2
                && input.getNotifications().getContactDetails().get(2).getContact() != null
                && input.getNotifications().getContactDetails().get(2).getContact().getUsername() != null
        ) {
            value2.setName(ConvertUtil.escapeSpecialCharacters(input.getNotifications().getContactDetails().get(2).getContact().getUsername()));
        } else {
            value2.setName("CLIENTE");
        }

        ValueDTO value3 = new ValueDTO();
        value3.setId(DESCRIPTION_EMAIL);
        value3.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("descriptionEmail")));

        ValueDTO value4 = new ValueDTO();
        value4.setId(ADD_DESCRIPTION_EMAIL);
        value4.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("addDescriptionEmail")));

        ValueDTO value5 = new ValueDTO();
        value5.setId(TITLE_EMAIL);
        value5.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("titleEmail")));

        ValueDTO value6 = new ValueDTO();
        value6.setId(APPLICATION_DATE_EMAIL);
        value6.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("applicationDateEmail")));

        ValueDTO value7 = new ValueDTO();
        value7.setId(APPLICATION_DATE_VALUE_EMAIL);
        value7.setName(new SimpleDateFormat(RBVDConstants.DATEFORMAT_ANNULATION_DATE).format(new Date())); //"05 de marzo del 2024"

        ValueDTO value8 = new ValueDTO();
        value8.setId(APPLICATION_NUMBER_EMAIL);
        value8.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("applicationNumberEmail")));

        ValueDTO value9 = new ValueDTO();
        value9.setId(APPLICATION_NUMBER_VALUE_EMAIL);
        value9.setName(requestCancellationId);

        ValueDTO value10 = new ValueDTO();
        value10.setId(CERTIFICATE_NUMBER_EMAIL);
        value10.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("certificateNumberEmail")));

        ValueDTO value11 = new ValueDTO();
        value11.setId(CERTIFICATE_NUMBER_VALUE_EMAIL);
        value11.setName((input.getContractId().substring(0, 4))
                            .concat("-")
                            .concat(input.getContractId().substring(4, 8))
                            .concat("-")
                            .concat(input.getContractId().substring(10))); //"0011-0176-4000188860"

        ValueDTO value12 = new ValueDTO();
        value12.setId(PLAN_TYPE_EMAIL);
        value12.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("planTypeEmail")));

        ValueDTO value13 = new ValueDTO();
        value13.setId(PLAN_TYPE_VALUE_EMAIL);
        value13.setName(isRoyal
                            ? policy.get(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue()).toString()
                            : icf2Response.getIcmf1S2().getPLANELE());

        ValueDTO value14 = new ValueDTO();
        value14.setId(ADVICE_EMAIL);
        value14.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("adviceEmail")));

        ValueDTO value15 = new ValueDTO();
        value15.setId(ADDITIONAL_INFORMATION_EMAIL);
        value15.setName(ConvertUtil.escapeSpecialCharacters(propertiesEmail.get("additionalInformationEmail")));

        List<ValueDTO> values = new ArrayList<>();
        values.add(value2);
        values.add(value3);
        values.add(value4);
        values.add(value5);
        values.add(value6);
        values.add(value7);
        values.add(value8);
        values.add(value9);
        values.add(value10);
        values.add(value11);
        values.add(value12);
        values.add(value13);
        values.add(value14);
        values.add(value15);

        NotificationDTO notification = new NotificationDTO();
        notification.setApplicationCode("RBVD"); //RBVD
        notification.setNotificationTypeId(propertiesEmail.get("notificationTypeRequestCancellationId"));
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


}
