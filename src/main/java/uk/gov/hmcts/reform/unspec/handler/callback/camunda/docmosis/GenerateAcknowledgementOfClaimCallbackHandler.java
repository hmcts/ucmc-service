package uk.gov.hmcts.reform.unspec.handler.callback.camunda.docmosis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.model.common.Element;
import uk.gov.hmcts.reform.unspec.model.documents.CaseDocument;
import uk.gov.hmcts.reform.unspec.service.docmosis.aos.AcknowledgementOfClaimGenerator;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.unspec.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.GENERATE_ACKNOWLEDGEMENT_OF_CLAIM;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.GENERATE_ACKNOWLEDGEMENT_OF_SERVICE;
import static uk.gov.hmcts.reform.unspec.utils.ElementUtils.element;

@Service
@RequiredArgsConstructor
public class GenerateAcknowledgementOfClaimCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = List.of(
        //TODO: CMC-957 backwards compatibility
        GENERATE_ACKNOWLEDGEMENT_OF_SERVICE,
        GENERATE_ACKNOWLEDGEMENT_OF_CLAIM
    );

    private final AcknowledgementOfClaimGenerator acknowledgementOfClaimGenerator;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::prepareAcknowledgementOfClaim);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse prepareAcknowledgementOfClaim(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        CaseData.CaseDataBuilder caseDataBuilder = caseData.toBuilder();

        CaseDocument acknowledgementOfClaim = acknowledgementOfClaimGenerator.generate(
            caseData,
            callbackParams.getParams().get(BEARER_TOKEN).toString()
        );

        List<Element<CaseDocument>> systemGeneratedCaseDocuments = caseData.getSystemGeneratedCaseDocuments();
        systemGeneratedCaseDocuments.add(element(acknowledgementOfClaim));
        caseDataBuilder.systemGeneratedCaseDocuments(systemGeneratedCaseDocuments);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetailsConverter.toMap(caseDataBuilder.build()))
            .build();
    }
}
