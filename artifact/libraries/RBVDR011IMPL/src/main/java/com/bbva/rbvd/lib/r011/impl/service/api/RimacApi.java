package com.bbva.rbvd.lib.r011.impl.service.api;

import com.bbva.elara.configuration.manager.application.ApplicationConfigurationService;
import com.bbva.rbvd.dto.cicsconnection.icf2.ICF2Response;
import com.bbva.rbvd.dto.insurancecancelation.bo.*;
import com.bbva.rbvd.dto.insurancecancelation.bo.rescue.RescueInversionBO;
import com.bbva.rbvd.dto.insurancecancelation.commons.AutorizadorDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.DuesPaidDTO;
import com.bbva.rbvd.dto.insurancecancelation.commons.NacarDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.ContractCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.policycancellation.InputParametersPolicyCancellationDTO;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDConstants;
import com.bbva.rbvd.dto.insurancecancelation.utils.RBVDProperties;
import com.bbva.rbvd.lib.r011.impl.transform.bean.RequestRimacBean;
import com.bbva.rbvd.lib.r011.impl.utils.ConstantsUtil;
import com.bbva.rbvd.lib.r311.RBVDR311;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RimacApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(RimacApi.class);

    private final RBVDR311 rbvdR311;
    private final ApplicationConfigurationService applicationConfigurationService;

    public RimacApi(RBVDR311 rbvdR311, ApplicationConfigurationService applicationConfigurationService) {
        this.rbvdR311 = rbvdR311;
        this.applicationConfigurationService = applicationConfigurationService;
    }

    public CancelationSimulationPayloadBO getCancellationSimulationResponse(InputParametersPolicyCancellationDTO input, String policyId, String productCodeForRimac){
        LOGGER.info("RBVDR011Impl - executePolicyCancellation() - START");

        CancelationSimulationPayloadBO cancellationSimulationResponse = null;

        InputRimacBO rimacSimulationRequest = RequestRimacBean.buildRimacSimulationRequest(input, policyId, productCodeForRimac);
        LOGGER.info("RBVDR011Impl - getCancellationSimulationResponse() - rimacSimulationRequest: {}", rimacSimulationRequest);

        cancellationSimulationResponse = rbvdR311.executeSimulateCancelationRimac(rimacSimulationRequest);
        LOGGER.info("RBVDR011Impl - getCancellationSimulationResponse() - cancellationSimulationResponse: {}", cancellationSimulationResponse);

        if(cancellationSimulationResponse != null) {
            cancellationSimulationResponse.setMoneda(conversor(cancellationSimulationResponse.getMoneda()));
        }

        LOGGER.info("RBVDR011Impl - executePolicyCancellation() - END - cancellationSimulationResponse: {}", cancellationSimulationResponse);

        return cancellationSimulationResponse;
    }

    public PolicyCancellationPayloadBO executeRimacCancellation(InputParametersPolicyCancellationDTO input, String policyId, String productCode,
                                                                 String isChannelEndoso, String userCode, Map<String, Object> cancellationRequest, String email,
                                                                boolean isRoyal, ICF2Response icf2Response){
        InputRimacBO inputrimac = new InputRimacBO();
        inputrimac.setTraceId(input.getTraceId());
        inputrimac.setNumeroPoliza(Integer.parseInt(policyId));
        inputrimac.setCodProducto(productCode);
        PolicyCancellationPayloadBO inputPayload = new PolicyCancellationPayloadBO();


        if(!Strings.isNullOrEmpty(isChannelEndoso)){
            LOGGER.info("***** RBVDR011Impl - CANAL: {} ACCEPTED  *****", isChannelEndoso);
            AutorizadorDTO autorizadorDTO = new AutorizadorDTO();
            autorizadorDTO.setFlagAutorizador("S");
            autorizadorDTO.setAutorizador(userCode);
            inputPayload.setAutorizador(autorizadorDTO);
            LOGGER.info("***** RBVDR011Impl - isChannelEndoso END  *****");
        }

        Timestamp dateTimestamp = (Timestamp)cancellationRequest.get(RBVDProperties.FIELD_REQUEST_CNCL_POLICY_DATE.getValue());
        Date date = new Date(dateTimestamp.getTime());
        DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);
        String strDate = dateFormat.format(date);

        // Póliza del seguro
        PolizaBO poliza = new PolizaBO();
        poliza.setFechaAnulacion(strDate);
        poliza.setCodigoMotivo(input.getReason().getId());

        // Contratante del seguro
        ContratanteBO contratante = new ContratanteBO();
        contratante.setCorreo(email);
        contratante.setEnvioElectronico("S");

        if(!isRoyal){
            AutorizadorDTO autorizadorDTO = new AutorizadorDTO();
            autorizadorDTO.setFlagAutorizador("S");
            autorizadorDTO.setAutorizador(userCode);

            DuesPaidDTO duesPaidDTO1 = new DuesPaidDTO();
            duesPaidDTO1.setMontoPagado(icf2Response.getIcmf1S2().getMOPRIR1());
            duesPaidDTO1.setFechaInicio(icf2Response.getIcmf1S2().getFINREC1());
            duesPaidDTO1.setFechaFin(icf2Response.getIcmf1S2().getFFIREC1());

            DuesPaidDTO duesPaidDTO2 = new DuesPaidDTO();
            duesPaidDTO2.setMontoPagado(icf2Response.getIcmf1S2().getMOPRIR2());
            duesPaidDTO2.setFechaInicio(icf2Response.getIcmf1S2().getFINREC2());
            duesPaidDTO2.setFechaFin(icf2Response.getIcmf1S2().getFFIREC2());

            List<DuesPaidDTO> cuotasPagadas = new ArrayList<>();
            cuotasPagadas.add(duesPaidDTO1);
            cuotasPagadas.add(duesPaidDTO2);

            NacarDTO nacarDTO = new NacarDTO();
            nacarDTO.setMontoSimulado(icf2Response.getIcmf1S2().getIMDECIA());
            nacarDTO.setExtornoComision(icf2Response.getIcmf1S2().getIMPCOMI());
            nacarDTO.setFechaInicioVigencia(icf2Response.getIcmf1S2().getFINICON());
            nacarDTO.setFechaFinVigencia(icf2Response.getIcmf1S2().getFFINCON());
            nacarDTO.setCuotasPagadas(cuotasPagadas);

            inputPayload.setAutorizador(autorizadorDTO);
            inputPayload.setNacar(nacarDTO);
        }

        inputPayload.setPoliza(poliza);
        inputPayload.setContratante(contratante);

        return rbvdR311.executeCancelPolicyRimac(inputrimac, inputPayload);
    }

    public void executeRescueCancellationRimac(InputParametersPolicyCancellationDTO input, String policyId ,String productCode){
        InputRimacBO inputRimac = new InputRimacBO();
        inputRimac.setTraceId(input.getTraceId());
        inputRimac.setNumeroPoliza(Integer.parseInt(policyId));
        inputRimac.setCodProducto(productCode);
        Date date = input.getCancellationDate().getTime();
        DateFormat dateFormat = new SimpleDateFormat(RBVDConstants.DATEFORMAT_YYYYMMDD);
        String strDate = dateFormat.format(date);
        RescueInversionBO polizaRescue = new RescueInversionBO();
        CancelationRescuePayloadBO cancelPayload = new CancelationRescuePayloadBO();
        ContratanteBO contratante = new ContratanteBO();
        BankAccountBO cuentaBancaria = new BankAccountBO();

        polizaRescue.setFechaSolicitud(strDate);
        String email = "";
        if (input.getNotifications() != null
                && !input.getNotifications().getContactDetails().isEmpty()
                && input.getNotifications().getContactDetails().get(0).getContact() != null) {
            email = input.getNotifications().getContactDetails().get(0).getContact().getAddress();
        }
        contratante.setCorreo(email);
        contratante.setEnvioElectronico("S");
        ContractCancellationDTO contractReturn = input.getInsurerRefund().getPaymentMethod().getContract();
        cuentaBancaria.setTipoCuenta("A");
        if(contractReturn.getId().startsWith("01", 10)){
            cuentaBancaria.setTipoCuenta("C");
        }
        cuentaBancaria.setNumeroCuenta(contractReturn.getId());
        cuentaBancaria.setTipoMoneda(this.applicationConfigurationService.getProperty("cancellation.rescue.currency"));
        cuentaBancaria.setRazonSocialBanco(this.applicationConfigurationService.getProperty("cancellation.rescue.bank.name"));
        contratante.setCuentaBancaria(cuentaBancaria);
        cancelPayload.setPoliza(polizaRescue);
        cancelPayload.setContratante(contratante);

        rbvdR311.executeRescueCancelationRimac(inputRimac, cancelPayload);
    }

    public String conversor(String currency) {
        if(currency.equalsIgnoreCase(RBVDConstants.CURRENCY_SOL)) {
            currency = ConstantsUtil.CURRENCY_PEN;
        }
        return currency;
    }
}
