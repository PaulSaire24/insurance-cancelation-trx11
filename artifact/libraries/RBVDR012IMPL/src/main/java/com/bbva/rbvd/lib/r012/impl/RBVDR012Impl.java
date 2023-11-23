package com.bbva.rbvd.lib.r012.impl;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.bbva.rbvd.dto.insurancecancelation.aso.policycancellation.PolicyCancellationASO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r012.impl.util.MockService;

public class

RBVDR012Impl extends RBVDR012Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR012Impl.class);
	private static final String TAG_CONTRCTID = "contractId";

	@Override
	public EntityOutPolicyCancellationDTO executeCancelPolicyHost(String contractId, Calendar cancellationDate, GenericIndicatorDTO reason,
			NotificationsDTO notifications) {
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost START *****");
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** contractId: {}", contractId);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** cancellationDate: {}", cancellationDate);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** reason: {}", reason);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** notifications: {}", notifications);

		EntityOutPolicyCancellationDTO output = null;
		if (contractId == null || contractId.length() < 20) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** invalid input contractId");
			this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_CONTRACT_VALIDATERULESCANCELATION_HOST.getAdviceCode());
			return null; 
		}
		
		if (reason == null || reason.getId() == null) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** invalid input reason to cancelate a policy");
			this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_REASON_TO_CANCELATE_HOST.getAdviceCode());
			return null; 
		}
		
		if(this.mockService.isEnabled(MockService.MOCKER_ASOCANCELLATION)) {
			LOGGER.info("***** RBVDR012Impl - MockService executeCancelPolicyHost Invokation *****");
			EntityOutPolicyCancellationDTO mockresp = this.mockService.getAsoCancellationMock();
			LOGGER.info("***** RBVDR012Impl - MockService executeCancelPolicyHost Invokation ***** mockresp: {} ", mockresp);
			return mockresp;
		}
		
		EntityOutPolicyCancellationDTO body = new EntityOutPolicyCancellationDTO();
		body.setReason(reason);
		body.setNotifications(notifications);
		

		JSONObject requestJson = new JSONObject(body);
		if (cancellationDate != null) {
			Date date = cancellationDate.getTime();  
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");  
			String strDate = dateFormat.format(date);  
			requestJson.put("cancellationDate", strDate);
		}
		
		Map<String, String> map = new HashMap<>();
		map.put(TAG_CONTRCTID, contractId);
		
		HttpHeaders headers = createHttpMediaType();
		headers.set(RBVDConstants.BCS_OPERATION_TRACER, "ICF3");
		HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);
		LOGGER.info("***** RBVDR012Impl - MockService executeCancelPolicyHost ***** body: {} ", entity.getBody());
		
		ResponseEntity<PolicyCancellationASO> response = null;
		
		try {
			response = this.internalApiConnector.exchange(RBVDProperties.ID_API_POLICY_CANCELLATION_ASO.getValue(),
					org.springframework.http.HttpMethod.POST, entity, PolicyCancellationASO.class, map);
			if (response != null && response.getBody() != null && response.getBody().getData() != null) {
				output = response.getBody().getData();
			}
		} catch(RestClientException e) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** Exception: {}", e.getMessage());
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** Exception body: {}", ((HttpStatusCodeException) e).getResponseBodyAsString());
			this.addAdvice(this.policyCancellationAsoErrorHandler.handler(e));
		}

		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** Response: {}", output);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost END *****");

		return output;
	}

	
	private HttpHeaders createHttpMediaType() {
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);
		headers.setContentType(mediaType);
		return headers;
	}

}
