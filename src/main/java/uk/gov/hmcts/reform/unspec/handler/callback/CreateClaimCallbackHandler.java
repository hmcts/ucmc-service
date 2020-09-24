package uk.gov.hmcts.reform.unspec.handler.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.config.ClaimIssueConfiguration;
import uk.gov.hmcts.reform.unspec.enums.ClaimType;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.helpers.PartyDateOfBirthHelper;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.model.ClaimValue;
import uk.gov.hmcts.reform.unspec.model.Party;
import uk.gov.hmcts.reform.unspec.model.documents.CaseDocument;
import uk.gov.hmcts.reform.unspec.model.documents.DocumentType;
import uk.gov.hmcts.reform.unspec.repositories.ReferenceNumberRepository;
import uk.gov.hmcts.reform.unspec.service.DeadlinesCalculator;
import uk.gov.hmcts.reform.unspec.service.IssueDateCalculator;
import uk.gov.hmcts.reform.unspec.service.docmosis.sealedclaim.SealedClaimFormGenerator;
import uk.gov.hmcts.reform.unspec.utils.ElementUtils;
import uk.gov.hmcts.reform.unspec.utils.PartyNameUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.unspec.callback.CallbackParams.Params.BEARER_TOKEN;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CREATE_CASE;
import static uk.gov.hmcts.reform.unspec.enums.AllocatedTrack.getAllocatedTrack;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE_TIME_AT;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDateTime;

@Service
@RequiredArgsConstructor
public class CreateClaimCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(CREATE_CASE);
    public static final String CONFIRMATION_SUMMARY = "<br />Follow these steps to serve a claim:"
        + "\n* <a href=\"%s\" target=\"_blank\">[Download the sealed claim form]</a> (PDF, %sKB)"
        + "\n* Send the form, particulars of claim and "
        + "<a href=\"%s\" target=\"_blank\">a response pack</a> (PDF, 266 KB) to the defendant by %s"
        + "\n* Confirm service online within 21 days of sending the form, particulars and response pack, before"
        + " 4pm if you're doing this on the due day";
    public static final String RESPONDENT = "respondent1";
    public static final String APPLICANT_1 = "applicant1";

    private final SealedClaimFormGenerator sealedClaimFormGenerator;
    private final ClaimIssueConfiguration claimIssueConfiguration;
    private final CaseDetailsConverter caseDetailsConverter;
    private final IssueDateCalculator issueDateCalculator;
    private final DeadlinesCalculator deadlinesCalculator;
    private final ReferenceNumberRepository referenceNumberRepository;
    private final PartyDateOfBirthHelper dateOfBirthHelper;
    private final ObjectMapper mapper;

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return Map.of(
            CallbackType.MID, this::validateClaimValues,
            CallbackType.MID_SECONDARY, this::validateDateOfBirth,
            CallbackType.ABOUT_TO_SUBMIT, this::issueClaim,
            CallbackType.SUBMITTED, this::buildConfirmation
        );
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse validateDateOfBirth(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(dateOfBirthHelper.validateDateOfBirth(callbackParams, APPLICANT_1))
            .build();
    }

    private CallbackResponse validateClaimValues(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        List<String> errors = new ArrayList<>();

        ClaimValue claimValue = caseData.getClaimValue();
        if (claimValue.hasLargerLowerValue()) {
            errors.add("CONTENT TBC: Higher value must not be lower than the lower value.");
        }

        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        if (errors.isEmpty()) {
            ClaimType claimType = caseData.getClaimType();
            data.put("allocatedTrack", getAllocatedTrack(claimValue, claimType));
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .errors(errors)
            .build();
    }

    private CallbackResponse issueClaim(CallbackParams callbackParams) {
        CaseDetails caseDetails = callbackParams.getRequest().getCaseDetails();
        LocalDateTime submittedAt = LocalDateTime.now();
        LocalDate issueDate = issueDateCalculator.calculateIssueDay(submittedAt);
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        String referenceNumber = referenceNumberRepository.getReferenceNumber();

        Party respondent = updatePartyWithPartyName(caseDetails.getData().get(RESPONDENT));
        Party applicant = updatePartyWithPartyName(caseDetails.getData().get(APPLICANT_1));
        CaseDocument sealedClaim = sealedClaimFormGenerator.generate(
            caseData.toBuilder()
                .claimIssuedDate(issueDate)
                .legacyCaseReference(referenceNumber)
                .claimSubmittedDateTime(submittedAt)
                .applicant1(applicant)
                .respondent1(respondent)
                .build(),
            callbackParams.getParams().get(BEARER_TOKEN).toString()
        );

        Map<String, Object> data = caseDetails.getData();
        data.put("claimSubmittedDateTime", submittedAt);
        data.put("claimIssuedDate", issueDate);
        data.put(
            "confirmationOfServiceDeadline",
            deadlinesCalculator.calculateConfirmationOfServiceDeadline(issueDate)
        );
        data.put("systemGeneratedCaseDocuments", ElementUtils.wrapElements(sealedClaim));

        data.put(RESPONDENT, respondent);
        data.put(APPLICANT_1, applicant);
        data.put("legacyCaseReference", referenceNumber);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private Party updatePartyWithPartyName(Object partyObject) {
        Party party = mapper.convertValue(partyObject, Party.class);

        return party.toBuilder()
            .partyName(PartyNameUtils.getPartyNameBasedOnType(party))
            .partyTypeDisplayValue(party.getType().getDisplayValue())
            .build();
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());
        Long documentSize = ElementUtils.unwrapElements(caseData.getSystemGeneratedCaseDocuments()).stream()
            .filter(c -> c.getDocumentType() == DocumentType.SEALED_CLAIM)
            .findFirst()
            .map(CaseDocument::getDocumentSize)
            .orElse(0L);

        LocalDateTime serviceDeadline = LocalDate.now().plusDays(112).atTime(23, 59);
        String formattedServiceDeadline = formatLocalDateTime(serviceDeadline, DATE_TIME_AT);
        String claimNumber = caseData.getLegacyCaseReference();

        String body = format(
            CONFIRMATION_SUMMARY,
            format("/cases/case-details/%s#CaseDocuments", caseData.getCcdCaseReference()),
            documentSize / 1024,
            claimIssueConfiguration.getResponsePackLink(),
            formattedServiceDeadline
        );

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(format("# Your claim has been issued%n## Claim number: %s", claimNumber))
            .confirmationBody(body)
            .build();
    }
}
