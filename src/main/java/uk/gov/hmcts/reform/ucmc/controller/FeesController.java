package uk.gov.hmcts.reform.ucmc.controller;

import feign.FeignException;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.ucmc.service.FeeService;
import uk.gov.hmcts.reform.ucmc.service.PaymentService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Api
@Slf4j
@RestController
@RequestMapping("/callback/fees")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FeesController {

    private final FeeService feeService;
    private final PaymentService paymentService;

    @PostMapping("/mid-event")
    public AboutToStartOrSubmitCallbackResponse handleMidEvent(@RequestBody CallbackRequest callbackRequest) {
        var caseData = callbackRequest.getCaseDetails().getData();

        List<String> errors = new ArrayList<>();
        try {
            //TODO: mapping to CaseData model
            var claimValue = new BigDecimal(caseData.get("claimValue").toString());
            caseData.put("feeAmount", feeService.getFeeAmountByClaimValue(claimValue));
        } catch (FeignException e) {
            //TODO: proper error scenario handling
            log.error("There was a problem with finding fee value: " + e.contentUTF8());
            errors.add("Fee amount could not be retrieved");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseData)
            .build();
    }

    @PostMapping("/about-to-submit")
    public AboutToStartOrSubmitCallbackResponse handleAboutToSubmit(@RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();

        List<String> errors = new ArrayList<>();
        try {
            PaymentDto paymentResponse = paymentService.createCreditAccountPayment(caseDetails);
            log.info("Payment made successfully: " + paymentResponse);
            caseDetails.getData().remove("feeAmount");
        } catch (FeignException e) {
            //TODO: proper error scenario handling
            log.error("There was a problem with making payment: " + e.contentUTF8());
            errors.add("Payment could not be processed");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .data(caseDetails.getData())
            .build();
    }
}
