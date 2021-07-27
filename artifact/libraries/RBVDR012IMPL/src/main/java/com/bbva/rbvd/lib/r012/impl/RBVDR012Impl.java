package com.bbva.rbvd.lib.r012.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import com.bbva.pisd.dto.insurance.amazon.SignatureAWS;
import com.bbva.rbvd.dto.insurancecancelation.aso.policycancellation.PolicyCancellationASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NotificationsDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;
import com.bbva.rbvd.lib.r012.impl.util.JsonHelper;

public class RBVDR012Impl extends RBVDR012Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR012Impl.class);
	private static final String TAG_CONTRCTID = "contractId";

	@Override
	public PolicyCancellationPayloadBO executeCancelPolicyRimac(InputRimacBO input, PolicyCancellationPayloadBO inputPayload) {
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac START *****");
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** Params: {}, {}", input, inputPayload);

		if (input == null || input.getCodProducto() == null || input.getNumeroPoliza() == null) { 
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** invalid input");
			this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_VALIDATERULESCANCELATION.getAdviceCode());
			return null; 
		}
		if (!NumberUtils.isNumber(input.getCodProducto())) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** invalid CodProducto");
			this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_RIMAC_NONNUMBER_CODPRODUCTO.getAdviceCode());
			return null; 
		}
		if (input.getCodProducto().length() > 4) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** invalid CodProducto");
			this.addAdvice(RBVDErrors.ERROR_INVALID_INPUT_RIMAC_CHARMAX4_CODPRODUCTO.getAdviceCode());
			return null; 
		}
		
		PolicyCancellationPayloadBO output = null;
		PolicyCancellationBO bodyRequest = new PolicyCancellationBO();
		bodyRequest.setPayload(inputPayload);
		String requestJson = getRequestJson(bodyRequest);

		Map<String, String> uriParams = new HashMap<>();
		Map<String, String> queryparams = new HashMap<>();
		if (input.getCertificado() != null) {
			queryparams.put(RBVDProperties.CANCELATION_QUERYSTRING_CERTIFICATE.getValue(), input.getCertificado().toString());
		}
		String paramstr = RBVDUtils.queryParamsToString(queryparams);
		uriParams.put(RBVDProperties.CANCELATION_QUERYSTRING_PRODUCTOCOD.getValue(), input.getCodProducto());
		uriParams.put(RBVDProperties.CANCELATION_QUERYSTRING_POLICYNUMBER.getValue(), input.getNumeroPoliza().toString());
		uriParams.put(RBVDProperties.CANCELATION_QUERYSTRING_QUERYPARAMS.getValue(), StringUtils.defaultString(paramstr));
		String uri = RBVDProperties.URI_CANCELATION_CANCEL.getValue();
		uri = uri.replace("{" + RBVDProperties.CANCELATION_QUERYSTRING_PRODUCTOCOD.getValue() + "}", input.getCodProducto());
		uri = uri.replace("{" + RBVDProperties.CANCELATION_QUERYSTRING_POLICYNUMBER.getValue() + "}", input.getNumeroPoliza().toString());
		SignatureAWS signatureAWS = this.pisdR014.executeSignatureConstruction(requestJson, HttpMethod.PATCH.toString(),
				uri, paramstr, input.getTraceId());
		HttpEntity<String> entity = new HttpEntity<>(requestJson, createHttpHeadersAWS(signatureAWS));
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** awsParams: {}", paramstr);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** uriParams: {}", uriParams);
		ResponseEntity<PolicyCancellationBO> response = null;
		try {
			response = this.externalApiConnector.exchange(RBVDProperties.ID_API_CANCELATION_CANCEL_POILICY_RIMAC.getValue(),
						org.springframework.http.HttpMethod.PATCH, entity, PolicyCancellationBO.class, uriParams);
			output = response.getBody().getPayload();
		} catch(RestClientException e) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** Exception: {}", e.getMessage());
			this.addAdvice(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_RIMAC.getAdviceCode());
		}

		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac ***** Response: {}", output);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyRimac END *****");
		return output;
	}

	@Override
	public EntityOutPolicyCancellationDTO executeCancelPolicyHost(String contractId, Date cancellationDate, GenericIndicatorDTO reason,
			NotificationsDTO notifications) {
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost START *****");
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** contractId: {}", contractId);

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
		
		EntityOutPolicyCancellationDTO body = new EntityOutPolicyCancellationDTO();
		body.setCancellationDate(cancellationDate);
		body.setReason(reason);
		body.setNotifications(notifications);
		
		Map<String, String> map = new HashMap<>();
		map.put(TAG_CONTRCTID, contractId);
		
		HttpHeaders headers = createHttpMediaType();
		headers.set(RBVDConstants.BCS_OPERATION_TRACER, "ICF3");
		HttpEntity<EntityOutPolicyCancellationDTO> entity = new HttpEntity<>(body, headers);
		
		ResponseEntity<PolicyCancellationASO> response = null;
		
		try {
			response = this.internalApiConnector.exchange(RBVDProperties.ID_API_POLICY_CANCELLATION_ASO.getValue(),
					org.springframework.http.HttpMethod.POST, entity, PolicyCancellationASO.class, map);
			if (response != null && response.getBody() != null && response.getBody().getData() != null) {
				output = response.getBody().getData();
			}
		} catch(RestClientException e) {
			LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** Exception: {}", e.getMessage());
			this .addAdvice(RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode());
		}

		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost ***** Response: {}", output);
		LOGGER.info("***** RBVDR012Impl - executeCancelPolicyHost END *****");

		return output;
	}

	private HttpHeaders createHttpHeadersAWS(SignatureAWS signature) {
		HttpHeaders headers = createHttpMediaType();
		headers.set(RBVDConstants.AUTHORIZATION, signature.getAuthorization());
		headers.set("X-Amz-Date", signature.getxAmzDate());
		headers.set("x-api-key", signature.getxApiKey());
		headers.set("traceId", signature.getTraceId());
		return headers;
	}
	
	private HttpHeaders createHttpMediaType() {
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", StandardCharsets.UTF_8);
		headers.setContentType(mediaType);
		return headers;
	}

	private String getRequestJson(Object o) {
		return JsonHelper.getInstance().toJsonString(o);
	}
}
