package com.bbva.rbvd.lib.r011.impl.transform.bean;

import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

public class RequestRimacBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestRimacBean.class);

    public static InputRimacBO buildRimacSimulationRequest(InputParametersPolicyCancellationDTO input, String policyId, String productCodeForRimac){
        LOGGER.info("***** RBVDR011Impl - buildRimacSimulationRequest - Begin *****");

        InputRimacBO rimacSimulationRequest = new InputRimacBO();
        rimacSimulationRequest.setTraceId(input.getTraceId());
        ZoneId zone = ZoneId.of("UTC");
        java.time.LocalDate cancellationDate = input.getCancellationDate().toInstant().atZone(zone).toLocalDate();
        rimacSimulationRequest.setFechaAnulacion(cancellationDate);
        rimacSimulationRequest.setNumeroPoliza(Integer.parseInt(policyId));
        rimacSimulationRequest.setCodProducto(productCodeForRimac);
        LOGGER.info("***** CancellationRequestImpl - buildRimacSimulationRequest - rimacSimulationRequest : {} *****", rimacSimulationRequest);
        return rimacSimulationRequest;
    }


}
