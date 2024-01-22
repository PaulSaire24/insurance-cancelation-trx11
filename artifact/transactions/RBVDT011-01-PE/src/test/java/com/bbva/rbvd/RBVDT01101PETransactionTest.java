package com.bbva.rbvd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbva.elara.domain.transaction.Context;
import com.bbva.elara.domain.transaction.Severity;
import com.bbva.elara.domain.transaction.request.TransactionRequest;
import com.bbva.elara.domain.transaction.request.body.CommonRequestBody;
import com.bbva.elara.domain.transaction.request.header.CommonRequestHeader;
import com.bbva.elara.test.osgi.DummyBundleContext;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.lib.r011.RBVDR011;

/**
 * Test for transaction RBVDT01101PETransaction
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:/META-INF/spring/elara-test.xml",
		"classpath:/META-INF/spring/RBVDT01101PETest.xml" })
public class RBVDT01101PETransactionTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDT01101PETransactionTest.class);

	@Spy
	@Autowired
	private RBVDT01101PETransaction transaction;

	@Resource(name = "dummyBundleContext")
	private DummyBundleContext bundleContext;
	
	@Resource(name = "rbvdR011")
    private RBVDR011 rbvdr011;

	@Mock
	private CommonRequestHeader header;

	@Mock
	private TransactionRequest transactionRequest;
	
	private MockDTO mockDTO;

	@Before
	public void initializeClass() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.transaction.start(bundleContext);
		this.transaction.setContext(new Context());
		CommonRequestBody commonRequestBody = new CommonRequestBody();
		commonRequestBody.setTransactionParameters(new ArrayList<>());
		this.transactionRequest.setBody(commonRequestBody);
		this.transactionRequest.setHeader(header);
		this.transaction.getContext().setTransactionRequest(transactionRequest);
		this.transaction.setIsrefund(true);
		this.transaction.setCancellationtype("APPLICATION_DATE");
		mockDTO = MockDTO.getInstance();
	}

	@Test
    public void executeTestOK() {
		LOGGER.info("Execution of RBVDT01101PETransactionTest - executeTestOK *********");
		EntityOutPolicyCancellationDTO output = new EntityOutPolicyCancellationDTO();
		output.setCancellationDate(Calendar.getInstance());
        when(rbvdr011.executePolicyCancellation(anyObject())).thenReturn(output);
        this.transaction.execute();
        assertTrue(this.transaction.getAdviceList().isEmpty());
    }

	@Test
	public void testNotNull(){
		Assert.assertNotNull(this.transaction);
		this.transaction.execute();
		assertTrue(this.transaction.getIsrefund());
		assertEquals("APPLICATION_DATE", this.transaction.getCancellationtype());
	}

	@Test
	public void executeTestNull() {
		LOGGER.info("Execution of RBVDT01101PETransactionTest - executeTestNull *********");
		when(rbvdr011.executePolicyCancellation(anyObject())).thenReturn(null);
		this.transaction.execute();
		assertEquals(Severity.ENR.getValue(), this.transaction.getSeverity().getValue());
	}
}
