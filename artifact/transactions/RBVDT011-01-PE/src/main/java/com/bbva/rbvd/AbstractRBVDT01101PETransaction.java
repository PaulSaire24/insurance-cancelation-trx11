package com.bbva.rbvd;

import com.bbva.elara.transaction.AbstractTransaction;
import com.bbva.rbvd.dto.insurancecancelation.commons.ExchangeRateDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import java.util.Calendar;

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
	 * Set value for GenericAmountDTO output parameter insurerRefund
	 */
	protected void setInsurerrefund(final GenericAmountDTO field){
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
}
