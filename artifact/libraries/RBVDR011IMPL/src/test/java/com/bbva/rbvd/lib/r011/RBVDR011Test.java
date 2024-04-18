package com.bbva.rbvd.lib.r011;

import static com.bbva.rbvd.cancellationRequest.CancellationRequestImplTest.*;
import static com.bbva.rbvd.hostConnections.ICF2ConnectionTest.buildICF2ResponseOk;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.APPLICATION_DATE;
import static com.bbva.rbvd.lib.r011.impl.utils.CancellationTypes.END_OF_VALIDATY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bbva.apx.exception.business.BusinessException;
import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;

import com.bbva.pisd.lib.r100.PISDR100;
import com.bbva.pisd.lib.r103.PISDR103;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICMF1S2;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICMF3S0;
import com.bbva.rbvd.dto.insurancecancelation.bo.CancelationSimulationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.DatoParticularBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.*;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.lib.r011.impl.business.CancellationBusiness;
import com.bbva.rbvd.lib.r011.impl.business.CancellationRequestImpl;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF2Connection;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICF3Connection;
import com.bbva.rbvd.lib.r011.impl.hostConnections.ICR4Connection;
import com.bbva.rbvd.lib.r011.impl.service.dao.BaseDAO;
import com.bbva.rbvd.lib.r011.impl.transform.map.CancellationMapper;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r042.RBVDR042;
import com.bbva.rbvd.lib.r051.RBVDR051;
import com.bbva.rbvd.lib.r305.RBVDR305;
import com.bbva.rbvd.lib.r310.RBVDR310;
import com.bbva.rbvd.lib.r311.RBVDR311;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbva.elara.domain.transaction.Advice;
import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.ThreadContext;
import com.bbva.pisd.dto.insurance.utils.PISDErrors;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
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
	private ApplicationConfigurationService applicationConfigurationService;
	private PISDR100 pisdr100;
	private PISDR103 pisdr103;
	private RBVDR042 rbvdR042;
	private RBVDR051 rbvdR051;
	private RBVDR311 rbvdr311;
	private PISDR401 pisdR401;
	private RBVDR310 rbvdR310;
	private CancellationRequestImpl cancellationRequestImpl;
	private ICR4Connection icr4Connection;
	private ICF2Connection icf2Connection;
	private ICF3Connection icf3Connection;
	private ICF2Response icf2Response;
	private BaseDAO baseDAO;
	private RBVDR305 rbvdR305;

	@Before
	public void setUp() {
		ThreadContext.set(new Context());

		applicationConfigurationService = mock(ApplicationConfigurationService.class);
		pisdr100 = mock(PISDR100.class);
		pisdr103 = mock(PISDR103.class);
		rbvdR042 = mock(RBVDR042.class);
		rbvdR051 = mock(RBVDR051.class);
		rbvdr311 = mock(RBVDR311.class);
		pisdR401 = mock(PISDR401.class);
		rbvdR310 = mock(RBVDR310.class);
		cancellationRequestImpl = mock(CancellationRequestImpl.class);
		icr4Connection = mock(ICR4Connection.class);
		icf2Connection = mock(ICF2Connection.class);
		icf3Connection = mock(ICF3Connection.class);
		rbvdR305 = mock(RBVDR305.class);


		rbvdR011.setApplicationConfigurationService(applicationConfigurationService);
		rbvdR011.setPisdR100(pisdr100);
		rbvdR011.setPisdR103(pisdr103);
		rbvdR011.setRbvdR042(rbvdR042);
		rbvdR011.setRbvdR051(rbvdR051);
		rbvdR011.setRbvdR311(rbvdr311);
		rbvdR011.setPisdR401(pisdR401);
		rbvdR011.setRbvdR310(rbvdR310);
		rbvdR011.setCancellationRequestImpl(cancellationRequestImpl);
		rbvdR011.setIcr4Connection(icr4Connection);
		rbvdR011.setIcf2Connection(icf2Connection);
		rbvdR011.setIcf3Connection(icf3Connection);
		rbvdR011.setRbvdR305(rbvdR305);


		spyRbvdR011 = spy(rbvdR011);
		spyRbvdR011.setApplicationConfigurationService(applicationConfigurationService);
		spyRbvdR011.setRbvdR311(rbvdr311);
		spyRbvdR011.setPisdR100(pisdr100);
		spyRbvdR011.setPisdR103(pisdr103);
		spyRbvdR011.setRbvdR051(rbvdR051);
		spyRbvdR011.setRbvdR310(rbvdR310);
		spyRbvdR011.setPisdR401(pisdR401);
		spyRbvdR011.setRbvdR042(rbvdR042);
		spyRbvdR011.setCancellationRequestImpl(cancellationRequestImpl);
		spyRbvdR011.setIcr4Connection(icr4Connection);
		spyRbvdR011.setIcf2Connection(icf2Connection);
		spyRbvdR011.setIcf3Connection(icf3Connection);
		spyRbvdR011.setRbvdR305(rbvdR305);


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

		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");

		icf2Response = new ICF2Response();
		ICMF1S2 icmf1S2 = new ICMF1S2();
		icmf1S2.setCODPROD("801");
		icf2Response.setIcmf1S2(icmf1S2);


		when(rbvdR051.executePolicyCancellation(anyObject())).thenReturn(ifc3Response);
		when(rbvdR042.executeICR4(anyObject())).thenReturn("OK");
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(rbvdR310.executeICF2(anyObject())).thenReturn(icf2Response);
		when(applicationConfigurationService.getDefaultProperty(RBVDConstants.MASSIVE_PRODUCTS_LIST,",")).thenReturn("1121,");


		baseDAO = new BaseDAO(this.pisdr103, this.pisdr100, this.cancellationRequestImpl, this.applicationConfigurationService);


		when(applicationConfigurationService.getProperty("notification.config.email.notificationTypeRequestCancellation")).thenReturn("65fc61d0233e735e5ba80031");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.description.email")).thenReturn("Recibimos tu solicitud de cancelación del seguro 86600, la cual está siendo procesada y será atendida en un plazo máximo de 1 día hábil.");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.addDescription.email")).thenReturn("¡Te mantendremos informado!");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.title.email")).thenReturn("Datos importantes");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.applicationDate.email")).thenReturn("Fecha de solicitud");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.applicationNumber.email")).thenReturn("Número de Solicitud");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.certificateNumber.email")).thenReturn("Número de Certificado");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.planType.email")).thenReturn("Tipo de Plan");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.advice.email")).thenReturn("¡No te quedes sin la protección de tu Seguro!");
		when(applicationConfigurationService.getProperty("notificationTypeRequestCancellation.additionalInformation.email")).thenReturn("Recuerda los beneficios que estarías perdiendo dando clic al siguiente botón");

	}

	@Test
	public void executePolicyCancellationApplicationDAte() {
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildPolicyMap();
		Map<String,Object> product = buildProductMap();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(null);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn(",");
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);


		rbvdR011.executePolicyCancellation(input);
	}

	@Test
	public void executePolicyCancellationTestNull_ICF3responsesWithError() {
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull_ICF3responsesWithError...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		Map<String,Object> product = buildProductMap();

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn(",");
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(icf3Connection.executeICF3Transaction(anyObject(),anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(null);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationTestNull_CancelledPolicy(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull_CancelledPolicy...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildCancelledPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		Map<String,Object> product = buildProductMap();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn(",");
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(icf3Connection.executeICF3Transaction(anyObject(),anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationTestNull_AnnulledPolicy(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestNull_AnnulledPolicy...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildAnnulledPolicy();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		Map<String,Object> product = buildProductMap();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn(",");
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(icf3Connection.executeICF3Transaction(anyObject(),anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executeImmediateRoyalPolicyCancellationTestOK(){
		LOGGER.info("PISDR011Test - Executing executeImmediateRoyalPolicyCancellationTestOK...");

		Map<String,Object> product = buildProductMap();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(rbvdR305.executeSendingEmail(anyObject())).thenReturn(201);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executeImmediateNoRoyalPolicyCancellationTestOK(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationTestOK...");

		Map<String,Object> product = buildProductMap();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseOk();
		ICF2Response icf2Response = buildICF2ResponseOk();
		PolicyCancellationPayloadBO policyCancellationPayloadBO = new PolicyCancellationPayloadBO();
		policyCancellationPayloadBO.setAutorizarRetiro(RBVDConstants.TAG_S);
		Map<String, Object> policy = buildPolicyMap();

		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		List<Advice> advices = new ArrayList<>();
		Advice advice = new Advice();
		advices.add(advice);
		advice.setCode(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode());
		when(spyRbvdR011.getAdviceList()).thenReturn(advices);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(policyCancellationPayloadBO);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(rbvdR305.executeSendingEmail(anyObject())).thenReturn(201);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executeImmediateNoRoyalPolicyCancellationNoProductCode(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationTestOK...");

		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		ICF2Response icf2Response = buildICF2ResponseOk();
		icf2Response.getIcmf1S2().setPRODRI(null);

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);

		assertNull(validation);
		assertTrue(spyRbvdR011.getAdviceList().stream().anyMatch(x -> x.getCode().equals(RBVDErrors.ERROR_INVALID_INPUT_SIMULATECANCELATION.getAdviceCode())));
	}

	@Test
	public void executeImmediateNoRoyalPolicyCancellationRimacFailed(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationTestOK...");

		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		ICF2Response icf2Response = buildICF2ResponseOk();

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(null);
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);

		assertNull(validation);
		assertTrue(spyRbvdR011.getAdviceList().stream().anyMatch(x -> x.getCode().equals(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_RIMAC.getAdviceCode())));
	}

	@Test
	public void executeImmediateNoRoyalPolicyCancellationRimacNotAuthorizedCancellation(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationTestOK...");

		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		ICF2Response icf2Response = buildICF2ResponseOk();
		PolicyCancellationPayloadBO policyCancellationPayloadBO = new PolicyCancellationPayloadBO();
		policyCancellationPayloadBO.setAutorizarRetiro(RBVDConstants.TAG_N);

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(policyCancellationPayloadBO);
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);

		assertNull(validation);
		//verify(icf3Connection, times(1)).executeICF3Transaction(input, responseGetRequestCancellation, null, icf2Response, "", RBVDConstants.TAG_N);
	}

	@Test
	public void executeImmediateNoRoyalPolicyCancellationRimacTestOk(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationTestOK...");

		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		ICF2Response icf2Response = buildICF2ResponseOk();
		PolicyCancellationPayloadBO policyCancellationPayloadBO = new PolicyCancellationPayloadBO();
		policyCancellationPayloadBO.setAutorizarRetiro(RBVDConstants.TAG_S);

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(policyCancellationPayloadBO);
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	//@Test
	public void executeImmediateNoRoyalPolicyCancellationRimacException(){
		LOGGER.info("PISDR011Test - Executing executeImmediateNoRoyalPolicyCancellationRimacException...");

		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		ICF2Response icf2Response = buildICF2ResponseOk();
		PolicyCancellationPayloadBO policyCancellationPayloadBO = new PolicyCancellationPayloadBO();
		policyCancellationPayloadBO.setAutorizarRetiro(RBVDConstants.TAG_S);

		when(applicationConfigurationService.getProperty("cancellation.legacy.flow")).thenReturn("false");
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(null);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, null, false)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenThrow(new BusinessException("RBVD0000000", false));
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);

		assertTrue(spyRbvdR011.getAdviceList().stream().anyMatch(x -> x.getCode().equals(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_RIMAC.getAdviceCode())));
		assertNull(validation);
	}


	@Test
	public void executePolicyCancellationTestOK_Refund(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(false);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);

		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyalOk(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyalOk...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyalOk_WithoutLastRequestCancellationMov(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyalOk_WithoutLastRequestCancellationMov...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();

		executeCancellationBDoperationsOk(null, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(this.applicationConfigurationService.getProperty(Mockito.anyString())).thenReturn("false").thenReturn("true").thenReturn("TRUE");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		//assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyalOk_WithoutStartDate(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyalOk_WithoutStartDate...");
		Map<String, Object> policy = buildPolicyMap();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), null);
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyal_WithCanceledCancellationRequest(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyal_WithOpenCancellationRequest...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastBaj();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(this.applicationConfigurationService.getProperty(Mockito.anyString())).thenReturn("false").thenReturn("false").thenReturn("true");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyal_WithOpenCancellationRequest(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyal_WithOpenCancellationRequest...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLastOpen = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastBaj();

		executeCancellationBDoperationsOk(requestCancellationMovLastOpen, policy, responseGetRequestCancellation);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false").thenReturn("true");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executeCancellationForRoyal_WithMassiveProductAndApplicationDateAndNotRefund(){
		LOGGER.info("PISDR011Test - Executing executeCancellationForRoyal_WithMassiveProductAndApplicationDateAndNotRefund...");
		Map<String, Object> policy = buildPolicyMap();
		policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(),"1121");
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLastOpen = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.setCancellationType(APPLICATION_DATE.name());
		input.setIsRefund(false);
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLastOpen, policy, responseGetRequestCancellation);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.1.pc")).thenReturn("false");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyal_WithoutRequestCancellationMovLast(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyal_WithoutRequestCancellationMovLast...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponse();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(null);
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(this.applicationConfigurationService.getProperty(Mockito.anyString())).thenReturn("false").thenReturn("false").thenReturn("true");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}

	@Test
	public void executeOnlyCancellationRequestForRoyalOk(){
		LOGGER.info("PISDR011Test - Executing executeOnlyCancellationRequestForRoyalOk...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false").thenReturn("true").thenReturn("true");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		//assertNotNull(validation);
	}

	//@Test
	public void executeOnlyCancellationRequestForRoyalOk_WithoutCancellationType(){
		LOGGER.info("PISDR011Test - Executing executeOnlyCancellationRequestForRoyalOk_WithoutCancellationType...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.setCancellationType(null);

		Map<String, Object> mapp = new HashMap<>();
		mapp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		mapp.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), "733");
		mapp.put(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue(), "descripcion");
		mapp.put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), "122");

		Map<String, Object> mappp = new HashMap<>();
		mappp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		ICF2Response icf2Response = buildICF2ResponseOk();
		when(rbvdR310.executeICF2(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		//when(CancellationMapper.mapInRequestCancellationMov(anyObject(), anyObject(), anyString(), anyInt())).thenReturn(mappp);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);

		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyal_SimulationResponseWithCardData_Ok(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyal_SimulationResponseWithCardData_Ok...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponseWithCardData();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);

	}

	@Test
	public void executeCancellationRequestAndCancellationForRoyal_ICR4RespondsWithError(){
		LOGGER.info("PISDR011Test - Executing executeCancellationRequestAndCancellationForRoyal_ICR4RespondsWithError...");
		Map<String, Object> policy = buildPolicyMap();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		CancelationSimulationPayloadBO payload = buildCancelationSimulationResponseWithCardData();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(false);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("false");
		when(rbvdr311.executeSimulateCancelationRimac(anyObject())).thenReturn(payload);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNull(validation);
	}


	//@Test
	public void executeEndOfValidityCancellationForRoyalOk(){
		LOGGER.info("PISDR011Test - Executing executeEndOfValidityCancellationOk...");
		InputParametersPolicyCancellationDTO input = buildEndOfValidityCancellationInput();
		Map<String,Object> product = buildProductMap();
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(rbvdR310.executeICF2(anyObject())).thenReturn(buildICF2ResponseOk());
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(false);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());

		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executeEndOfValidityCancellationForNoRoyalOk(){
		LOGGER.info("PISDR011Test - Executing executeEndOfValidityCancellationOk...");
		InputParametersPolicyCancellationDTO input = buildEndOfValidityCancellationInput();
		Map<String,Object> product = buildProductMap();
		Map<String, Object> policy = null;
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();
		PolicyCancellationPayloadBO policyCancellationPayloadBO = new PolicyCancellationPayloadBO();
		policyCancellationPayloadBO.setAutorizarRetiro(RBVDConstants.TAG_S);
		List<Advice> advices = new ArrayList<>();
		Advice advice = new Advice();
		advices.add(advice);
		advice.setCode(PISDErrors.QUERY_EMPTY_RESULT.getAdviceCode());
		when(spyRbvdR011.getAdviceList()).thenReturn(advices);

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, false)).thenReturn(false);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(true);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(policyCancellationPayloadBO);
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(buildICF2Response());
		EntityOutPolicyCancellationDTO validation = spyRbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executeEndOfValidityCancellationForNoRoyalError(){
		LOGGER.info("PISDR011Test - Executing executeEndOfValidityCancellationForNoRoyalError...");
		InputParametersPolicyCancellationDTO input = buildEndOfValidityCancellationInput();
		Map<String,Object> product = buildProductMap();
		Map<String, Object> policy = null;
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3Response = new EntityOutPolicyCancellationDTO();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(false);
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3Response);
		when(icr4Connection.executeICR4Transaction(anyObject(), anyString())).thenReturn(false);
		when(rbvdr311.executeCancelPolicyRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNull(validation);
	}

	@Test
	public void executePolicyCancellationInvestmentTestOK_Refund(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationInvestmentTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC, "VIDAINVERSION");
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationInvestmentTestOKNotification(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationInvestmentTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.getInsurerRefund().getPaymentMethod().getContract().setId("00110130220210452319");
		input.getNotifications().getContactDetails().get(0).setContact(null);
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC, "VIDAINVERSION");
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}
	@Test
	public void executePolicyCancellationInvestmentTestOKNotificationDetails(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationInvestmentTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.getInsurerRefund().getPaymentMethod().getContract().setId("00110130220210452319");
		input.getNotifications().getContactDetails().clear();
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC, "VIDAINVERSION");
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationInvestmentTestOKNotificationNull(){
		LOGGER.info("PISDR011Test - Executing executePolicyCancellationInvestmentTestOK_Refund...");
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.getInsurerRefund().getPaymentMethod().getContract().setId("00110130220110452319");
		input.setNotifications(null);
		Map<String, Object> policy = buildPolicyMap();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		EntityOutPolicyCancellationDTO icf3MappedResponse = buildInsuranceCancellationResponseRefundOk();
		Map<String,Object> product = new HashMap<>();
		product.put(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC, "VIDAINVERSION");
		product.put(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME,"VIDA");
		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(pisdR401.executeGetProductById(anyString(), any())).thenReturn(product);
		when(rbvdr311.executeRescueCancelationRimac(anyObject(), anyObject())).thenReturn(new PolicyCancellationPayloadBO());
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(icf3MappedResponse);
		when(applicationConfigurationService.getProperty("cancellation.request.01.pc")).thenReturn("false");
		when(applicationConfigurationService.getProperty("cancellation.list.endoso")).thenReturn("PC,");
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);
		assertNotNull(validation);
	}

	@Test
	public void executePolicyCancellationTodayLegacyOk(){
		LOGGER.info("PISDR011Test - Executing executeOnlyCancellationRequestForRoyalOk_WithoutCancellationType...");
		Map<String, Object> policy = buildPolicyMap();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), LocalDateTime.now().format(formatter));
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.setCancellationType(null);

		Map<String, Object> mapp = new HashMap<>();
		mapp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		mapp.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), "733");
		mapp.put(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue(), "descripcion");
		mapp.put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), "122");

		Map<String, Object> mappp = new HashMap<>();
		mappp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		ICF2Response icf2Response = buildICF2Response();

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		//hen(CancellationMapper.mapInRequestCancellationMov(anyObject(), anyObject(), anyString(), anyInt())).thenReturn(mappp);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}


	@Test
	public void executePolicyCancellationTodayLegacyICF2FailedOk(){
		LOGGER.info("PISDR011Test - Executing executeOnlyCancellationRequestForRoyalOk_WithoutCancellationType...");
		Map<String, Object> policy = buildPolicyMap();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), LocalDateTime.now().format(formatter));
		Map<String, Object> responseGetRequestCancellation = buildResponseGetRequestCancellation();
		List<Map<String, Object>> requestCancellationMovLast = buildOpenRequestCancellationMovLastOpen();
		InputParametersPolicyCancellationDTO input = buildImmediateCancellationInput_EmailContactAndPhoneContact();
		input.setCancellationType(null);

		Map<String, Object> mapp = new HashMap<>();
		mapp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		mapp.put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), "733");
		mapp.put(RBVDProperties.FIELD_ADDITIONAL_DATA_DESC.getValue(), "descripcion");
		mapp.put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), "122");

		Map<String, Object> mappp = new HashMap<>();
		mappp.put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		ICF2Response icf2Response = buildICF2Response();
		//icf2Response.setIcmf1S2(null);

		executeCancellationBDoperationsOk(requestCancellationMovLast, policy, responseGetRequestCancellation);
		when(icf2Connection.executeICF2Transaction(anyObject())).thenReturn(icf2Response);
		when(cancellationRequestImpl.validateNewCancellationRequest(input, policy, true)).thenReturn(true);
		when(cancellationRequestImpl.executeFirstCancellationRequest(anyObject(), anyMap(), anyBoolean(), anyObject(), anyObject(), anyString(), anyString())).thenReturn(true);
		when(applicationConfigurationService.getProperty(anyString())).thenReturn("true");
		when(icf3Connection.executeICF3Transaction(anyObject(), anyMap(), anyMap(), anyObject(), anyString(), anyString())).thenReturn(new EntityOutPolicyCancellationDTO());
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(buildOpenRequestCancellationMovLastOpen());
		//when(CancellationMapper.mapInRequestCancellationMov(anyObject(), anyObject(), anyString(), anyInt())).thenReturn(mappp);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		EntityOutPolicyCancellationDTO validation = rbvdR011.executePolicyCancellation(input);

		assertNotNull(validation);
	}



	private InputParametersPolicyCancellationDTO buildEndOfValidityCancellationInput(){
		InputParametersPolicyCancellationDTO input = new InputParametersPolicyCancellationDTO();
		input.setContractId("11111111111111111111");
		input.setChannelId("PC");
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		input.setReason(reason);
		input.setCancellationType(END_OF_VALIDATY.name());
		input.setNotifications(new NotificationsDTO());
		input.getNotifications().setContactDetails(new ArrayList<>());
		input.getNotifications().getContactDetails().add(new ContactDetailDTO());
		input.getNotifications().getContactDetails().get(0).setContact(new GenericContactDTO());
		input.getNotifications().getContactDetails().get(0).getContact().setContactDetailType(RBVDProperties.CONTACT_EMAIL_ID.getValue());
		input.getNotifications().getContactDetails().get(0).getContact().setNumber("CARLOS.CARRILLO.DELGADO@BBVA.COM");
		input.getNotifications().getContactDetails().get(0).getContact().setUsername("SERGIO");
		input.getNotifications().getContactDetails().add(new ContactDetailDTO());
		input.getNotifications().getContactDetails().get(1).setContact(new GenericContactDTO());
		input.getNotifications().getContactDetails().get(1).getContact().setContactDetailType(RBVDProperties.CONTACT_MOBILE_ID.getValue());
		input.getNotifications().getContactDetails().get(1).getContact().setNumber("998877669");
		input.getNotifications().getContactDetails().add(new ContactDetailDTO());
		input.getNotifications().getContactDetails().get(2).setContact(new GenericContactDTO());
		input.getNotifications().getContactDetails().get(2).getContact().setContactDetailType("SOCIAL_MEDIA");
		input.getNotifications().getContactDetails().get(2).getContact().setUsername("SERGIO");
		input.setIsRefund(true);
		return input;
	}

	public static Map<String, Object> buildPolicyMap(){
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), "123456");
		policy.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), "12345678");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), "08-09-2021 00:00:00");
		policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(),"2101");
		policy.put(RBVDProperties.KEY_RESPONSE_PAYMENT_FREQUENCY_NAME.getValue(), "MENSUAL");
		policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_DESC.getValue(), "SEGURO VEHICULAR BBVA");
		return policy;
	}
	public static Map<String, Object> buildPolicyMapNull(){
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), "FOR");
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.FIELD_INSURANCE_MODALITY_TYPE.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), null);
		policy.put(RBVDProperties.FIELD_CUSTOMER_ID.getValue(), "12345678");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), "08-09-2021 00:00:00");
		policy.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(),null);
		return policy;
	}

	public static List<Map<String, Object>> buildOpenRequestCancellationMovLastOpen(){
		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "01");
		return requestCancellationMovLast;
	}

	public static List<Map<String, Object>> buildOpenRequestCancellationMovLastOpenContract(){
		Map<String, Object> requestCancellationMovLast = new HashMap<>();
		requestCancellationMovLast.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_ENTITY_ID.getValue(), "0011");
		requestCancellationMovLast.put(RBVDProperties.FIELD_INSURANCE_CONTRACT_BRANCH_ID.getValue(), "0182");
		requestCancellationMovLast.put(RBVDProperties.FIELD_INSRC_CONTRACT_INT_ACCOUNT_ID.getValue(), "4000000346");

		List<Map<String, Object>> list = new ArrayList<>();
		list.add(requestCancellationMovLast);
		return list;
	}

	private List<Map<String, Object>> buildOpenRequestCancellationMovLastBaj(){
		List<Map<String, Object>> requestCancellationMovLast = new ArrayList<>();
		requestCancellationMovLast.add(new HashMap<>());
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_REQUEST_SEQUENCE_ID.getValue(), BigDecimal.valueOf(123));
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_SEQ_MOV_NUMBER.getValue(), 1);
		requestCancellationMovLast.get(0).put(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue(), "03");
		return requestCancellationMovLast;
	}


	public static Map<String, Object> buildResponseGetRequestCancellation(){
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), new Timestamp(System.currentTimeMillis()));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), new BigDecimal(0));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), new BigDecimal(0));
		return responseGetRequestCancellation;
	}
	public static Map<String, Object> buildResponseGetRequestCancellationNull(){
		Map<String, Object> responseGetRequestCancellationNull = new HashMap<>();
		responseGetRequestCancellationNull.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), null);
		responseGetRequestCancellationNull.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), null);
		return responseGetRequestCancellationNull;
	}

	public static Map<String, Object> buildResponseGetRequestCancellationWithoutCancelPolicyDate(){
		Map<String, Object> responseGetRequestCancellation = new HashMap<>();
		responseGetRequestCancellation.put(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue(), null);
		responseGetRequestCancellation.put(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue(), new BigDecimal(0));
		responseGetRequestCancellation.put(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue(), new BigDecimal(0));
		return responseGetRequestCancellation;
	}

	private void executeCancellationBDoperationsOk(List<Map<String, Object>> requestCancellationMovLast, Map<String, Object> policy,
												   Map<String, Object> responseGetRequestCancellation){
		when(pisdr103.executeGetRequestCancellationMovLast(anyMap())).thenReturn(requestCancellationMovLast);
		when(pisdr103.executeSaveInsuranceRequestCancellationMov(anyMap())).thenReturn(1);
		when(pisdr100.executeGetPolicyNumber(anyString(), anyString())).thenReturn(policy);
		when(pisdr100.executeSaveContractMovement(anyMap())).thenReturn(true);
		when(pisdr100.executeSaveContractCancellation(anyMap())).thenReturn(true);
		when(pisdr100.executeUpdateContractStatus(anyMap())).thenReturn(1);
		when(pisdr100.executeUpdateReceiptsStatusV2(anyMap())).thenReturn(1);
		when(pisdr103.executeGetRequestCancellation(anyMap())).thenReturn(responseGetRequestCancellation);
	}

	public static Map<String, Object> buildCancelledPolicyMap(){
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_BAJ);
		policy.put(RBVDProperties.KEY_REQUEST_CREATION_DATE.getValue(), "2021-08-09 18:04:42.36226");
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), "01-01-2021 12:00:00");
		return policy;
	}

	private Map<String, Object> buildAnnulledPolicy(){
		Map<String, Object> policy = new HashMap<>();
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), RBVDConstants.TAG_ANU);
		policy.put(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue(), "1");
		policy.put(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue(), "01-01-2021 12:00:00");
		return policy;
	}
	private EntityOutPolicyCancellationDTO buildInsuranceCancellationResponseRefundOk(){
		EntityOutPolicyCancellationDTO out = new EntityOutPolicyCancellationDTO();
		out.setId("00110172444000017959202311071631");
		InsurerRefundCancellationDTO insurerRefund = new InsurerRefundCancellationDTO();
		insurerRefund.setCurrency("PEN");
		insurerRefund.setAmount(15.00);
		out.setInsurerRefund(insurerRefund);
		out.setCustomerRefund(insurerRefund);
		GenericStatusDTO status = new GenericStatusDTO();
		status.setDescription("REFUND");
		status.setId("REFUND");
		out.setStatus(status);
		out.setCancellationDate(Calendar.getInstance());
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		reason.setDescription("PETICION DEL ASEGURADO");
		out.setReason(reason);
		ExchangeRateDTO exchangeRate = new ExchangeRateDTO();
		exchangeRate.setBaseCurrency("PEN");
		exchangeRate.setTargetCurrency("PEN");
		exchangeRate.setCalculationDate(new Date());
		exchangeRate.setValue(0.0);
		out.setExchangeRate(exchangeRate);
		NotificationsDTO notifications = new NotificationsDTO();
		List<ContactDetailDTO> contactDetails = new ArrayList<>();
		ContactDetailDTO contactDetail = new ContactDetailDTO();
		GenericContactDTO contact = new GenericContactDTO();
		contact.setAddress("CARLOS.CARRILLO.DELGADO@BBVA.COM");
		contact.setContactDetailType("EMAIL");
		contactDetail.setContact(contact);
		contactDetails.add(contactDetail);
		notifications.setContactDetails(contactDetails);
		out.setNotifications(notifications);
		return out;
	}
	private EntityOutPolicyCancellationDTO buildInsuranceCancellationResponseOk(){
		EntityOutPolicyCancellationDTO out = new EntityOutPolicyCancellationDTO();
		out.setId("00110172444000017959202311071631");
		InsurerRefundCancellationDTO insurerRefund = new InsurerRefundCancellationDTO();
		insurerRefund.setCurrency("PEN");
		insurerRefund.setAmount(15.00);
		out.setInsurerRefund(insurerRefund);
		out.setCustomerRefund(insurerRefund);
		GenericStatusDTO status = new GenericStatusDTO();
		status.setDescription("COMPLETED");
		status.setId("COMPLETED");
		out.setStatus(status);
		out.setCancellationDate(Calendar.getInstance());
		GenericIndicatorDTO reason = new GenericIndicatorDTO();
		reason.setId("01");
		reason.setDescription("PETICION DEL ASEGURADO");
		out.setReason(reason);
		ExchangeRateDTO exchangeRate = new ExchangeRateDTO();
		exchangeRate.setBaseCurrency("PEN");
		exchangeRate.setTargetCurrency("PEN");
		exchangeRate.setCalculationDate(new Date());
		exchangeRate.setValue(0.0);
		out.setExchangeRate(exchangeRate);
		NotificationsDTO notifications = new NotificationsDTO();
		List<ContactDetailDTO> contactDetails = new ArrayList<>();
		ContactDetailDTO contactDetail = new ContactDetailDTO();
		GenericContactDTO contact = new GenericContactDTO();
		contact.setAddress("CARLOS.CARRILLO.DELGADO@BBVA.COM");
		contact.setContactDetailType("EMAIL");
		contactDetail.setContact(contact);
		contactDetails.add(contactDetail);
		notifications.setContactDetails(contactDetails);
		out.setNotifications(notifications);
		return out;
	}

	private Map<String, Object> buildProductMap(){
		Map<String,Object> product = new HashMap<>();
		product.put("VIDA", "EASYYES");
		return product;
	}

	private CancelationSimulationPayloadBO buildCancelationSimulationResponseWithCardData(){
		CancelationSimulationPayloadBO response = new CancelationSimulationPayloadBO();
		response.setFechaAnulacion(Calendar.getInstance().getTime());
		response.setExtornoComision(0.00);
		response.setMoneda("USD");
		DatoParticularBO cuenta = new DatoParticularBO();
		cuenta.setValor("TARJETA||***5085||PEN");
		response.setCuenta(cuenta);
		return response;
	}

	private ICF2Response buildICF2Response(){
		ICF2Response response = new ICF2Response();

		ICMF1S2 icmf1S2 = new ICMF1S2();
		icmf1S2.setCODPROD("801");
		icmf1S2.setIMPCLIE(15.00);
		icmf1S2.setDIVIMC("PEN");
		icmf1S2.setCODCLI("12345678");
		icmf1S2.setIMPCOMI(5.00);
		icmf1S2.setDIVDCIA("PEN");
		icmf1S2.setPRODRI("00002121");
		icmf1S2.setNUMPOL("0000000000");
		icmf1S2.setTIPCONT("001");
		icmf1S2.setDESCONT("test@email.com");
		icmf1S2.setNOMSEGU("SEGURO VIDA RENTA");
		response.setIcmf1S2(icmf1S2);
		return response;
	}
}