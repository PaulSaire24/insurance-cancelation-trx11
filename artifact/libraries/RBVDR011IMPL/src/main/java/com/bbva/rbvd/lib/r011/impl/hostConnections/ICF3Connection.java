package com.bbva.rbvd.lib.r011.impl.hostConnections;

import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Request;
import com.bbva.rbvd.dto.cicsconnection.icf3.ICF3Response;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericAmountDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.ExchangeRateDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InsurerRefundCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r051.RBVDR051;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static com.bbva.rbvd.lib.r011.impl.utils.DateUtil.convertStringToDate;
import static com.bbva.rbvd.lib.r011.impl.utils.DateUtil.getCancellationDate;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.validateEmailContact;
import static com.bbva.rbvd.lib.r011.impl.utils.ValidationUtil.obtainInsurerRefundAccountOrCard;
import static java.util.Objects.nonNull;

public class ICF3Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ICF3Connection.class);
    protected RBVDR051 rbvdR051;

    public EntityOutPolicyCancellationDTO executeICF3Transaction(InputParametersPolicyCancellationDTO input,
                                                                        Map<String, Object> cancellationRequest, Map<String, Object> policy,
                                                                        ICF2Response icf2Response) {
        LOGGER.info("***** RBVDR011Impl - executeICF3Transaction - Start");
        ICF3Request icf3DTORequest = buildICF3Request(input, cancellationRequest, policy, icf2Response);
        LOGGER.info("***** RBVDR011Impl - executeICF3Transaction - ICF3Request: {}", icf3DTORequest);
        ICF3Response icf3Response = rbvdR051.executePolicyCancellation(icf3DTORequest);
        LOGGER.info("***** RBVDR011Impl - executeICF3Transaction - ICF3Response: {}", icf3Response);

        if (icf3Response.getHostAdviceCode() != null) {
            LOGGER.info("***** RBVDR011Impl - executeICF3Transaction - Error at icf3 execution - Host advice code: {}", icf3Response.getHostAdviceCode());
            return null;
        }
        LOGGER.info("***** RBVDR011Impl - executeICF3Transaction - End");
        return mapICF3Response(input, icf3Response, cancellationRequest);
    }

    public ICF3Request buildICF3Request(InputParametersPolicyCancellationDTO input, Map<String, Object> cancellationRequest, Map<String, Object> policy, ICF2Response icf2Response){
        ICF3Request icf3DTORequest = new ICF3Request();
        icf3DTORequest.setNUMCER(input.getContractId());
        Date date = getCancellationDate(cancellationRequest, input);
        DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_CNCL_DATE);
        icf3DTORequest.setFECCANC(dateFormat.format(date));
        icf3DTORequest.setCODMOCA(input.getReason().getId());
        LOGGER.info("***** RBVDR011Impl - ICF3Connection - buildICF3Request: {}", icf3DTORequest.getFECCANC());

        if (nonNull(input.getNotifications())){
            input.getNotifications().getContactDetails().stream().forEach(contactDetailDTO -> {
                if (validateEmailContact(contactDetailDTO)) {
                    icf3DTORequest.setTIPCONT(RBVDConstants.EMAIL_CONTACT_TYPE_ICF3);
                    icf3DTORequest.setDESCONT(contactDetailDTO.getContact().getAddress());
                }
            });
        }

        if(cancellationRequest != null && cancellationRequest.get(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue()) != null){
            icf3DTORequest.setCOMRIMA(((BigDecimal)cancellationRequest.get(RBVDProperties.FIELD_INSRC_COMPANY_RETURNED_AMOUNT.getValue())).doubleValue());
        }
        if(cancellationRequest != null && cancellationRequest.get(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue()) != null){
            icf3DTORequest.setMONTDEV(((BigDecimal)cancellationRequest.get(RBVDProperties.FIELD_PREMIUM_AMOUNT.getValue())).doubleValue());
        }

        icf3DTORequest.setNUMPOL("");
        if(policy != null && policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()) != null){
            icf3DTORequest.setNUMPOL(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue())));
        }

        if(policy != null && policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()) != null){
            icf3DTORequest.setPRODRI(String.valueOf(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue())));
        }else{
            icf3DTORequest.setPRODRI(icf2Response.getIcmf1S2().getCODPROD());
        }

        icf3DTORequest.setINDDEV(RBVDConstants.TAG_S);
        icf3DTORequest.setCTAADEV(obtainInsurerRefundAccountOrCard(input));

        return icf3DTORequest;
    }

    public EntityOutPolicyCancellationDTO mapICF3Response(InputParametersPolicyCancellationDTO input, ICF3Response icf3Response, Map<String, Object> cancellationRequest){
        EntityOutPolicyCancellationDTO output = new EntityOutPolicyCancellationDTO();
        output.setId(icf3Response.getIcmf3s0().getIDCANCE());
        GenericStatusDTO status = new GenericStatusDTO();
        status.setId(icf3Response.getIcmf3s0().getDESSTCA());
        status.setDescription(icf3Response.getIcmf3s0().getDESSTCA());
        output.setStatus(status);
        Calendar calendarTime = Calendar.getInstance();
        Date date = getCancellationDate(cancellationRequest, input);
        calendarTime.setTime(date);
        output.setCancellationDate(calendarTime);
        GenericIndicatorDTO reason = new GenericIndicatorDTO();
        reason.setId(icf3Response.getIcmf3s0().getCODMOCA());
        reason.setDescription(icf3Response.getIcmf3s0().getDESMOCA());
        output.setReason(reason);
        output.setNotifications(input.getNotifications());
        InsurerRefundCancellationDTO insurerRefund = new InsurerRefundCancellationDTO();
        insurerRefund.setAmount(icf3Response.getIcmf3s0().getIMDECIA());
        insurerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVDCIA());
        output.setInsurerRefund(insurerRefund);
        GenericAmountDTO customerRefund = new GenericAmountDTO();
        customerRefund.setAmount(icf3Response.getIcmf3s0().getIMPCLIE());
        customerRefund.setCurrency(icf3Response.getIcmf3s0().getDIVIMC());
        output.setCustomerRefund(customerRefund);
        ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
        exchangeRateDTO.setTargetCurrency(icf3Response.getIcmf3s0().getDIVDEST());
        if(!icf3Response.getIcmf3s0().getFETIPCA().isEmpty()){
            exchangeRateDTO.setCalculationDate(convertStringToDate(icf3Response.getIcmf3s0().getFETIPCA()));
        }else{
            exchangeRateDTO.setCalculationDate(new Date());
        }
        exchangeRateDTO.setValue(icf3Response.getIcmf3s0().getTIPCAMB());
        exchangeRateDTO.setBaseCurrency(icf3Response.getIcmf3s0().getDIVORIG());
        output.setExchangeRate(exchangeRateDTO);
        return output;
    }

    public void setRbvdR051(RBVDR051 rbvdR051){this.rbvdR051 = rbvdR051;}
}