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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bbva.apx.exception.business.BusinessException;
import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICMF3S0;
import com.bbva.rbvd.dto.insurancecancelation.commons.*;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.*;
import com.bbva.rbvd.lib.r011.impl.util.ConstantsUtil;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbva.elara.domain.transaction.Advice;
import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.rbvd.lib.r311.RBVDR311;
import com.bbva.rbvd.lib.r042.RBVDR042;
import com.bbva.rbvd.lib.r051.RBVDR051;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
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
	private PISDR401 pisdR401;
	private RBVDR311 rbvdr311;
	private RBVDR042 rbvdR042;

	private PISDR103 pisdr103;

	private RBVDR051 rbvdR051;



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

		pisdR401 = mock(PISDR401.class);
		rbvdR011.setPisdR401(pisdR401);

		applicationConfigurationService = mock(ApplicationConfigurationService.class);
		rbvdR011.setApplicationConfigurationService(applicationConfigurationService);

		pisdr100 = mock(PISDR100.class);
		rbvdR011.setPisdR100(pisdr100);
		spyRbvdR011.setPisdR100(pisdr100);

		rbvdr311 = mock(RBVDR311.class);
		rbvdR011.setRbvdR311(rbvdr311);
		spyRbvdR011.setRbvdR311(rbvdr311);

		rbvdR042 = mock(RBVDR042.class);
		rbvdR011.setRbvdR042(rbvdR042);
		spyRbvdR011.setRbvdR042(rbvdR042);

		pisdr103 = mock(PISDR103.class);
		rbvdR011.setPisdR103(pisdr103);
		spyRbvdR011.setPisdR103(pisdr103);

		rbvdR051 = mock(RBVDR051.class);
		rbvdR011.setRbvdR051(rbvdR051);
		ICMF3S0 icmf3s0 = new ICMF3S0();
		icmf3s0.setIDSTCAN("1");
		icmf3s0.setDESSTCA("OK");
		icmf3s0.setIMDECIA(0);
		icmf3s0.setIMPCLIE(0);
		icmf3s0.setTIPCAMB(0);
		icmf3s0.setFETIPCA("2023-11-03");
		icmf3s0.setDESSTCA("COMPLETED");
		icmf3s0.setDIVDCIA("USD");
		icmf3s0.setDIVIMC("USD");
		icmf3s0.setDIVORIG("USD");
		icmf3s0.setIDCANCE("idmock");
		icmf3s0.setCODMOCA("1");
		icmf3s0.setDESMOCA("Desription mock");
		icmf3s0.setDIVDEST("Div");
		ICF3Response  ifc3Response = new ICF3Response();
		ifc3Response.setIcmf3s0(icmf3s0);
		ifc3Response.setHostAdviceCode(null);
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);
		spyRbvdR011.setRbvdR051(rbvdR051);
	}
	
	@Test
	public void executePolicyCancellationTestNull(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");

		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		
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

		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME, ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
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
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		when(RBVDR012.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		input.setChannelId("PC");
		input.setUserId("UI");
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);

		//Vida Inversion
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		input.setNotifications(new NotificationsDTO());
		input.getNotifications().setContactDetails(new ArrayList<>());
		input.getNotifications().getContactDetails().add(new ContactDetailDTO());
		input.getNotifications().getContactDetails().get(0).setContact(new GenericContactDTO());
		input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_MOBILE_ID.getValue());
		input.getNotifications().getContactDetails().get(0).getContact().setNumber("999888777");
		input.setInsurerRefund(new InsurerRefundCancellationDTO());
		input.getInsurerRefund().setPaymentMethod(new PaymentMethodCancellationDTO());
		input.getInsurerRefund().getPaymentMethod().setContract(new ContractCancellationDTO());
		input.getInsurerRefund().getPaymentMethod().getContract().setId("idMock");
		input.getInsurerRefund().getPaymentMethod().getContract().setProductType(new CommonCancellationDTO());
		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		product.put(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC,"VIDAINVERSION");
		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject()))
				.thenReturn(new PolicyCancellationPayloadBO());
		NotificationsDTO notificationsDTO = new NotificationsDTO();
		List<ContactDetailDTO> listContactDetailDTO = new ArrayList<>();
		ContactDetailDTO contactDetailDTO = new ContactDetailDTO();
		GenericContactDTO genericContactDTO = new GenericContactDTO();
		genericContactDTO.setContactDetailType("EMAIL");
		contactDetailDTO.setContact(genericContactDTO);
		listContactDetailDTO.add(contactDetailDTO);
		notificationsDTO.setContactDetails(listContactDetailDTO);
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(),new Timestamp(System.currentTimeMillis()));
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		input.setNotifications(notificationsDTO);
		EntityOutPolicyCancellationDTO validation1 = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation1);

		when(rbvdR042.executeICR4(anyObject())).thenReturn("ERROR");
		EntityOutPolicyCancellationDTO validation2 = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation2);

		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject()))
				.thenThrow(new BusinessException("01020052", false, "Mensaje Error"));
		validation2 = rbvdR011.executePolicyCancellation(input);
		assertNull(validation2);
	}

	@Test
	public void executePolicyCancellationWithRefundTestOK() throws IOException{
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationWithRefundTestOK...");
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME, ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
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
