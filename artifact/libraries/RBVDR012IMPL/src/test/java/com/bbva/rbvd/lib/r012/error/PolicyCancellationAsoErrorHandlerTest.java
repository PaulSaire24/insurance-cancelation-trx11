package com.bbva.rbvd.lib.r012.error;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.lib.r012.RBVDR012Test;
import com.bbva.rbvd.lib.r012.impl.error.PolicyCancellationAsoErrorHandler;

public class PolicyCancellationAsoErrorHandlerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR012Test.class);
	
	private PolicyCancellationAsoErrorHandler policyCancellationAsoErrorHandler = new PolicyCancellationAsoErrorHandler();
	private String bodyError;
	
    @Before
    public void setUp() {
    	bodyError = "{\"messages\": [{\"code\": \"functionalError\",\"message\": \"CODE#ERROR.\",\"parameters\": [],\"type\": \"FATAL\"}]}";
    }
	
    @Test
    public void getAdviceCodeTest() {
		LOGGER.info("RBVDR001Test - Executing getAdviceCodeTest...");
        String validation = policyCancellationAsoErrorHandler.handler(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "","".getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode(), validation);
        
        String body = bodyError.replace("CODE", PolicyCancellationAsoErrorHandler.ICE9041);
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_POLICY_NOT_EXIST.getAdviceCode(), validation);
        
        body = bodyError.replace("CODE", PolicyCancellationAsoErrorHandler.ICE9230);
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode(), validation);
        
        body = bodyError.replace("CODE", PolicyCancellationAsoErrorHandler.ICER163);
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_CANCELLATIONDATE_EXCEEDS_COVERAGE.getAdviceCode(), validation);
        
        body = bodyError.replace("CODE", PolicyCancellationAsoErrorHandler.ICE9114);
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_UNCANCELLABLE_PRODUCT.getAdviceCode(), validation);
        
        body = bodyError.replace("CODE", "ABC");
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode(), validation);
        
        body = "{ }";
        validation = policyCancellationAsoErrorHandler.handler(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "", body.getBytes(), StandardCharsets.UTF_8));
        assertEquals(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode(), validation);
        
    }
}
