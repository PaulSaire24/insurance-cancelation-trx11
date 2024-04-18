package com.bbva.rbvd.lib.r011.impl.transform.map;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICMF1S2;
import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericContactDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.*;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.notification.events.SendNotificationsDTO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class NotificationMapperTest {

    @Test
    public void buildEmail() {

        SendNotificationsDTO sendNotificationsDTO = NotificationMapper.buildEmail("REQUEST_CANCELLATION", input(), policy(), true, buildICF2Response(),
                "SACARBAJAL@GMAIL.COM", "563764", emailProperties(), new HashMap<>());

        Assert.assertNotNull(sendNotificationsDTO);
    }

    @Test
    public void buildEmailNoRoyal() {

        SendNotificationsDTO sendNotificationsDTO = NotificationMapper.buildEmail("REQUEST_CANCELLATION", input(), policy(), false, buildICF2Response(),
                "SACARBAJAL@GMAIL.COM", "563764", emailProperties(), new HashMap<>());

        Assert.assertNotNull(sendNotificationsDTO);
    }

    @Test
    public void buildEmailNot() {

        SendNotificationsDTO sendNotificationsDTO = NotificationMapper.buildEmail("REQUEST_CANCELLATION",input2(), policy(), false, buildICF2Response(),
                "SACARBAJAL@GMAIL.COM", "563764", emailProperties(), new HashMap<>());

        Assert.assertNotNull(sendNotificationsDTO);
    }

    private Map<Object, String> emailProperties() {
        Map<Object, String> propertiesEmail = new HashMap<>();

        propertiesEmail.put("notificationTypeRequestCancellationId", "65fc61d0233e735e5ba80031");
        propertiesEmail.put("descriptionEmail", "Recibimos tu solicitud de cancelación del seguro 86600, la cual está siendo procesada y será atendida en un plazo máximo de 1 día hábil.");
        propertiesEmail.put("addDescriptionEmail", "¡Te mantendremos informado!");
        propertiesEmail.put("titleEmail", "Datos importantes");
        propertiesEmail.put("applicationDateEmail", "Fecha de solicitud");
        propertiesEmail.put("applicationNumberEmail", "Número de Solicitud");
        propertiesEmail.put("certificateNumberEmail", "Número de Certificado");
        propertiesEmail.put("planTypeEmail", "Tipo de Plan");
        propertiesEmail.put("adviceEmail", "¡No te quedes sin la protección de tu Seguro!");
        propertiesEmail.put("additionalInformationEmail", "Recuerda los beneficios que estarías perdiendo dando clic al siguiente botón");

        return propertiesEmail;
    }

    private ICF2Response buildICF2Response(){
        ICF2Response response = new ICF2Response();

        ICMF1S2 icmf1S2 = new ICMF1S2();
        icmf1S2.setCODPROD("801");
        icmf1S2.setIMPCLIE(15.00);
        icmf1S2.setDIVIMC("PEN");
        icmf1S2.setCODCLI("12345678");
        icmf1S2.setIMPCOMI(5.00);
        icmf1S2.setDIVDCIA("PEN");
        icmf1S2.setPRODRI("00002121");
        icmf1S2.setNUMPOL("0000000000");
        icmf1S2.setTIPCONT("001");
        icmf1S2.setDESCONT("test@email.com");
        icmf1S2.setNOMSEGU("SEGURO VIDA RENTA");
        response.setIcmf1S2(icmf1S2);
        return response;
    }

    public static Map<String, Object> policy(){
        Map<String, Object> policy = new HashMap<>();
        policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
        policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
        policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
        policy.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), "1");
        policy.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), "123456");
        policy.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), "12345678");
        policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), "08-09-2021 00:00:00");
        policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(),"2101");
        policy.put(RBVDProperties.KEY_RESPONSE_PAYMENT_FREQUENCY_NAME.getValue(), "MENSUAL");
        policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_DESC.getValue(), "SEGURO VEHICULAR BBVA");
        return policy;
    }

    public static InputParametersPolicyCancellationDTO input(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("INMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
        input.setNotifications(new NotificationsDTO());
        input.getNotifications().setContactDetails(new ArrayList<>());
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.getNotifications().getContactDetails().get(0).setContact(new GenericContactDTO());
        input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_EMAIL_ID.getValue());
        input.getNotifications().getContactDetails().get(0).getContact().setAddress("CARLOS.CARRILLO.DELGADO@BBVA.COM");
        input.getNotifications().getContactDetails().get(0).getContact().setUsername("CESAR ANDRE");
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.getNotifications().getContactDetails().get(1).setContact(new GenericContactDTO());
        input.getNotifications().getContactDetails().get(1).getContact().setContactDetailType(RBVDProperties.CONTACT_MOBILE_ID.getValue());
        input.getNotifications().getContactDetails().get(1).getContact().setNumber("999888777");
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.getNotifications().getContactDetails().get(2).setContact(new GenericContactDTO());
        input.getNotifications().getContactDetails().get(2).getContact().setContactDetailType("SOCIAL_MEDIA");
        input.getNotifications().getContactDetails().get(2).getContact().setUsername("SERGIO");
        input.setIsRefund(true);
        input.setInsurerRefund(new InsurerRefundCancellationDTO());
        input.getInsurerRefund().setPaymentMethod(new PaymentMethodCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().setContract(new ContractCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setProductType(new CommonCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setContractType(RBVDProperties.CONTRACT_TYPE_INTERNAL_ID.getValue());
        input.getInsurerRefund().getPaymentMethod().getContract().getProductType().setId(RBVDConstants.TAG_ACCOUNT);
        input.getInsurerRefund().getPaymentMethod().getContract().setId("00110130220210452319");
        return input;
    }

    public static InputParametersPolicyCancellationDTO input2(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("INMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
        input.setNotifications(new NotificationsDTO());
        input.getNotifications().setContactDetails(new ArrayList<>());
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.getNotifications().getContactDetails().get(0).setContact(new GenericContactDTO());
        input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_EMAIL_ID.getValue());
        input.getNotifications().getContactDetails().get(0).getContact().setAddress("CARLOS.CARRILLO.DELGADO@BBVA.COM");
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.getNotifications().getContactDetails().get(1).setContact(new GenericContactDTO());
        input.getNotifications().getContactDetails().get(1).getContact().setContactDetailType(RBVDProperties.CONTACT_MOBILE_ID.getValue());
        input.getNotifications().getContactDetails().get(1).getContact().setNumber("999888777");
        input.setIsRefund(true);
        input.setInsurerRefund(new InsurerRefundCancellationDTO());
        input.getInsurerRefund().setPaymentMethod(new PaymentMethodCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().setContract(new ContractCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setProductType(new CommonCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setContractType(RBVDProperties.CONTRACT_TYPE_INTERNAL_ID.getValue());
        input.getInsurerRefund().getPaymentMethod().getContract().getProductType().setId(RBVDConstants.TAG_ACCOUNT);
        input.getInsurerRefund().getPaymentMethod().getContract().setId("00110130220210452319");
        return input;
    }
}