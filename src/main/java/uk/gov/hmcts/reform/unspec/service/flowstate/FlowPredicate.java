package uk.gov.hmcts.reform.unspec.service.flowstate;

import uk.gov.hmcts.reform.unspec.enums.YesOrNo;
import uk.gov.hmcts.reform.unspec.model.CaseData;

import java.util.Objects;
import java.util.function.Predicate;

import static uk.gov.hmcts.reform.unspec.enums.CaseState.CLOSED;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.PROCEEDS_IN_HERITAGE_SYSTEM;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.STAYED;
import static uk.gov.hmcts.reform.unspec.enums.PaymentStatus.FAILED;
import static uk.gov.hmcts.reform.unspec.enums.PaymentStatus.SUCCESS;

public class FlowPredicate {

    public static final Predicate<CaseData> pendingCaseIssued = caseData ->
        caseData.getClaimSubmittedDateTime() != null && caseData.getRespondent1Represented() == YesOrNo.YES;

    public static final Predicate<CaseData> respondent1NotRepresented = caseData ->
        caseData.getRespondent1Represented() == YesOrNo.NO;

    public static final Predicate<CaseData> paymentFailed = caseData ->
        caseData.getPaymentDetails() != null && caseData.getPaymentDetails().getStatus() == FAILED;

    public static final Predicate<CaseData> paymentSuccessful = caseData ->
        caseData.getPaymentDetails() != null && caseData.getPaymentDetails().getStatus() == SUCCESS;

    public static final Predicate<CaseData> claimIssued = caseData ->
        caseData.getClaimIssuedDate() != null;

    public static final Predicate<CaseData> applicantConfirmService = caseData ->
        caseData.getDeemedServiceDateToRespondentSolicitor1() != null
            && Objects.isNull(caseData.getWithdrawClaim())
            && Objects.isNull(caseData.getDiscontinueClaim());

    public static final Predicate<CaseData> respondentAcknowledgeService = caseData ->
        caseData.getRespondent1ClaimResponseIntentionType() != null
            && caseData.getRespondent1ClaimResponseDocument() == null;

    public static final Predicate<CaseData> respondentRespondToClaim = caseData ->
        caseData.getRespondent1ClaimResponseDocument() != null;

    public static final Predicate<CaseData> respondentAskForAnExtension = caseData ->
        caseData.getRespondentSolicitor1claimResponseExtensionProposedDeadline() != null;

    public static final Predicate<CaseData> applicantRespondToRequestForExtension = caseData ->
        caseData.getRespondentSolicitor1claimResponseExtensionAccepted() != null;

    public static final Predicate<CaseData> applicantRespondToDefence = caseData ->
        caseData.getApplicant1ProceedWithClaim() != null
            && caseData.getApplicant1DefenceResponseDocument() != null;

    public static final Predicate<CaseData> schedulerStayClaim = caseData ->
        caseData.getCcdState() == STAYED && caseData.getDeemedServiceDateToRespondentSolicitor1() == null;

    public static final Predicate<CaseData> claimWithdrawn = caseData ->
        caseData.getWithdrawClaim() != null
            && caseData.getCcdState() == CLOSED;

    public static final Predicate<CaseData> claimDiscontinued = caseData ->
        caseData.getDiscontinueClaim() != null
            && caseData.getCcdState() == CLOSED;

    public static final Predicate<CaseData> claimTakenOffline = caseData ->
        caseData.getCcdState() == PROCEEDS_IN_HERITAGE_SYSTEM;

    private FlowPredicate() {
        //Utility class
    }

}
