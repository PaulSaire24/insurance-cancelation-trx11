package com.bbva.rbvd.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.rbvd.lib.r012.impl.util.MockService;

public class MockServiceTest {
	private MockService mockService = new MockService();
    private ApplicationConfigurationService applicationConfigurationService;
    
    @Before
    public void setUp() {
        applicationConfigurationService = mock(ApplicationConfigurationService.class);
        mockService.setApplicationConfigurationService(applicationConfigurationService);
    }

    @Test
    public void isEnabledTrue() {
        when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
        
        boolean validation = mockService.isEnabled(MockService.MOCKER_ASOCANCELLATION);
        assertTrue(validation);
    }
    
    @Test
    public void isEnabledFalse() {
    	boolean validation = mockService.isEnabled("");
    	assertFalse(validation);
    	
    	when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
    	validation = mockService.isEnabled(MockService.MOCKER_ASOCANCELLATION);
    	assertFalse(validation);
    }
    
}
