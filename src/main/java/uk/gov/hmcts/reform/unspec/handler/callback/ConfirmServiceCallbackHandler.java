package uk.gov.hmcts.reform.unspec.handler.callback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.enums.ServedDocuments;
import uk.gov.hmcts.reform.unspec.enums.ServiceMethod;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.model.common.Element;
import uk.gov.hmcts.reform.unspec.model.documents.CaseDocument;
import uk.gov.hmcts.reform.unspec.model.documents.DocumentType;
import uk.gov.hmcts.reform.unspec.service.docmosis.cos.CertificateOfServiceGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.unspec.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CONFIRM_SERVICE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE_TIME_AT;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDate;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDateTime;
import static uk.gov.hmcts.reform.unspec.utils.ElementUtils.element;
import static uk.gov.hmcts.reform.unspec.utils.ElementUtils.unwrapElements;
import static uk.gov.hmcts.reform.unspec.utils.ElementUtils.wrapElements;

@Service
@RequiredArgsConstructor
public class ConfirmServiceCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(CONFIRM_SERVICE);
    public static final String CONFIRMATION_SUMMARY = "<br /> Deemed date of service: %s."
        + "<br />The defendant must respond before %s."
        + "\n\n[Download certificate of service](%s) (PDF, %s KB)";

    private final CertificateOfServiceGenerator certificateOfServiceGenerator;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return Map.of(
            CallbackType.ABOUT_TO_START, this::prepopulateServedDocuments,
            CallbackType.MID, this::checkServedDocumentsOtherHasWhiteSpace,
            CallbackType.ABOUT_TO_SUBMIT, this::prepareCertificateOfService,
            CallbackType.SUBMITTED, this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse prepopulateServedDocuments(CallbackParams callbackParams) {
        List<ServedDocuments> servedDocuments = List.of(ServedDocuments.CLAIM_FORM);

        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        data.put("servedDocuments", servedDocuments);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private CallbackResponse checkServedDocumentsOtherHasWhiteSpace(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        List<String> errors = new ArrayList<>();
        var servedDocumentsOther = data.get("servedDocumentsOther");

        if (servedDocumentsOther != null && servedDocumentsOther.toString().isBlank()) {
            errors.add("CONTENT TBC: please enter a valid value for other documents");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .errors(errors)
            .build();
    }

    private CallbackResponse prepareCertificateOfService(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());

        ServiceMethod serviceMethod = caseData.getServiceMethod();
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        // TODO: this field will be different (date / date time) in CCD depending on service method.
        LocalDate serviceDate = caseData.getServiceDate();

        LocalDate deemedDateOfService = serviceMethod.getDeemedDateOfService(serviceDate);
        LocalDateTime responseDeadline = addFourteenDays(deemedDateOfService);
        data.put("deemedDateOfService", deemedDateOfService);
        data.put("responseDeadline", responseDeadline);

        CaseData caseDateUpdated = caseData.toBuilder()
            .deemedDateOfService(deemedDateOfService)
            .responseDeadline(responseDeadline)
            .build();

        CaseDocument certificateOfService = certificateOfServiceGenerator.generate(
            caseDateUpdated,
            callbackParams.getParams().get(BEARER_TOKEN).toString()
        );
        List<Element<CaseDocument>> systemGeneratedCaseDocuments = caseData.getSystemGeneratedCaseDocuments();
        if (ObjectUtils.isEmpty(systemGeneratedCaseDocuments)) {
            data.put("systemGeneratedCaseDocuments", wrapElements(certificateOfService));
        } else {
            systemGeneratedCaseDocuments.add(element(certificateOfService));
            data.put("systemGeneratedCaseDocuments", systemGeneratedCaseDocuments);
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());

        LocalDate deemedDateOfService = caseData.getDeemedDateOfService();
        String formattedDeemedDateOfService = formatLocalDate(deemedDateOfService, DATE);
        String responseDeadlineDate = formatLocalDateTime(addFourteenDays(deemedDateOfService), DATE_TIME_AT);
        Long documentSize = unwrapElements(caseData.getSystemGeneratedCaseDocuments()).stream()
            .filter(c -> c.getDocumentType() == DocumentType.CERTIFICATE_OF_SERVICE)
            .findFirst()
            .map(CaseDocument::getDocumentSize)
            .orElse(0L);

        String body = format(
            CONFIRMATION_SUMMARY,
            formattedDeemedDateOfService,
            responseDeadlineDate,
            format("/cases/case-details/%s#CaseDocuments", caseData.getCcdCaseReference()),
            documentSize / 1024
        );

        return SubmittedCallbackResponse.builder()
            .confirmationHeader("# You've confirmed service")
            .confirmationBody(body)
            .build();
    }

    private LocalDateTime addFourteenDays(LocalDate deemedDateOfService) {
        return deemedDateOfService.plusDays(14).atTime(16, 0);
    }
}
