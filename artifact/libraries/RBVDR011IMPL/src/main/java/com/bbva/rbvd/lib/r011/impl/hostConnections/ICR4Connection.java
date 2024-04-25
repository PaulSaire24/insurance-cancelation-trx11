package com.bbva.rbvd.lib.r011.impl.hostConnections;

import com.bbva.rbvd.dto.cicsconnection.icr4.ICR4Response;
import com.bbva.rbvd.dto.cicsconnection.utils.ICR4DTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.lib.r042.RBVDR042;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICR4Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ICR4Connection.class);
    private static final String OK = "OK";
    private static final String OK_WARN = "OK_WARN";
    protected RBVDR042 rbvdR042;

    public boolean executeICR4Transaction(InputParametersPolicyCancellationDTO input, String status){
        LOGGER.info("***** ICR4Connection - executeICR4Transaction - status: {}", status);
        ICR4DTO icr4Dto = new ICR4DTO();
        icr4Dto.setNUMCON(input.getContractId());
        icr4Dto.setICSITU(status);
        ICR4Response result = rbvdR042.executeICR4(icr4Dto);
        LOGGER.info("***** ICR4Connection - executeICR4Transaction - ICR4 result: {}", result);
        return result.getCodeMessage().equals(OK) || result.getCodeMessage().equals(OK_WARN);
    }

    public void setRbvdR042(RBVDR042 rbvdR042){
        this.rbvdR042 = rbvdR042;
    }
}
