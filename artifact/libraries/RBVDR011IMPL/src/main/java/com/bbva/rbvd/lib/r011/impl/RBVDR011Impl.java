package com.bbva.rbvd.lib.r011.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbva.rbvd.dto.insurancecancelation.aso.cypher.CypherASO;
import com.bbva.rbvd.dto.insurancecancelation.bo.ContratanteBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.InputRimacBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolicyCancellationPayloadBO;
import com.bbva.rbvd.dto.insurancecancelation.bo.PolizaBO;
import com.bbva.rbvd.dto.insurancecancelation.mock.MockDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.EntityOutPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellation;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDErrors;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDUtils;

public class RBVDR011Impl extends RBVDR011Abstract {

	private static final Logger LOGGER = LoggerFactory.getLogger(RBVDR011Impl.class);

	@Override
	public EntityOutPolicyCancellationDTO executePolicyCancellation(InputParametersPolicyCancellation input) {
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation START *****");
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation Params: {} *****", input);
		
		String xcontractNumber = this.rbvdR003.executeCypherService(new CypherASO(input.getContractId(), CypherASO.KINDAPXCYPHER_CONTRACTID));
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation xcontractNumber: {} *****", xcontractNumber);
		
		EntityOutPolicyCancellationDTO out = MockDTO.getInstance().getPolicyCancellationHostMockResponse().getData();
		/*EntityOutPolicyCancellationDTO out = this.rbvdR012.executeCancelPolicyHost(xcontractNumber
				, input.getCancellationDate()
				, input.getReason()
				, input.getNotifications());*/
		if (out == null) { return null; }
		Map<String, Object> policy = this.pisdR100.executeGetPolicyNumber(input.getContractId(), null);
		if (policy == null) { return null; }
		LOGGER.info("***** RBVDR010Impl - executeSimulateCancelation: policy = {} *****", policy);
		String statusid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()), "0");
		if (RBVDConstants.TAG_ANU.equals(statusid) || RBVDConstants.TAG_BAJ.equals(statusid)) {
			this.addAdvice(RBVDErrors.ERROR_POLICY_CANCELED.getAdviceCode());
			return null;
		}		
		String policyid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_POLICY_ID.getValue()), "0");
		String productid= java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_PRODUCT_ID.getValue()), "0");
		Double totalDebt = NumberUtils.toDouble(java.util.Objects.toString(policy.get(RBVDProperties.KEY_RESPONSE_TOTAL_DEBT_AMOUNT.getValue()), "0"));
		Double pendingAmount = NumberUtils.toDouble(java.util.Objects.toString(policy.get(RBVDProperties.KEY_REQUEST_CNCL_SETTLE_PENDING_PREMIUM_AMOUNT.getValue()), "0"));
		
		String statusId = RBVDConstants.TAG_BAJ;
		String movementType = RBVDConstants.MOV_BAJ;
		String email = "";
		
		Map<String, Object> mapContract = RBVDUtils.getMapContractNumber(input.getContractId());
		mapContract.put(RBVDProperties.KEY_REQUEST_USER_AUDIT_ID.getValue(), input.getUserId());
		Map<String, Object> arguments = new HashMap<>();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue() , input.getBranchId());
		arguments.put(RBVDProperties.KEY_REQUEST_MOVEMENT_TYPE.getValue()               , movementType);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue()         , statusId);
		if (!this.pisdR100.executeSaveContractMovement(arguments)) { return null; }
		
		if (input.getNotifications() != null && !input.getNotifications().getContactDetails().isEmpty()
				&& input.getNotifications().getContactDetails().get(0).getContact() != null) {
			email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
		}
		
		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_BRANCH.getValue(), input.getBranchId());
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_SEND_CST_EMAIL_DESC.getValue(), email);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RETURNED_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_RETURNED_CURRENCY.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_ORIGIN_CHANNEL.getValue(), input.getChannelId());
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_CUSTOMER_RETURNED_AMOUNT.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_CUSTOMER_RETURNED_CURRENCY.getValue(), null);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_TOTAL_DEBT_AMOUNT.getValue(), totalDebt);
		arguments.put(RBVDProperties.KEY_REQUEST_CNCL_SETTLE_PENDING_PREMIUM_AMOUNT.getValue(), pendingAmount);
		arguments.put(RBVDProperties.KEY_REQUEST_INSRCCONTRACT_TRANSACTION.getValue(), input.getTransactionId());
		if (!this.pisdR100.executeSaveContractCancellation(arguments)) { return null; }
		
		arguments.clear();
		arguments.putAll(mapContract);
		arguments.put(RBVDProperties.KEY_RESPONSE_CONTRACT_STATUS_ID.getValue(), statusId);
		Integer ucs = this.pisdR100.executeUpdateContractStatus(arguments);
		if (ucs == null) { return null; }
		
		InputRimacBO inputrimac = new InputRimacBO();
		inputrimac.setTraceId(input.getTraceId());
		inputrimac.setNumeroPoliza(Integer.parseInt(policyid));
		inputrimac.setCodProducto(productid);
		PolicyCancellationPayloadBO inputPayload = new PolicyCancellationPayloadBO();
		PolizaBO poliza = new PolizaBO();
		if (input.getCancellationDate() == null) {
			input.setCancellationDate(new Date());
		}
		Date date = input.getCancellationDate();  
		DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);  
		String strDate = dateFormat.format(date);  
		poliza.setFechaAnulacion(strDate);
		poliza.setCodigoMotivo("001");
		ContratanteBO contratante = new ContratanteBO();
		contratante.setCorreo(strDate);
		contratante.setEnvioElectronico("N");
		inputPayload.setPoliza(poliza);
		inputPayload.setContratante(contratante);
		PolicyCancellationPayloadBO rimacCanellation = this.rbvdR012.executeCancelPolicyRimac(inputrimac, inputPayload);
		if (rimacCanellation == null) { return null; }
		
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation ***** Response: {}", out);
		LOGGER.info("***** RBVDR011Impl - executePolicyCancellation END *****");
		return out;
	}

}
