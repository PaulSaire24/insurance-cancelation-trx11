package com.bbva.rbvd.lib.r011;

import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;

import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICMF3S0;
import com.bbva.rbvd.dto.insurancecancelation.aso.cancelationsimulation.CancelationSimulationASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.DatoParticularBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.cancelationsimulation.CancelationSimulationHostBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericContactDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r042.RBVDR042;
import com.bbva.rbvd.lib.r051.RBVDR051;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.joda.time.LocalDate;
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
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.RBVDR011Impl;

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
	private RBVDR311 rbvdr311;
	private PISDR100 pisdr100;
	private PISDR103 pisdr103;
	private RBVDR042 rbvdR042;
	private RBVDR051 rbvdR051;
	private PISDR401 pisdR401;
	private ApplicationConfigurationService applicationConfigurationService;

	@Before
	public void setUp() {
		ThreadContext.set(new Context());
		spyRbvdR011 = spy(rbvdR011);

		rbvdr311 = mock(RBVDR311.class);
		rbvdR011.setRbvdR311(rbvdr311);
		spyRbvdR011.setRbvdR311(rbvdr311);

		applicationConfigurationService = mock(ApplicationConfigurationService.class);
		rbvdR011.setApplicationConfigurationService(applicationConfigurationService);
		spyRbvdR011.setApplicationConfigurationService(applicationConfigurationService);

		pisdr100 = mock(PISDR100.class);
		rbvdR011.setPisdR100(pisdr100);
		spyRbvdR011.setPisdR100(pisdr100);

		pisdr103 = mock(PISDR103.class);
		rbvdR011.setPisdR103(pisdr103);
		spyRbvdR011.setPisdR103(pisdr103);

		rbvdR042 = mock(RBVDR042.class);
		rbvdR011.setRbvdR042(rbvdR042);
		spyRbvdR011.setRbvdR042(rbvdR042);

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
		ICF3Response  ifc3Response = new ICF3Response();
		ifc3Response.setIcmf3s0(icmf3s0);
		ifc3Response.setHostAdviceCode(null);
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);
		spyRbvdR011.setRbvdR051(rbvdR051);

		pisdR401 = mock(PISDR401.class);
		rbvdR011.setPisdR401(pisdR401);
		spyRbvdR011.setPisdR401(pisdR401);
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
	}
	
	@Test
	public void executePolicyCancellationTestNull(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");

		input.setChannelId("PC");
		input.setCancellationType("test");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("1");

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);

		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);

		ICF3Response  ifc3Response = new ICF3Response();
		ifc3Response.setHostAdviceCode("00000169");
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);

		ICMF3S0 icmf3s0 = new ICMF3S0();
		icmf3s0.setIDSTCAN("1");
		icmf3s0.setDESSTCA("OK");
		icmf3s0.setIMDECIA(0);
		icmf3s0.setIMPCLIE(0);
		icmf3s0.setTIPCAMB(0);
		icmf3s0.setFETIPCA("2023-11-03");
		icmf3s0.setDESSTCA("COMPLETED");
		ifc3Response.setIcmf3s0(icmf3s0);
		ifc3Response.setHostAdviceCode(null);
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);

		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_ANU);
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
		
		policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_BAJ);
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationTestOK(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK...");

		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME, ConstantsUtil.BUSINESS_NAME_FAKE_EASYYES);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		input.setCancellationType("test");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);

		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);

		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

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
	public void executePolicyCancellationTestOK_Refund(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		input.setCancellationType("test");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "8");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(applicationConfigurationService.getProperty("cancellation.request.8.PC")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");

		ICMF3S0 icmf3s0 = new ICMF3S0();
		icmf3s0.setIDSTCAN("1");
		icmf3s0.setDESSTCA("OK");
		icmf3s0.setIMDECIA(0);
		icmf3s0.setIMPCLIE(0);
		icmf3s0.setTIPCAMB(0);
		icmf3s0.setFETIPCA("2023-11-03");
		icmf3s0.setDESSTCA("REFUND");
		ICF3Response  ifc3Response = new ICF3Response();
		ifc3Response.setIcmf3s0(icmf3s0);
		ifc3Response.setHostAdviceCode(null);
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);

		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		input.setChannelId("PC");
		input.setUserId("UI");
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}
	
	@Test
	public void executePolicyCancellationTestBDEmptyResult(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestBDEmptyResult...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		input.setCancellationType("test");

		GenericIndicatorDTO genericDTO = new GenericIndicatorDTO();
		genericDTO.setId("1");
		input.setReason(genericDTO);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		List<Advice> advices = new ArrayList<>();
		Advice advice = new Advice();
		advices.add(advice);
		advice.setCode(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode());
		when(spyRbvdR011.getAdviceList()).thenReturn(advices);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(null);

		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestOK(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), new BigDecimal("0.01"));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), new BigDecimal("0.01"));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(null);
		CancelationSimulationPayloadBO payload = new CancelationSimulationPayloadBO();
		payload.setFechaAnulacion(Calendar.getInstance().getTime());
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
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
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestOK2(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		CancelationSimulationPayloadBO payload = new CancelationSimulationPayloadBO();
		payload.setFechaAnulacion(Calendar.getInstance().getTime());
		payload.setExtornoComision(0.00);
		DatoParticularBO cuenta = new DatoParticularBO();
		cuenta.setValor("TARJETA||***5085||PEN");
		payload.setCuenta(cuenta);
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), RBVDConstants.MOV_BAJ);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), new BigDecimal("0.01"));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), new BigDecimal("0.01"));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		input.setNotifications(new NotificationsDTO());
		input.getNotifications().setContactDetails(new ArrayList<>());
		input.getNotifications().getContactDetails().add(new ContactDetailDTO());
		input.getNotifications().getContactDetails().get(0).setContact(new GenericContactDTO());
		input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_EMAIL_ID.getValue());
		input.getNotifications().getContactDetails().get(0).getContact().setAddress("example@example.com");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestError(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("ERROR");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(null);
		CancelationSimulationPayloadBO payload = new CancelationSimulationPayloadBO();
		payload.setFechaAnulacion(Calendar.getInstance().getTime());
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestError2(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		Date cancellationDate = new Date();
		CancelationSimulationASO cancelationSimulationASO = new CancelationSimulationASO();
		cancelationSimulationASO.setData(new CancelationSimulationHostBO());
		cancelationSimulationASO.getData().setCancelationDate(cancellationDate);
		cancelationSimulationASO.getData().setCustomerRefund(new GenericAmountDTO());
		cancelationSimulationASO.getData().getCustomerRefund().setAmount(123.0);
		cancelationSimulationASO.getData().getCustomerRefund().setCurrency("USD");

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(null);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestOK3(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		Date cancellationDate = new Date();
		Calendar cancellationCalendar = Calendar.getInstance();
		cancellationCalendar.setTime(cancellationDate);
		CancelationSimulationASO cancelationSimulationASO = new CancelationSimulationASO();
		cancelationSimulationASO.setData(new CancelationSimulationHostBO());
		cancelationSimulationASO.getData().setCancelationDate(cancellationDate);
		cancelationSimulationASO.getData().setCustomerRefund(new GenericAmountDTO());
		cancelationSimulationASO.getData().getCustomerRefund().setAmount(123.0);
		cancelationSimulationASO.getData().getCustomerRefund().setCurrency("USD");

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		EntityOutPolicyCancellationDTO outHost = new EntityOutPolicyCancellationDTO();
		outHost.setStatus(new GenericStatusDTO());
		outHost.getStatus().setDescription("REFUND");
		outHost.setCancellationDate(cancellationCalendar);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		input.setCancellationType("test");
		input.setCancellationDate(cancellationCalendar);
		NotificationsDTO notificationsDTO = new NotificationsDTO();
		List<ContactDetailDTO> listContactDetailDTO = new ArrayList<>();
		ContactDetailDTO contactDetailDTO = new ContactDetailDTO();
		GenericContactDTO genericContactDTO = new GenericContactDTO();
		genericContactDTO.setContactDetailType("EMAIL");
		contactDetailDTO.setContact(genericContactDTO);
		listContactDetailDTO.add(contactDetailDTO);
		notificationsDTO.setContactDetails(listContactDetailDTO);
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		input.setNotifications(notificationsDTO);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		assertEquals(cancellationDate, validation.getCancellationDate().getTime());
	}

	@Test
	public void executePolicyCancellationWhenIsCancellationRequestTestOK4(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), new LocalDate("2023-01-15"));

		Date cancellationDate = new Date();
		Calendar cancellationCalendar = Calendar.getInstance();
		cancellationCalendar.setTime(cancellationDate);

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		EntityOutPolicyCancellationDTO outHost = new EntityOutPolicyCancellationDTO();
		outHost.setStatus(new GenericStatusDTO());
		outHost.getStatus().setDescription("REFUND");
		outHost.setCancellationDate(cancellationCalendar);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		CancelationSimulationPayloadBO payload = new CancelationSimulationPayloadBO();
		payload.setFechaAnulacion(Calendar.getInstance().getTime());
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), RBVDConstants.MOV_BAJ);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationIsCancellationRequestTestError(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationIsCancellationRequestTestOK...");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), "123456");

		Date cancellationDate = new Date();
		CancelationSimulationASO cancelationSimulationASO = new CancelationSimulationASO();
		cancelationSimulationASO.setData(new CancelationSimulationHostBO());
		cancelationSimulationASO.getData().setCancelationDate(cancellationDate);
		cancelationSimulationASO.getData().setCustomerRefund(new GenericAmountDTO());
		cancelationSimulationASO.getData().getCustomerRefund().setAmount(123.0);
		cancelationSimulationASO.getData().getCustomerRefund().setCurrency("USD");

		Map<String, Object> responseGetRequestCancellationId = new HashMap<>();
		responseGetRequestCancellationId.put(RBVDProperties.FIELD_Q_PISD_REQUEST_SQUENCE_ID0_NEXTVAL.getValue(), new BigDecimal("123"));

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(pisdr103.executeGetRequestCancellationId()).thenReturn(responseGetRequestCancellationId);
		when(pisdr103.executeSaveInsuranceRequestCancellation(anyMap())).thenReturn(1);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		ICMF3S0 icmf3s0 = new ICMF3S0();
		icmf3s0.setIDSTCAN("2");
		icmf3s0.setDESSTCA("ERR");
		ICF3Response  ifc3Response = new ICF3Response();
		ifc3Response.setIcmf3s0(icmf3s0);
		ifc3Response.setHostAdviceCode("00000169");
		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);

		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("00110840020012345678");
		input.setChannelId("PC");
		input.setReason(new GenericIndicatorDTO());
		input.getReason().setId("01");
		input.setCancellationType("test");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}
	@Test
	public void executePolicyCancellationisCancellationTypeValidaty(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		input.setCancellationType("ABC");
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);

		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);

		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

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
	public void executePolicyCancellationUpdateContract(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK...");
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		input.setCancellationType(END_OF_VALIDATY.name());
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "0");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE.getValue(), "2021-08-09");

		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), Calendar.getInstance());
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);

		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());

		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
		input.setChannelId("PC");
		input.setUserId("UI");
		validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}
}