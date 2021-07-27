package com.bbva.rbvd.lib.r011;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellation;

public interface RBVDR011 {

	EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellation input);

}
