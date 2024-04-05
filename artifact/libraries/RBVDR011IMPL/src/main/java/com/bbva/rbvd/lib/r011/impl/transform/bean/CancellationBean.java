package com.bbva.rbvd.lib.r011.impl.transform.bean;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.pisd.lib.r401.PISDR401;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericIndicatorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.GenericStatusDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CancellationBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancellationBean.class);

    private final PISDR401 pisdR401;
    private final ApplicationConfigurationService applicationConfigurationService;

    public CancellationBean(PISDR401 pisdR401, ApplicationConfigurationService applicationConfigurationService) {
        this.pisdR401 = pisdR401;
        this.applicationConfigurationService = applicationConfigurationService;
    }

    public EntityOutPolicyCancellationDTO mapRetentionResponse(String policyId, InputParametersPolicyCancellationDTO input, String statusId, String statusDescription, Calendar cancellationDate) {
        LOGGER.info("***** RBVDR011Impl - mapRetentionResponse START *****");

        EntityOutPolicyCancellationDTO entityOutPolicyCancellationDTO = new EntityOutPolicyCancellationDTO();
        entityOutPolicyCancellationDTO.setId(policyId);
        entityOutPolicyCancellationDTO.setCancellationDate(cancellationDate);
        entityOutPolicyCancellationDTO.setReason(new GenericIndicatorDTO());
        entityOutPolicyCancellationDTO.getReason().setId(input.getReason().getId());
        entityOutPolicyCancellationDTO.setStatus(new GenericStatusDTO());
        entityOutPolicyCancellationDTO.getStatus().setId(statusId);
        entityOutPolicyCancellationDTO.getStatus().setDescription(statusDescription);

        LOGGER.info("***** RBVDR011Impl - mapRetentionResponse END *****");
        return entityOutPolicyCancellationDTO;
    }

    public Map<String, Object> getPolicyInsuranceData(boolean isRoyal, Map<String, Object> policy, ICF2Response icf2Response){
        String policyId;
        String insuranceProductId;
        String productCode;
        String productCompanyId;

        if(isRoyal){
            policyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
            insuranceProductId= Objects.toString(policy.get(RBVDProperties.KEY_INSURANCE_PRODUCT_ID.getValue()), "0");
            productCompanyId= Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
            productCode = getProductCode(insuranceProductId, productCompanyId);
        } else{
            insuranceProductId = icf2Response.getIcmf1S2().getCODPROD();
            policyId = "";
            productCode = "";
        }

        Map<String, Object> policyMap = new HashMap<>();
        policyMap.put(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue(), policyId);
        policyMap.put(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue(), insuranceProductId);
        policyMap.put(RBVDConstants.PRODUCT_CODE_FOR_RIMAC, productCode);
        return policyMap;
    }

    public String getProductCode(String insuranceProductId, String companyProductCode){
        Map<String, Object> product = getProductByProductId(insuranceProductId);
        LOGGER.info("***** RBVDR011Impl - executeSimulateCancelation: product = {} *****", product);
        String businessName= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_INSURANCE_BUSINESS_NAME), "");
        String shortDesc= java.util.Objects.toString(product.get(ConstantsUtil.FIELD_PRODUCT_SHORT_DESC), "");
        if(isLifeProduct(businessName)) return shortDesc;
        else return companyProductCode;
    }

    public Map<String, Object> getProductByProductId(String productId) {
        Map<String,Object> arguments = new HashMap<>();
        arguments.put(ConstantsUtil.FIELD_INSURANCE_PRODUCT_ID, productId);
        return (Map<String,Object>) this.pisdR401.executeGetProductById(ConstantsUtil.QUERY_GET_PRODUCT_BY_PRODUCT_ID, arguments);
    }

    public boolean isLifeProduct(String businessName){
        return Objects.nonNull(businessName) && (
                businessName.equals(ConstantsUtil.BUSINESS_NAME_VIDA) || businessName.equals(ConstantsUtil.BUSINESS_NAME_EASYYES));
    }

    public String getEmailFromInput(InputParametersPolicyCancellationDTO input, EntityOutPolicyCancellationDTO policyCancellationDTO, ICF2Response  icf2Response){
        String email;

        if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().get(0).getContact() != null
                && !StringUtils.isEmpty(input.getNotifications().getContactDetails().get(0).getContact().getAddress())) {
            email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
        } else if(policyCancellationDTO != null && policyCancellationDTO.getNotifications() != null) {
            email = policyCancellationDTO.getNotifications().getContactDetails().get(0).getContact().getAddress();
        } else if (icf2Response != null && icf2Response.getIcmf1S2() != null && icf2Response.getIcmf1S2().getTIPCONT() != null && icf2Response.getIcmf1S2().getTIPCONT().equals(RBVDConstants.EMAIL_CONTACT_TYPE_ICF3) && icf2Response.getIcmf1S2().getDESCONT() != null) {
            email = icf2Response.getIcmf1S2().getDESCONT();
        } else {
            email = this.applicationConfigurationService.getProperty("default.cancellation.email");
        }

        return email;
    }

}
