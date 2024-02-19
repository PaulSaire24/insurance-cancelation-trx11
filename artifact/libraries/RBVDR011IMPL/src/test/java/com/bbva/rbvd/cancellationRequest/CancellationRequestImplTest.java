package com.bbva.rbvd.cancellationRequest;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.DatoParticularBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericContactDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.*;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.business.CancellationRequestImpl;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.bbva.rbvd.hostConnections.ICF2ConnectionTest.buildICF2ResponseOk;
import static com.bbva.rbvd.lib.r011.RBVDR011Test.*;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CancellationRequestImplTest {
    private CancellationRequestImpl cancellationRequestImpl = new CancellationRequestImpl();
    private RBVDR311 rbvdr311;
    private PISDR103 pisdr103;
    private PISDR100 pisdr100;
    private ICR4Connection icr4Connection;
    private ApplicationConfigurationService applicationConfigurationService;

    @Before
    public void setUp() {
        ThreadContext.set(new Context());
        rbvdr311 = mock(RBVDR311.class);
        cancellationRequestImpl.setRbvdR311(rbvdr311);

        pisdr103 = mock(PISDR103.class);
        cancellationRequestImpl.setPisdR103(pisdr103);

        pisdr100 = mock(PISDR100.class);
        cancellationRequestImpl.setPisdR100(pisdr100);

        icr4Connection = mock(ICR4Connection.class);
        cancellationRequestImpl.setIcr4Connection(icr4Connection);

        applicationConfigurationService = mock(ApplicationConfigurationService.class);
        cancellationRequestImpl.setApplicationConfigurationService(applicationConfigurationService);

        when(applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",")).thenReturn("1121,");
    }

    @Test
    public void validateNewCancellationRequestRoyal(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> policy = buildPolicyMap();
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, true);
        assertTrue(validate);
    }

    @Test
    public void validateNewCancellationRequestRoyal_WithRetainedCancellationRequest(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        List<Map<String, Object>> requestCancellationMovLastRet = buildOpenRequestCancellationMovLastRet();
        when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLastRet);
        Map<String, Object> policy = buildPolicyMap();
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, true);
        assertTrue(validate);
    }

    @Test
    public void validateNewCancellationRequestRoyal_CancelledPolicy(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> policy = buildCancelledPolicyMap();
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, true);
        assertFalse(validate);
    }

    @Test
    public void validateNewCancellationRequestNoRoyal(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> policy = null;
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, false);
        assertTrue(validate);
    }


    @Test
    public void validateCancellationRequestRegisterForRoyalWithEmailContactOk(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }

    @Test
    public void validateCancellationRequestRegisterForRoyalWithoutExtornoComisionOk(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponseWithoutExtornoComision();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }

    @Test
    public void validateCancellationRequestRegisterForRoyal_EndOfValidity(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponseWithoutExtornoComision();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        input.setCancellationType(END_OF_VALIDATY.name());
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }

    @Test
    public void validateCancellationRequestRegisterForRoyal_ApplicationDate(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponseWithoutExtornoComision();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        input.setCancellationType(APPLICATION_DATE.name());
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(),"1121");
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }


    @Test
    public void validateCancellationRequestRegisterForNoRoyalWithEmailContactOk(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = null;
        ICF2Response icf2Response = buildICF2ResponseOk();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, false, icf2Response, "123456", "1121");
        assertTrue(validate);
    }

    @Test
    public void validateExecuteGetRequestCancellationMovLast(){
        when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
        Map<String, Object> validate = cancellationRequestImpl.executeGetRequestCancellationMovLast("00110176584000189190");
        assertNotNull(validate);
    }

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_EmailContactAndPhoneContact(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("IMMEDIATE");
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

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_WithOutNotifications(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("IMMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
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

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_WithoutContactEmail(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("IMMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
        input.setNotifications(new NotificationsDTO());
        input.getNotifications().setContactDetails(new ArrayList<>());
        input.getNotifications().getContactDetails().add(new ContactDetailDTO());
        input.setIsRefund(true);
        return input;
    }

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_WithCardPaymentMethod(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("IMMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
        input.setIsRefund(true);
        input.setInsurerRefund(new InsurerRefundCancellationDTO());
        input.getInsurerRefund().setPaymentMethod(new PaymentMethodCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().setContract(new ContractCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setProductType(new CommonCancellationDTO());
        input.getInsurerRefund().getPaymentMethod().getContract().setContractType(RBVDProperties.CONTRACT_TYPE_EXTERNAL_ID.getValue());
        input.getInsurerRefund().getPaymentMethod().getContract().getProductType().setId(RBVDConstants.TAG_CARD);
        input.getInsurerRefund().getPaymentMethod().getContract().setNumber("00110130220210452319");
        return input;
    }

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_WithAccountPaymentMethod(){
        InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
        input.setContractId("11111111111111111111");
        input.setChannelId("PC");
        input.setUserId("user");
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId("01");
        input.setReason(reason);
        input.setCancellationType("IMMEDIATE");
        input.setCancellationDate(Calendar.getInstance());
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

    public static CancelationSimulationPayloadBO buildCancelationSimulationResponse(){
        CancelationSimulationPayloadBO response = new CancelationSimulationPayloadBO();
        response.setFechaAnulacion(Calendar.getInstance().getTime());
        response.setMoneda("PEN");
        response.setMonto(15.00);
        DatoParticularBO cuenta = new DatoParticularBO();
        cuenta.setValor("1234****1234|15.00|PEN");
        response.setCuenta(cuenta);
        response.setExtornoComision(5.00);
        return response;
    }

    private CancelationSimulationPayloadBO buildCancelationSimulationResponseWithoutExtornoComision(){
        CancelationSimulationPayloadBO response = new CancelationSimulationPayloadBO();
        response.setFechaAnulacion(Calendar.getInstance().getTime());
        response.setMoneda("PEN");
        response.setMonto(15.00);
        DatoParticularBO cuenta = new DatoParticularBO();
        cuenta.setValor("1234****1234|15.00|PEN");
        response.setCuenta(cuenta);
        return response;
    }

    private Map<String, Object> buildResponseGetRequestCancellationId(){
        Map<String, Object> response = new HashMap<>();
        response.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal(1));
        return response;
    }

    public static List<Map<String, Object>> buildOpenRequestCancellationMovLastRet(){
        List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
        requestCancellationMovLast.add(new HashMap<>());
        requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
        requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
        requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "02");
        return requestCancellationMovLast;
    }
}
