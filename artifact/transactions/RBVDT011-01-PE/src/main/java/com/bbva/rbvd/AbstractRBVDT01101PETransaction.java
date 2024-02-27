package com.bbva.rbvd;

import com.bbva.elara.transaction.AbstractTransaction;
import com.bbva.rbvd.dto.insurancecancelation.commons.ExchangeRateDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import java.util.Calendar;

/**
 * In this class, the input and output data is defined automatically through the setters and getters.
 */
public abstract class AbstractRBVDT01101PETransaction extends AbstractTransaction {

	public AbstractRBVDT01101PETransaction(){
	}


	/**
	 * Return value for input parameter insurance-contract-id
	 */
	protected String getInsuranceContractId(){
		return (String)this.getParameter("insurance-contract-id");
	}

	/**
	 * Return value for input parameter cancellationDate
	 */
	protected Calendar getCancellationdate(){
		return (Calendar)this.getParameter("cancellationDate");
	}

	/**
	 * Return value for input parameter reason
	 */
	protected GenericIndicatorDTO getReason(){
		return (GenericIndicatorDTO)this.getParameter("reason");
	}

	/**
	 * Return value for input parameter notifications
	 */
	protected NotificationsDTO getNotifications(){
		return (NotificationsDTO)this.getParameter("notifications");
	}

	/**
	 * Return value for input parameter isRefund
	 */
	protected Boolean getIsrefund(){
		return (Boolean)this.getParameter("isRefund");
	}

	/**
	 * Return value for input parameter cancellationType
	 */
	protected String getCancellationtype(){
		return (String)this.getParameter("cancellationType");
	}

	/**
	 * Return value for input parameter insurerRefund
	 */
	protected InsurerRefundCancellationDTO getInsurerrefund(){
		return (InsurerRefundCancellationDTO)this.getParameter("insurerRefund");
	}

	/**
	 * Set value for String output parameter id
	 */
	protected void setId(final String field){
		this.addParameter("id", field);
	}

	/**
	 * Set value for Calendar output parameter cancellationDate
	 */
	protected void setCancellationdate(final Calendar field){
		this.addParameter("cancellationDate", field);
	}

	/**
	 * Set value for GenericIndicatorDTO output parameter reason
	 */
	protected void setReason(final GenericIndicatorDTO field){
		this.addParameter("reason", field);
	}

	/**
	 * Set value for NotificationsDTO output parameter notifications
	 */
	protected void setNotifications(final NotificationsDTO field){
		this.addParameter("notifications", field);
	}

	/**
	 * Set value for GenericStatusDTO output parameter status
	 */
	protected void setStatus(final GenericStatusDTO field){
		this.addParameter("status", field);
	}

	/**
	 * Set value for InsurerRefundCancellationDTO output parameter insurerRefund
	 */
	protected void setInsurerrefund(final InsurerRefundCancellationDTO field){
		this.addParameter("insurerRefund", field);
	}

	/**
	 * Set value for GenericAmountDTO output parameter customerRefund
	 */
	protected void setCustomerrefund(final GenericAmountDTO field){
		this.addParameter("customerRefund", field);
	}

	/**
	 * Set value for ExchangeRateDTO output parameter exchangeRate
	 */
	protected void setExchangerate(final ExchangeRateDTO field){
		this.addParameter("exchangeRate", field);
	}

	/**
	 * Set value for Boolean output parameter isRefund
	 */
	protected void setIsrefund(final Boolean field){
		this.addParameter("isRefund", field);
	}

	/**
	 * Set value for String output parameter cancellationType
	 */
	protected void setCancellationtype(final String field){
		this.addParameter("cancellationType", field);
	}
}
