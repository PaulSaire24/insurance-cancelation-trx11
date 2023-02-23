package com.bbva.rbvd.lib.r011;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.rbvd.dto.insurancecancelation.aso.cancelationsimulation.CancelationSimulationASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.cancelationsimulation.CancelationSimulationHostBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbva.elara.domain.transaction.Advice;
import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r003.RBVDR003;
import com.bbva.rbvd.lib.r011.impl.RBVDR011Impl;
import com.bbva.rbvd.lib.r012.RBVDR012;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:/META-INF/spring/RBVDR011-app.xml",
		"classpath:/META-INF/spring/RBVDR011-app-test.xml",
		"classpath:/META-INF/spring/RBVDR011-arc.xml",
		"classpath:/META-INF/spring/RBVDR011-arc-test.xml" })
public class RBVDR011Test {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Test.class);

	private RBVDR011Impl rbvdR011 = new RBVDR011Impl();
	private RBVDR011Impl spyRbvdR011;
	private RBVDR012 RBVDR012;
	private RBVDR003 rbvdr003;
	private PISDR100 pisdr100;
	private PISDR103 pisdr103;
	private ApplicationConfigurationService applicationConfigurationService;

	@Before
	public void setUp() throws Exception {
		ThreadContext.set(new Context());
		spyRbvdR011 = spy(rbvdR011);
		
		RBVDR012 = mock(RBVDR012.class);
		rbvdR011.setRbvdR012(RBVDR012);
		spyRbvdR011.setRbvdR012(RBVDR012);
		
		rbvdr003 = mock(RBVDR003.class);
		rbvdR011.setRbvdR003(rbvdr003);
		spyRbvdR011.setRbvdR003(rbvdr003);

		applicationConfigurationService = mock(ApplicationConfigurationService.class);
		rbvdR011.setApplicationConfigurationService(applicationConfigurationService);
		spyRbvdR011.setApplicationConfigurationService(applicationConfigurationService);

		pisdr100 = mock(PISDR100.class);
		rbvdR011.setPisdR100(pisdr100);
		spyRbvdR011.setPisdR100(pisdr100);

		pisdr103 = mock(PISDR103.class);
		rbvdR011.setPisdR103(pisdr103);
		spyRbvdR011.setPisdR103(pisdr103);
	}
	
	@Test
	public void executePolicyCancellationTestNull(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(null);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_ANU);
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_BAJ);
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}
	
	@Test
	public void executePolicyCancellationTestOK() throws IOException{
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		when(RBVDR012.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		input.setChannelId("PC");
		input.setUserId("UI");
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationWithRefundTestOK() throws IOException{
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationWithRefundTestOK...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		EntityOutPolicyCancellationDTO outHost = new EntityOutPolicyCancellationDTO();
		GenericStatusDTO genericStatusDTO = new GenericStatusDTO();
		outHost.setStatus(genericStatusDTO);
		outHost.getStatus().setDescription("REFUND");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(outHost);
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		when(RBVDR012.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		input.setChannelId("PC");
		input.setUserId("UI");
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);

		outHost.getStatus().setDescription("NULL");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(outHost);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);

		outHost.setStatus(null);
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(outHost);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}
	
	@Test
	public void executePolicyCancellationTestBDEmptyResult(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestBDEmptyResult...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		List<Advice> advices = new ArrayList<>();
		Advice advice = new Advice();
		advices.add(advice);
		advice.setCode(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode());
		when(spyRbvdR011.getAdviceList()).thenReturn(advices);
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationIsCancellationRequestTestOK() {
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");

		Date cancellationDate = new Date();
		CancelationSimulationASO cancelationSimulationASO = new CancelationSimulationASO();
		cancelationSimulationASO.setData(new CancelationSimulationHostBO());
		cancelationSimulationASO.getData().setCancelationDate(cancellationDate);
		cancelationSimulationASO.getData().setCustomerRefund(new GenericAmountDTO());
		cancelationSimulationASO.getData().getCustomerRefund().setAmount(Double.valueOf(123));
		cancelationSimulationASO.getData().getCustomerRefund().setCurrency("USD");

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(rbvdr003.executeCypherService(anyObject())).thenReturn("");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(RBVDR012.executeSimulateInsuranceContractCancellations(anyString())).thenReturn(cancelationSimulationASO);
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		assertEquals(cancellationDate, validation.getCancellationDate().getTime());
	}

	@Test
	public void executePolicyCancellationIsCancellationRequestTestError() {
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");

		Date cancellationDate = new Date();
		CancelationSimulationASO cancelationSimulationASO = new CancelationSimulationASO();
		cancelationSimulationASO.setData(new CancelationSimulationHostBO());
		cancelationSimulationASO.getData().setCancelationDate(cancellationDate);
		cancelationSimulationASO.getData().setCustomerRefund(new GenericAmountDTO());
		cancelationSimulationASO.getData().getCustomerRefund().setAmount(Double.valueOf(123));
		cancelationSimulationASO.getData().getCustomerRefund().setCurrency("USD");

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(rbvdr003.executeCypherService(anyObject())).thenReturn("");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(RBVDR012.executeSimulateInsuranceContractCancellations(anyString())).thenReturn(null);
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}
}
