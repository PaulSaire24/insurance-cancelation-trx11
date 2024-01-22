package com.bbva.rbvd.lib.r011.impl.hostConnections;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Request;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.lib.r310.RBVDR310;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICF2Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ICF2Connection.class);
    protected RBVDR310 rbvdR310;

    public ICF2Response executeICF2Transaction(InputParametersPolicyCancellationDTO input){
        ICF2Request request = new ICF2Request();
        request.setNUMCER(input.getContractId());
        request.setFECCANC((String.valueOf(input.getCancellationDate())).substring(0, 10));
        ICF2Response response = rbvdR310.executeICF2(request);
        LOGGER.info("***** RBVDR011Impl - executeICF2Transaction - rbvdR310.executeICF2 ***** response: {}", response);
        if (response.getHostAdviceCode() != null) {
            LOGGER.info("***** RBVDR011Impl - executeICF2Transaction - Error at icf2 execution - Host advice code: {}", response.getHostAdviceCode());
            return null;
        }
        return response;
    }

    public void setRbvdR310(RBVDR310 rbvdR310){this.rbvdR310 = rbvdR310;}
}
