package com.bbva.rbvd.lib.r012;

import java.util.Calendar;

import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;

public interface RBVDR012 {
	 EntityOutPolicyCancellationDTO executeCancelPolicyHost(String contractId, Calendar cancellationDate, GenericIndicatorDTO reason, NotificationsDTO notifications);
}
