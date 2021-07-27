package com.bbva.rbvd.lib.r012.factory;

import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import com.bbva.elara.utility.api.connector.APIConnector;
import com.bbva.elara.utility.api.connector.APIConnectorBuilder;
import com.bbva.elara.utility.api.connector.APIConnectorFactory;

public class ApiConnectorFactoryTest implements APIConnectorFactory {

	@Override
	public APIConnector getAPIConnector(BundleContext arg0) {
		return Mockito.mock(APIConnector.class);
	}

	@Override
	public APIConnector getAPIConnector(BundleContext arg0, boolean arg1) {
		return Mockito.mock(APIConnector.class);
	}

	@Override
	public APIConnector getAPIConnector(BundleContext arg0, boolean arg1, String arg2) {
		return Mockito.mock(APIConnector.class);
	}

	@Override
	public APIConnector getAPIConnector(BundleContext arg0, boolean arg1, String s, String arg2) {
		return Mockito.mock(APIConnector.class);
	}

	@Override
	public APIConnectorBuilder getAPIConnectorBuilder(BundleContext arg0) {
		return Mockito.mock(APIConnectorBuilder.class);
	}

	@Override
	public APIConnector getAPIConnector(BundleContext arg0, boolean arg1, boolean arg2) {
		return Mockito.mock(APIConnector.class);
	}

}
