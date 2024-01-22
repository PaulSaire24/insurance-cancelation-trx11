package com.bbva.rbvd.lib.r011.impl.utils;

import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

public class DateUtil {
    private DateUtil(){}

    public static Date convertStringToDate(String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDate localDate = java.time.LocalDate.parse(date, formatter);
        return Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
    }

    public static Date getCancellationDate(Map<String, Object> cancellationRequest, InputParametersPolicyCancellationDTO input){
        Date date;
        if(cancellationRequest!=null){
            Timestamp dateTimestamp = (Timestamp)cancellationRequest.get(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue());
            date = new Date(dateTimestamp.getTime());
        }else{
            date = input.getCancellationDate().getTime();
        }
        return date;
    }
}
