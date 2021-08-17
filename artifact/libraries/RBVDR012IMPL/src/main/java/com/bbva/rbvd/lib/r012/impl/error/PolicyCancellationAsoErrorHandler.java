package com.bbva.rbvd.lib.r012.impl.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;

public class PolicyCancellationAsoErrorHandler extends AbstractErrorHandler {
	protected static final Logger LOGGER = LoggerFactory.getLogger(PolicyCancellationAsoErrorHandler.class);
	public static final String ICE9041 = "ICE9041";//NUMERO DE OPERACION NO EXISTE O NO ESTÁ FORMALIZADO
	public static final String ICE9230 = "ICE9230";//LA POLIZA YA FUE CANCELADA ANTERIORMENTE
	public static final String ICER163 = "ICER163";//CAMPO FECHA DE CANCELACIÓN DEL CLIENTE ES MAYOR Al FIN DE COBERTURA
	public static final String ICE9114 = "ICE9114";//PRODUCTO NO OPERATIVO PARA ESTA TRANSACCION
	public static final String ICE9212 = "ICE9212";//LA POLIZA NO SE PUEDE ANULAR

	@Override
	protected String getAdviceCode(HttpClientErrorException hcee) {
		LOGGER.debug("[PolicyCancellationAsoErrorHandler] httpClientErrorHandler: [Error = {}]", hcee.getResponseBodyAsString());
		String errorCode = getASOErrorCode(hcee.getResponseBodyAsString());
		switch (errorCode) {
		case ICE9041:
			return RBVDErrors.ERROR_POLICY_NOT_EXIST.getAdviceCode();
		case ICE9230:
			return RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode();
		case ICER163:
			return RBVDErrors.ERROR_CANCELLATIONDATE_EXCEEDS_COVERAGE.getAdviceCode();
		case ICE9114:
			return RBVDErrors.ERROR_UNCANCELLABLE_PRODUCT.getAdviceCode();
		case ICE9212:
			return RBVDErrors.ERROR_POLICY_CANCELLATION_UNABLE.getAdviceCode();
		default:
			return RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode();
		}
	}

	@Override
	protected String getAdviceCode(HttpServerErrorException hsee) {
		LOGGER.debug("[PolicyCancellationAsoErrorHandler] HttpServerErrorException: [Error = {}]", hsee.getResponseBodyAsString());
		return RBVDErrors.ERROR_TO_CONNECT_SERVICE_POLICYCANCELLATION_ASO.getAdviceCode();
	}
	
	private String getASOErrorCode(String body) {
		String separator = "#";
		int sepPos = body.lastIndexOf(separator);
		Integer init = Math.max(0, sepPos-7);
		Integer fin = Math.max(0, sepPos);
		return body.substring(init,fin);
	}
}
