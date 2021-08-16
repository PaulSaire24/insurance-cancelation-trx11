package com.bbva.rbvd.lib.r012.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

public class MockService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MockService.class);
	protected ApplicationConfigurationService applicationConfigurationService;
    
    public static final String MOCKER_ASOCANCELLATION = "ASOCANCELLATION";
    
    public boolean isEnabled(String mockType) {
    	LOGGER.info("***** mockService isEnabled function START ***** parameter: {}", mockType);
    	String value;
    	if (MOCKER_ASOCANCELLATION.equals(mockType)) {
    		value = applicationConfigurationService.getProperty(RBVDProperties.MOCK_ASO_CANCELLATION_ENABLED.getValue());
		} else {
			LOGGER.info("***** mockService isEnabled function DEFAULT VALUE FALSE ***** ");
			return false;
		}
    	boolean result = Boolean.parseBoolean(value);
    	if(result) LOGGER.info("***** mockService: Mock enabled *****");
    	return result;
    }
    
	public void setApplicationConfigurationService(ApplicationConfigurationService applicationConfigurationService) {
		this.applicationConfigurationService = applicationConfigurationService;
	}

	public EntityOutPolicyCancellationDTO getAsoCancellationMock() {
		return MockDTO.getInstance().getPolicyCancellationHostMockResponse().getData();
	}
}
