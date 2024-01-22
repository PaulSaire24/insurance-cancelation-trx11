package com.bbva.rbvd.hostConnections;

import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICMF1S2;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICMF3S0;
import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericContactDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF3Connection;
import com.bbva.rbvd.lib.r051.RBVDR051;
import com.bbva.rbvd.lib.r310.RBVDR310;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.bbva.rbvd.cancellationRequest.CancellationRequestImplTest.buildImmediateCancellationInput_EmailContact;
import static com.bbva.rbvd.hostConnections.ICF2ConnectionTest.buildICF2ResponseOk;
import static com.bbva.rbvd.lib.r011.RBVDR011Test.buildPolicyMap;
import static com.bbva.rbvd.lib.r011.RBVDR011Test.buildResponseGetRequestCancellation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ICF3ConnectionTest {
    private ICF3Connection icf3Connection = new ICF3Connection();
    private RBVDR051 rbvdr051;

    @Before
    public void setUp() {
        ThreadContext.set(new Context());
        rbvdr051 = mock(RBVDR051.class);
        icf3Connection.setRbvdR051(rbvdr051);
    }

    @Test
    public void validateExecuteICF3TransactionOk(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> policy = buildPolicyMap();
        ICF2Response icf2Response = buildICF2ResponseOk();
        Map<String, Object> cancellationRequest = buildResponseGetRequestCancellation();
        ICF3Response icf3Response = buildICF3ResponseOk();
        when(rbvdr051.executePolicyCancellation(anyObject())).thenReturn(icf3Response);
        EntityOutPolicyCancellationDTO out = icf3Connection.executeICF3Transaction(input, cancellationRequest, policy, icf2Response);
        assertNotNull(out);
    }

    @Test
    public void validateExecuteICF3TransactionError(){
        InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContact();
        Map<String, Object> policy = buildPolicyMap();
        ICF2Response icf2Response = buildICF2ResponseOk();
        Map<String, Object> cancellationRequest = buildResponseGetRequestCancellation();
        ICF3Response icf3Response = buildICF3ResponseWithError();
        when(rbvdr051.executePolicyCancellation(anyObject())).thenReturn(icf3Response);
        EntityOutPolicyCancellationDTO out = icf3Connection.executeICF3Transaction(input, cancellationRequest, policy, icf2Response);
        assertNull(out);
    }

    public static ICF3Response buildICF3ResponseWithError(){
        ICF3Response  icF3Response = new ICF3Response();
        icF3Response.setHostAdviceCode("00000169");
        return icF3Response;
    }
    public static ICF3Response buildICF3ResponseOk(){
        ICF3Response icF3Response = new ICF3Response();
        ICMF3S0 icmf3s0 = new ICMF3S0();
        icmf3s0.setIDSTCAN("1");
        icmf3s0.setDESSTCA("OK");
        icmf3s0.setIMDECIA(0);
        icmf3s0.setIMPCLIE(0);
        icmf3s0.setTIPCAMB(0);
        icmf3s0.setFETIPCA("2023-11-03");
        icmf3s0.setDESSTCA("COMPLETED");
        icF3Response.setIcmf3s0(icmf3s0);
        icF3Response.setHostAdviceCode(null);
        return icF3Response;
    }
}
