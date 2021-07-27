package com.bbva.rbvd.lib.r012;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.elara.utility.api.connector.APIConnector;
import com.bbva.pisd.dto.insurance.amazon.SignatureAWS;
import com.bbva.pisd.lib.r014.PISDR014;
import com.bbva.rbvd.dto.insurancecancelation.aso.cancelationdetail.CancelationDetailASO;
import com.bbva.rbvd.dto.insurancecancelation.aso.policycancellation.PolicyCancellationASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.cancelationrulesvalidation.CancelationRulesValidationBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.lib.r012.factory.ApiConnectorFactoryTest;
import com.bbva.rbvd.lib.r012.impl.RBVDR012Impl;
import com.bbva.rbvd.mock.MockBundleContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestClientException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:/META-INF/spring/RBVDR012-app.xml",
		"classpath:/META-INF/spring/RBVDR012-app-test.xml",
		"classpath:/META-INF/spring/RBVDR012-arc.xml",
		"classpath:/META-INF/spring/RBVDR012-arc-test.xml" })
public class RBVDR012Test {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR012Test.class);

	private RBVDR012Impl rbvdR012 = new RBVDR012Impl();
	private PISDR014 pisdR014;
	
	private MockDTO mockDTO;
	private APIConnector externalApiConnector;
	private APIConnector internalApiConnector;
	private ApplicationConfigurationService applicationConfigurationService;
	
	private PolicyCancellationBO responseCancelPolicy;
	private PolicyCancellationASO responsePolicyCanelationHost;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ThreadContext.set(new Context());
		
		MockBundleContext mockBundleContext = mock(MockBundleContext.class);
		ApiConnectorFactoryTest apiConnectorFactoryMock = new ApiConnectorFactoryTest();
		externalApiConnector = apiConnectorFactoryMock.getAPIConnector(mockBundleContext, false);
		rbvdR012.setExternalApiConnector(externalApiConnector);
		
		internalApiConnector = apiConnectorFactoryMock.getAPIConnector(mockBundleContext, false);
		rbvdR012.setInternalApiConnector(internalApiConnector);
		
		applicationConfigurationService = Mockito.mock(ApplicationConfigurationService.class);
		rbvdR012.setApplicationConfigurationService(applicationConfigurationService);
		
		mockDTO = MockDTO.getInstance();
		responseCancelPolicy = mockDTO.getCancelPolicyMockResponse();
		responsePolicyCanelationHost = mockDTO.getPolicyCancellationHostMockResponse();
		
		pisdR014 = mock(PISDR014.class);
		rbvdR012.setPisdR014(pisdR014);
		when(pisdR014.executeSignatureConstruction(anyString(), anyString(), anyString(), anyString(), anyString()))
			.thenReturn(new SignatureAWS("", "", "", ""));
		
	}
	
	@Test
	public void executeValidateCancelationRulesTestNull(){
		LOGGER.info("RBVDR019Test - Executing executeValidateCancelationRulesTestNull...");
		PolicyCancellationPayloadBO validation = rbvdR012.executeCancelPolicyRimac(null, null);
		assertNull(validation);
		
		InputRimacBO input = new InputRimacBO();
		validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNull(validation);
		
		input.setCodProducto("00001");
		validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNull(validation);
		
		input.setCodProducto("ABC");
		validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNull(validation);
		
		input.setCodProducto("001");
		validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNull(validation);
	}
	
	@Test
	public void executeValidateCancelationRulesTestOK() throws IOException{
		LOGGER.info("RBVDR019Test - Executing executeValidateCancelationRulesTestOK...");
		when(this.externalApiConnector.exchange(anyString(), any(HttpMethod.class), anyObject(), (Class<PolicyCancellationBO>) any(), anyMap()))
			.thenReturn(new ResponseEntity<>(responseCancelPolicy, HttpStatus.OK));
		
		InputRimacBO input = new InputRimacBO();
		input.setCodProducto("001");
		input.setNumeroPoliza(1000);
		PolicyCancellationPayloadBO validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNotNull(validation);
		
		input.setCertificado(100);
		input.setFechaAnulacion(LocalDate.now());
		input.setTipoFlujo("01");
		validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNotNull(validation);
		
		LOGGER.info("RBVDR019Test - Executing executeValidateCancelationRulesTestOK - END... validation: {}", validation);
	}

	@Test
	public void executeValidateCancelationRulesTestRestClientException() {
		LOGGER.info("RBVDR019Test - Executing executeValidateCancelationRulesTestRestClientException...");
		when(this.externalApiConnector.exchange(anyString(), any(HttpMethod.class), anyObject(), (Class<CancelationRulesValidationBO>) any(), anyMap()))
			.thenThrow(new RestClientException("ERROR"));
		
		InputRimacBO input = new InputRimacBO();
		input.setCodProducto("001");
		input.setNumeroPoliza(1000);
		PolicyCancellationPayloadBO validation = rbvdR012.executeCancelPolicyRimac(input, null);
		assertNull(validation);
	}
	
	@Test
	public void executeCancelPolicyHostTestNull(){
		LOGGER.info("RBVDR001Test - Executing executeCancelPolicyHostTestNull...");
		EntityOutPolicyCancellationDTO validation = rbvdR012.executeCancelPolicyHost(null, null, null, null);
		assertNull(validation);
		
		validation = rbvdR012.executeCancelPolicyHost("111", null, null, null);
		assertNull(validation);
		
		validation = rbvdR012.executeCancelPolicyHost("11111111111111111111", null, null, null);
		assertNull(validation);
		
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		validation = rbvdR012.executeCancelPolicyHost("11111111111111111111", null, reason, null);
		assertNull(validation);
	}
	
	@Test
	public void executeCancelPolicyHostTestOK() throws IOException{
		LOGGER.info("RBVDR001Test - Executing executeCancelPolicyHostTestOK...");
		when(this.internalApiConnector.exchange(anyString(), any(HttpMethod.class), anyObject(), (Class<PolicyCancellationASO>) any(), anyMap()))
		.thenReturn(new ResponseEntity<>(responsePolicyCanelationHost, HttpStatus.OK));
		
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("10");
		EntityOutPolicyCancellationDTO validation = rbvdR012.executeCancelPolicyHost("11111111111111111111", null, reason, null);
		assertNotNull(validation);
		
		LOGGER.info("RBVDR001Test - Executing executeCancelPolicyHostTestOK - END... validation: {}", validation);
	}
	
	@Test
	public void executeCancelPolicyHostTestRestClientException() {
		LOGGER.info("RBVDR001Test - Executing executeCancelPolicyHostTestRestClientException...");
		when(this.internalApiConnector.exchange(anyString(), any(HttpMethod.class), anyObject(), (Class<PolicyCancellationASO>) any(), anyMap()))
		.thenThrow(new RestClientException("ERROR"));
		
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("10");
		EntityOutPolicyCancellationDTO validation = rbvdR012.executeCancelPolicyHost("11111111111111111111", null, reason, null);
		assertNull(validation);
	}
}