package com.bbva.rbvd.cancellationRequest;

import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICMF1S2;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.DatoParticularBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericContactDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.cancellationRequest.CancellationRequestImpl;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.bbva.rbvd.hostConnections.ICF2ConnectionTest.buildICF2ResponseOk;
import static com.bbva.rbvd.lib.r011.RBVDR011Test.buildCancelledPolicyMap;
import static com.bbva.rbvd.lib.r011.RBVDR011Test.buildPolicyMap;
import static com.bbva.rbvd.lib.r011.impl.cancellationRequest.CancellationRequestImpl.PENDING_CANCELLATION_STATUS;
import static com.bbva.rbvd.lib.r011.impl.cancellationRequest.CancellationRequestImpl.RETAINED_INSURANCE_STATUS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CancellationRequestImplTest {
    private CancellationRequestImpl cancellationRequestImpl = new CancellationRequestImpl();
    private RBVDR311 rbvdr311;
    private PISDR103 pisdr103;
    private PISDR100 pisdr100;
    private ICR4Connection icr4Connection;
    private ICF2Response icf2Response;

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
    }

    @Test
    public void validateNewCancellationRequestRoyal(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> policy = buildPolicyMap();
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, true);
        assertTrue(validate);
    }

    @Test
    public void validateNewCancellationRequestRoyal_CancelledPolicy(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> policy = buildCancelledPolicyMap();
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, true);
        assertFalse(validate);
    }

    @Test
    public void validateNewCancellationRequestNoRoyal(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> policy = null;
        boolean validate = cancellationRequestImpl.validateNewCancellationRequest(input, policy, false);
        assertTrue(validate);
    }

    @Test
    public void validateOpenCancellationRequest_NotRegisteredCancellationRequest(){
        boolean validate = cancellationRequestImpl.isOpenOrNotExistsCancellationRequest(null);
        assertTrue(validate);
    }

    @Test
    public void validateOpenCancellationRequest_OpenCancellationRequest(){
        Map<String, Object> requestCancellationMovLast = new HashMap<>();
        requestCancellationMovLast.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), PENDING_CANCELLATION_STATUS);
        boolean validate = cancellationRequestImpl.isOpenOrNotExistsCancellationRequest(requestCancellationMovLast);
        assertTrue(validate);
    }

    @Test
    public void validateOpenCancellationRequest_CancelledCancellationRequest(){
        Map<String, Object> requestCancellationMovLast = new HashMap<>();
        requestCancellationMovLast.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), RBVDConstants.MOV_BAJ);
        boolean validate = cancellationRequestImpl.isOpenOrNotExistsCancellationRequest(requestCancellationMovLast);
        assertFalse(validate);
    }

    @Test
    public void validateNotRegisteredCancellationRequest(){
        boolean validate = cancellationRequestImpl.isRetainedOrNotExistsCancellationRequest(null);
        assertTrue(validate);
    }

    @Test
    public void validateRetainedCancellationRequest(){
        Map<String, Object> requestCancellationMovLast = new HashMap<>();
        requestCancellationMovLast.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), RETAINED_INSURANCE_STATUS);
        boolean validate = cancellationRequestImpl.isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
        assertTrue(validate);
    }

    @Test
    public void validateCancelledCancellationRequest(){
        Map<String, Object> requestCancellationMovLast = new HashMap<>();
        requestCancellationMovLast.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), RBVDConstants.MOV_BAJ);
        boolean validate = cancellationRequestImpl.isRetainedOrNotExistsCancellationRequest(requestCancellationMovLast);
        assertFalse(validate);
    }

    @Test
    public void validateCancellationRequestRegisterForRoyalWithEmailContactOk(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }

    @Test
    public void validateCancellationRequestRegisterForRoyalWithPhoneContactOk(){
        CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_PhoneContact();
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
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_PhoneContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = buildPolicyMap();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, true, null, "123456", "1121");
        assertTrue(validate);
    }


    @Test
    public void validateCancellationRequestRegisterForNoRoyalWithEmailContactOk(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> responseGetRequestCancellationId = buildResponseGetRequestCancellationId();
        Map<String, Object> policy = null;
        ICF2Response icf2Response = buildICF2ResponseOk();
        when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
        when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
        boolean validate = cancellationRequestImpl.executeFirstCancellationRequest(input, policy, false, icf2Response, "123456", "1121");
        assertTrue(validate);
    }

    public static InputParametersPolicyCancellationDTO buildImmediateCancellationInput_EmailContact(){
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
        input.getNotifications().getContactDetails().get(0).getContact().setNumber("CARLOS.CARRILLO.DELGADO@BBVA.COM");
        return input;
    }

    private InputParametersPolicyCancellationDTO buildImmediateCancellationInput_PhoneContact(){
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
        input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_MOBILE_ID.getValue());
        input.getNotifications().getContactDetails().get(0).getContact().setNumber("999888777");
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
}
