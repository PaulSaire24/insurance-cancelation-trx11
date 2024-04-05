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

        propertiesEmail.put("notificationTypeRequestCancellationId", this.applicationConfigurationService.getProperty("notification.config.email.notificationTypeRequestCancellation"));
        propertiesEmail.put("descriptionEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.description.email"));
        propertiesEmail.put("addDescriptionEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.addDescription.email"));
        propertiesEmail.put("titleEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.title.email"));
        propertiesEmail.put("applicationDateEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.applicationDate.email"));
        propertiesEmail.put("applicationNumberEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.applicationNumber.email"));
        propertiesEmail.put("certificateNumberEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.certificateNumber.email"));
        propertiesEmail.put("planTypeEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.planType.email"));
        propertiesEmail.put("adviceEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.advice.email"));
        propertiesEmail.put("additionalInformationEmail", this.applicationConfigurationService.getProperty("notificationTypeRequestCancellation.additionalInformation.email"));

        LOGGER.info("***** PropertiesConf emailProperties END *****");

        return propertiesEmail;
    }
 }
