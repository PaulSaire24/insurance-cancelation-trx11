package com.bbva.rbvd.hostConnections;

import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICMF1S2;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF2Connection;
import com.bbva.rbvd.lib.r310.RBVDR310;
import org.junit.Before;
import org.junit.Test;

import static com.bbva.rbvd.cancellationRequest.CancellationRequestImplTest.buildImmediateCancellationInput_EmailContact;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ICF2ConnectionTest {
    private ICF2Connection icf2Connection = new ICF2Connection();
    private RBVDR310 rbvdr310;

    @Before
    public void setUp() {
        ThreadContext.set(new Context());
        rbvdr310 = mock(RBVDR310.class);
        icf2Connection.setRbvdR310(rbvdr310);
    }

    @Test
    public void validateExecuteICF2TransactionOk(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        ICF2Response icf2Response = buildICF2ResponseOk();
        when(rbvdr310.executeICF2(anyObject())).thenReturn(icf2Response);
        icf2Response = icf2Connection.executeICF2Transaction(input);
        assertNotNull(icf2Response);
    }

    @Test
    public void validateExecuteICF2TransactionWithError(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        ICF2Response icf2Response = buildICF2ResponseError();
        when(rbvdr310.executeICF2(anyObject())).thenReturn(icf2Response);
        icf2Response = icf2Connection.executeICF2Transaction(input);
        assertNull(icf2Response);
    }

    public static ICF2Response buildICF2ResponseOk(){
        ICF2Response icf2Response = new ICF2Response();
        ICMF1S2 icmf1S2 = new ICMF1S2();
        icmf1S2.setCODPROD("801");
        icmf1S2.setIMPCLIE(15.00);
        icmf1S2.setDIVIMC("PEN");
        icmf1S2.setCODCLI("12345678");
        icmf1S2.setIMPCOMI(5.00);
        icmf1S2.setDIVDCIA("PEN");
        icf2Response.setIcmf1S2(icmf1S2);
        return icf2Response;
    }

    public static ICF2Response buildICF2ResponseError(){
        ICF2Response icf2Response = new ICF2Response();
        ICMF1S2 icmf1S2 = new ICMF1S2();
        icf2Response.setIcmf1S2(icmf1S2);
        icf2Response.setHostAdviceCode("00000169");
        return icf2Response;
    }

}
