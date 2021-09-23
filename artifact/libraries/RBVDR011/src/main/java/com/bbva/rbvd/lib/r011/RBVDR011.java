package com.bbva.rbvd.lib.r011;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;

public interface RBVDR011 {

	EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellationDTO input);

}
