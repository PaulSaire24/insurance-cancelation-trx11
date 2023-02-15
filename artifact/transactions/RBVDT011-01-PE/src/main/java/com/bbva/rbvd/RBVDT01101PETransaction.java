package com.bbva.rbvd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.elara.domain.transaction.RequestHeaderParamsName;
import com.bbva.elara.domain.transaction.Severity;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.lib.r011.RBVDR011;

/**
 * Transaction of policy cancelation
 *
 */
public class RBVDT01101PETransaction extends AbstractRBVDT01101PETransaction {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDT01101PETransaction.class);

	@Override
	public void execute() {
		RBVDR011 rbvdR011 = getServiceLibrary(RBVDR011.class);
		LOGGER.info("Execution of RBVDT01101PETransaction");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setTraceId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.REQUESTID));
		input.setBranchId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.BRANCHCODE));
		input.setChannelId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.CHANNELCODE));
		input.setTransactionId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.TRANSACTIONCODE));
		input.setUserId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.USERCODE));
		input.setContractId(this.getInsuranceContractId());
		input.setReason(this.getReason());
		input.setNotifications(this.getNotifications());
		input.setCancellationDate(this.getCancellationdate());
		input.setRequestBranch(this.getRequestbranch());
		LOGGER.info("input: {}", input);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		if (validation == null) {
			setSeverity(Severity.ENR);
		} else {
			this.setCancellationdate(validation.getCancellationDate());
			this.setId(validation.getId());
			this.setReason(validation.getReason());
			this.setNotifications(validation.getNotifications());
			this.setStatus(validation.getStatus());
			this.setInsurerrefund(validation.getInsurerRefund());
			this.setCustomerrefund(validation.getCustomerRefund());
			this.setExchangerate(validation.getExchangeRate());
		}
	}

}
