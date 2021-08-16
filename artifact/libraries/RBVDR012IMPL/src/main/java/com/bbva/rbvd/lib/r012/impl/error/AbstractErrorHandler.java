package com.bbva.rbvd.lib.r012.impl.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

public abstract class AbstractErrorHandler {
	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractErrorHandler.class);

    public String handler(RestClientException rce) {
        if (rce instanceof HttpClientErrorException) {
            LOGGER.info("[RepositoryASO] ErrorHandler: Calling httpClientErrorHandler");
            return this.getAdviceCode((HttpClientErrorException) rce);
        } else {
            LOGGER.info("[RepositoryASO] ErrorHandler: Calling httpServerErrorHandler");
            return this.getAdviceCode((HttpServerErrorException) rce);
        }
    }

    protected abstract String getAdviceCode(HttpClientErrorException hcee);
    protected abstract String getAdviceCode(HttpServerErrorException hsee);
}
