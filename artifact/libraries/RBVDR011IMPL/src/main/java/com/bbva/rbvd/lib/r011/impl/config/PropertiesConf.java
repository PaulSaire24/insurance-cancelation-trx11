package com.bbva.rbvd.lib.r011.impl.config;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PropertiesConf {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesConf.class);

    private ApplicationConfigurationService applicationConfigurationService;

    public PropertiesConf(ApplicationConfigurationService applicationConfigurationService) {
        this.applicationConfigurationService = applicationConfigurationService;
    }

    public Map<Object, String> emailProperties() {
        LOGGER.info("***** PropertiesConf emailProperties START *****");

        Map<Object, String> propertiesEmail = new HashMap<>();

        propertiesEmail.put("notificationTypeRequestCancellationId", this.applicationConfigurationService.getProperty("notification.config.notificationTypeRequestCancellation"));
        propertiesEmail.put("notificationTypeCancellationInmediateId", this.applicationConfigurationService.getProperty("notification.config.notificationTypeCancellationInmediate"));
        propertiesEmail.put("notificationTypeCancellationEndOfValidityId", this.applicationConfigurationService.getProperty("notification.config.notificationTypeCancellationEndOfValidity"));

        LOGGER.info("***** PropertiesConf emailProperties END *****");

        return propertiesEmail;
    }
 }
