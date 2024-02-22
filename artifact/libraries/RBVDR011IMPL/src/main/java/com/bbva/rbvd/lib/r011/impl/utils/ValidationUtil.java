package com.bbva.rbvd.lib.r011.impl.utils;

import com.bbva.rbvd.dto.insurancecancelation.commons.ContactDetailDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;


import static java.util.Objects.nonNull;

public class ValidationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtil.class);
    private static final String AMERICA_LIMA = "America/Lima";

    private ValidationUtil(){}

    public static  boolean validateInsurerRefundInternal(InputParametersPolicyCancellationDTO input){
        return input.getInsurerRefund() != null && input.getInsurerRefund().getPaymentMethod() != null
                && input.getInsurerRefund().getPaymentMethod().getContract().getContractType().equals(RBVDProperties.CONTRACT_TYPE_INTERNAL_ID.getValue())
                && input.getInsurerRefund().getPaymentMethod().getContract().getId() != null;
    }

    public static boolean validateInsurerRefundExternal(InputParametersPolicyCancellationDTO input){
        return input.getInsurerRefund() != null && input.getInsurerRefund().getPaymentMethod() != null
                && input.getInsurerRefund().getPaymentMethod().getContract().getContractType().equals(RBVDProperties.CONTRACT_TYPE_EXTERNAL_ID.getValue())
                && input.getInsurerRefund().getPaymentMethod().getContract().getNumber() != null;
    }

    public static boolean validateEmailContact(ContactDetailDTO contactDetailDTO){
        return nonNull(contactDetailDTO.getContact()) &&
                RBVDProperties.CONTACT_EMAIL_ID.getValue().equals(contactDetailDTO.getContact().getContactDetailType());
    }

    public static boolean isActiveStatusId(Object statusId) {
        return statusId != null && !RBVDConstants.TAG_ANU.equals(statusId.toString()) && !RBVDConstants.TAG_BAJ.equals(statusId.toString());
    }

    public static Boolean validateStartDate(String startDatePolicy){
        if(StringUtils.isNotEmpty(startDatePolicy)){
            String formattedDate = startDatePolicy.split(" ")[0];

            LOGGER.info("***** ValidationUtil - validateStartDate ***** : formattedDate{}", formattedDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LOGGER.info("***** ValidationUtil - validateStartDate ***** : strDate{}", startDatePolicy);

            java.time.LocalDate startDateFormatted = java.time.LocalDate.parse(formattedDate, formatter);
            ZonedDateTime zoneStartDate = startDateFormatted.atStartOfDay(ZoneId.of(AMERICA_LIMA));

            LOGGER.info("***** ValidationUtil - validateStartDate *****  startDateFormatted: {}", startDateFormatted);
            java.time.LocalDate todayFormatted = java.time.LocalDate.now();
            ZonedDateTime zonedToday = todayFormatted.atStartOfDay(ZoneId.of(AMERICA_LIMA));
            LOGGER.info("***** ValidationUtil - validateStartDate *****  zonedToday: {}", zonedToday);

            return zoneStartDate.isAfter(zonedToday) || zoneStartDate.isEqual(zonedToday);
        }
        return false;
    }

    public static boolean isStartDateTodayOrAfterToday(boolean isRoyal, Map<String, Object> policy) {
        if(isRoyal){
            String startDatePolicy= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue()),null);
            return validateStartDate(startDatePolicy);
        }else{
            return true;
        }
    }

    public static boolean isOpenCancellationRequest(Map<String, Object> requestCancellationMovLast) {
        if (requestCancellationMovLast != null) {
            String statusId = requestCancellationMovLast.get(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue()).toString();
            LOGGER.info("***** CancellationRequestImpl - isOpenCancellationRequest - requestCancellationMovLast -> statusId: {}", statusId);
            return RBVDConstants.MOV_PEN.equals(statusId);
        }
        return false;
    }

    public static boolean isRetainedOrNotExistsCancellationRequest(Map<String, Object> requestCancellationMovLast) {
        if (requestCancellationMovLast != null) {
            String statusId = requestCancellationMovLast.get(RBVDProperties.FIELD_CONTRACT_STATUS_ID.getValue()).toString();
            LOGGER.info("***** CancellationRequestImpl - isRetainedCancellationRequest - requestCancellationMovLast -> statusId: {}", statusId);
            return RBVDConstants.MOV_RET.equals(statusId);
        }
        return true;
    }

    public static String obtainInsurerRefundAccountOrCard(InputParametersPolicyCancellationDTO input){
        if(validateInsurerRefundInternal(input)) return input.getInsurerRefund().getPaymentMethod().getContract().getId();
        else if(validateInsurerRefundExternal(input)) return input.getInsurerRefund().getPaymentMethod().getContract().getNumber();
        else return null;
    }

    public static boolean validateMassiveProduct(Map<String, Object> policy, String massiveProductsParameter){
        String[] massiveProductsList = massiveProductsParameter.split(",");
        String massiveProduct = Arrays.stream(massiveProductsList).filter(product ->
                product.equals(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()))).findFirst().orElse(null);
        return massiveProduct != null;
    }

    public static boolean validateDaysOfRightToRepent(Map<String, Object> policy){
        String startDatePolicy= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_START_DATE_FORMATTED.getValue()),null);
        return validateRightToRepent(startDatePolicy);
    }

    public static Boolean validateRightToRepent(String startDatePolicy){
        if(StringUtils.isNotEmpty(startDatePolicy)){
            String formattedDate = startDatePolicy.split(" ")[0];

            LOGGER.info("***** ValidationUtil - validateRightToRepent ***** : formattedDate{}", formattedDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LOGGER.info("***** ValidationUtil - validateRightToRepent ***** : strDate{}", startDatePolicy);

            java.time.LocalDate startDateFormatted = java.time.LocalDate.parse(formattedDate, formatter);
            ZonedDateTime zoneStartDate = startDateFormatted.atStartOfDay(ZoneId.of(AMERICA_LIMA));

            LOGGER.info("***** ValidationUtil - validateRightToRepent *****  startDateFormatted: {}", startDateFormatted);
            java.time.LocalDate todayFormatted = java.time.LocalDate.now();
            ZonedDateTime zonedToday = todayFormatted.atStartOfDay(ZoneId.of(AMERICA_LIMA));
            LOGGER.info("***** ValidationUtil - validateRightToRepent *****  zonedToday: {}", zonedToday);

            return zonedToday.minusDays(RBVDConstants.DAYS_OF_RIGHT_TO_REPENT).isEqual(zoneStartDate) ||
                    zoneStartDate.isAfter(zonedToday.minusDays(RBVDConstants.DAYS_OF_RIGHT_TO_REPENT));
        }
        return false;
    }

}
