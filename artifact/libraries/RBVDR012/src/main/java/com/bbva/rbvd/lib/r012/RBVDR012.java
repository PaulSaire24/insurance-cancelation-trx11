package com.bbva.rbvd.lib.r012;

import java.util.Calendar;

import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;

public interface RBVDR012 {

	public PolicyCancellationPayloadBO executeCancelPolicyRimac(InputRimacBO input, PolicyCancellationPayloadBO inputPayload);
	public EntityOutPolicyCancellationDTO executeCancelPolicyHost(String contractId, Calendar cancellationDate, GenericIndicatorDTO reason, NotificationsDTO notifications);
	public CancelationSimulationPayloadBO executeSimulateCancelationRimac(InputRimacBO input);
}
