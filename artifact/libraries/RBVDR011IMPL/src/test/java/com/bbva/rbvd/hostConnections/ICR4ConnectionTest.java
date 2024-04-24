package com.bbva.rbvd.hostConnections;

import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.rbvd.dto.cicsconnection.icr4.ICR4Response;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r042.RBVDR042;
import org.junit.Before;
import org.junit.Test;

import static com.bbva.rbvd.cancellationRequest.CancellationRequestImplTest.buildImmediateCancellationInput_EmailContactAndPhoneContact;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ICR4ConnectionTest {
    private static final String OK = "OK";
    private static final String OK_WARN = "OK_WARN";
    private static final String ERROR_CODE = "ERROR";

    private static final String CANCELLATION_REQUEST_STATUS = "PS";
    private ICR4Connection icr4Connection = new ICR4Connection();
    private RBVDR042 rbvdr042;

    @Before
    public void setUp() {
        ThreadContext.set(new Context());
        rbvdr042 = mock(RBVDR042.class);
        icr4Connection.setRbvdR042(rbvdr042);
    }

    @Test
    public void validateExecuteICR4TransactionOk(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        ICR4Response icr4Response = new ICR4Response();
        icr4Response.setCodeMessage(OK);

        when(rbvdr042.executeICR4(anyObject())).thenReturn(icr4Response);
        boolean validate = icr4Connection.executeICR4Transaction(input, CANCELLATION_REQUEST_STATUS);
        assertTrue(validate);
    }

    @Test
    public void validateExecuteICR4TransactionOkWarn(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        ICR4Response icr4Response = new ICR4Response();
        icr4Response.setCodeMessage(OK_WARN);

        when(rbvdr042.executeICR4(anyObject())).thenReturn(icr4Response);
        boolean validate = icr4Connection.executeICR4Transaction(input, CANCELLATION_REQUEST_STATUS);
        assertTrue(validate);
    }

    @Test
    public void validateExecuteICR4TransactionError(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
        ICR4Response icr4Response = new ICR4Response();
        icr4Response.setCodeMessage(ERROR_CODE);

        when(rbvdr042.executeICR4(anyObject())).thenReturn(icr4Response);
        boolean validate = icr4Connection.executeICR4Transaction(input, CANCELLATION_REQUEST_STATUS);
        assertFalse(validate);
    }

}
