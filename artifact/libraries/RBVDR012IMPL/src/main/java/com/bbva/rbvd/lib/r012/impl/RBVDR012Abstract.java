package com.bbva.rbvd.lib.r012.impl;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.library.AbstractLibrary;
import com.bbva.elara.utility.api.connector.APIConnector;
import com.bbva.elara.utility.api.connector.APIConnectorBuilder;
import com.bbva.rbvd.lib.r012.RBVDR012;
import com.bbva.rbvd.lib.r012.impl.error.PolicyCancellationAsoErrorHandler;
import com.bbva.rbvd.lib.r012.impl.util.MockService;

/**
 * This class automatically defines the libraries and utilities that it will use.
 */
public abstract class RBVDR012Abstract extends AbstractLibrary implements RBVDR012 {

	protected ApplicationConfigurationService applicationConfigurationService;

	protected APIConnectorBuilder apiConnectorBuilder;

	protected APIConnector internalApiConnector;

	protected PolicyCancellationAsoErrorHandler policyCancellationAsoErrorHandler;

	protected MockService mockService;

	/**
	* @param applicationConfigurationService the this.applicationConfigurationService to set
	*/
	public void setApplicationConfigurationService(ApplicationConfigurationService applicationConfigurationService) {
		this.applicationConfigurationService = applicationConfigurationService;
	}

	/**
	* @param apiConnectorBuilder the this.apiConnectorBuilder to set
	*/
	public void setApiConnectorBuilder(APIConnectorBuilder apiConnectorBuilder) {
		this.apiConnectorBuilder = apiConnectorBuilder;
	}

	/**
	* @param internalApiConnector the this.internalApiConnector to set
	*/
	public void setInternalApiConnector(APIConnector internalApiConnector) {
		this.internalApiConnector = internalApiConnector;
	}
	public void setMockService(MockService mockService) { this.mockService = mockService; }

	public void setPolicyCancellationAsoErrorHandler(PolicyCancellationAsoErrorHandler policyCancellationAsoErrorHandler) { this.policyCancellationAsoErrorHandler = policyCancellationAsoErrorHandler; }

}