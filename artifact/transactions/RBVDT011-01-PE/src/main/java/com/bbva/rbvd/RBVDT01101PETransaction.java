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
		LOGGER.info("RBVDT01101PETransaction - START");

		RBVDR011 rbvdR011 = getServiceLibrary(RBVDR011.class);

		String traceId = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.REQUESTID);
		LOGGER.info("Cabecera traceId: {}", traceId);
		String saleChannelId = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.CHANNELCODE);
		LOGGER.info("Cabecera channel-code: {}", saleChannelId);
		String user = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.USERCODE);
		LOGGER.info("Cabecera user-code: {}", user);
		String aap = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.AAP);
		LOGGER.info("Cabecera aap: {}", aap);
		String ipv4 = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.IPADDRESS);
		LOGGER.info("Cabecera ipv4: {}", ipv4);
		String environmentCode = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.ENVIRONCODE);
		LOGGER.info("Cabecera environmentCode: {}", environmentCode);
		String productCode = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.PRODUCTCODE);
		LOGGER.info("Cabecera productCode: {}", productCode);
		String headerOperationDate = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.OPERATIONDATE);
		LOGGER.info("Cabecera operationDate: {}", headerOperationDate);
		String operationTime = (String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.OPERATIONTIME);
		LOGGER.info("Cabecera operationTime: {}", operationTime);


		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setTraceId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.REQUESTID));
		input.setBranchId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.BRANCHCODE));
		input.setChannelId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.CHANNELCODE));
		input.setTransactionId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.TRANSACTIONCODE));
		input.setUserId((String) this.getRequestHeader().getHeaderParameter(RequestHeaderParamsName.USERCODE));
		input.setCancellationType(this.getCancellationtype());
		if(this.getIsrefund() != null) {input.setIsRefund(this.getIsrefund());}
		else{input.setIsRefund(true);}
		input.setContractId(this.getInsuranceContractId());
		input.setReason(this.getReason());
		input.setNotifications(this.getNotifications());
		input.setCancellationDate(this.getCancellationdate());
		if(this.getInsurerrefund() != null) input.setInsurerRefund(this.getInsurerrefund());



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
			this.setCancellationtype(this.getCancellationtype());
			if(this.getIsrefund() != null) {input.setIsRefund(this.getIsrefund());}
			else{input.setIsRefund(true);}
		}
	}

}
