package com.bbva.rbvd.lib.r011;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

		pisdr100 = mock(PISDR100.class);
		rbvdR011.setPisdR100(pisdr100);
		spyRbvdR011.setPisdR100(pisdr100);
		
	}
	
	@Test
	public void executePolicyCancellationTestNull(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
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
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_BAJ);
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}
	
	@Test
	public void executePolicyCancellationTestOK() throws IOException{
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatus(anyMap())).thenReturn(1);
		when(RBVDR012.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		
	}
	
	@Test
	public void executePolicyCancellationTestBDEmptyResult(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestBDEmptyResult...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		List<Advice> advices = new ArrayList<>();
		Advice advice = new Advice();
		advices.add(advice);
		advice.setCode(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode());
		when(spyRbvdR011.getAdviceList()).thenReturn(advices);
		when(rbvdr003.executeCypherService(anyObject())).thenReturn("XYZ");
		when(RBVDR012.executeCancelPolicyHost(anyString(), any(Calendar.getInstance().getClass()), anyObject(), anyObject())).thenReturn(new EntityOutPolicyCancellationDTO());
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}
	
}
