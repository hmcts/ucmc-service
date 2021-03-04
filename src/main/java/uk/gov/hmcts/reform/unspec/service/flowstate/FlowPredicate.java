package uk.gov.hmcts.reform.unspec.service.flowstate;

import uk.gov.hmcts.reform.unspec.enums.YesOrNo;
import uk.gov.hmcts.reform.unspec.model.CaseData;

import java.util.function.Predicate;

import static uk.gov.hmcts.reform.unspec.enums.CaseState.CLOSED;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.PROCEEDS_WITH_OFFLINE_JOURNEY;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.STAYED;
import static uk.gov.hmcts.reform.unspec.enums.PaymentStatus.FAILED;
import static uk.gov.hmcts.reform.unspec.enums.PaymentStatus.SUCCESS;
import static uk.gov.hmcts.reform.unspec.enums.RespondentResponseType.COUNTER_CLAIM;
import static uk.gov.hmcts.reform.unspec.enums.RespondentResponseType.FULL_ADMISSION;
import static uk.gov.hmcts.reform.unspec.enums.RespondentResponseType.FULL_DEFENCE;
import static uk.gov.hmcts.reform.unspec.enums.RespondentResponseType.PART_ADMISSION;

public class FlowPredicate {

    public static final Predicate<CaseData> pendingCaseIssued = caseData ->
        caseData.getLegacyCaseReference() != null;

    public static final Predicate<CaseData> respondent1NotRepresented = caseData ->
        caseData.getClaimIssuedDate() != null && caseData.getRespondent1Represented() == YesOrNo.NO;

    public static final Predicate<CaseData> paymentFailed = caseData ->
        caseData.getPaymentDetails() != null && caseData.getPaymentDetails().getStatus() == FAILED;

    public static final Predicate<CaseData> paymentSuccessful = caseData ->
        caseData.getPaymentDetails() != null && caseData.getPaymentDetails().getStatus() == SUCCESS;

    public static final Predicate<CaseData> claimIssued = caseData ->
        caseData.getClaimIssuedDate() != null && caseData.getRespondent1Represented() == YesOrNo.YES;

    public static final Predicate<CaseData> claimNotified = caseData ->
        caseData.getClaimNotificationDate() != null;

    public static final Predicate<CaseData> claimDetailsNotified = caseData ->
        caseData.getClaimDetailsNotificationDate() != null;

    public static final Predicate<CaseData> respondentAcknowledgeService = caseData ->
        caseData.getRespondent1ClaimResponseIntentionType() != null
            && caseData.getRespondent1ClaimResponseType() == null
            && caseData.getRespondent1ClaimResponseDocument() == null
            && caseData.getCcdState() != CLOSED;

    public static final Predicate<CaseData> respondentFullDefence = caseData ->
        caseData.getRespondent1ClaimResponseType() == FULL_DEFENCE
            && caseData.getCcdState() != CLOSED
            && caseData.getCcdState() != STAYED;

    public static final Predicate<CaseData> respondentFullAdmission = caseData ->
        caseData.getRespondent1ClaimResponseType() == FULL_ADMISSION
            && caseData.getCcdState() != CLOSED
            && caseData.getCcdState() != STAYED;

    public static final Predicate<CaseData> respondentPartAdmission = caseData ->
        caseData.getRespondent1ClaimResponseType() == PART_ADMISSION
            && caseData.getCcdState() != CLOSED
            && caseData.getCcdState() != STAYED;

    public static final Predicate<CaseData> respondentCounterClaim = caseData ->
        caseData.getRespondent1ClaimResponseType() == COUNTER_CLAIM
            && caseData.getCcdState() != CLOSED
            && caseData.getCcdState() != STAYED;

    public static final Predicate<CaseData> applicantRespondToDefence = caseData ->
        caseData.getApplicant1ProceedWithClaim() != null
            && caseData.getApplicant1DefenceResponseDocument() != null;

    public static final Predicate<CaseData> schedulerStayClaim = caseData ->
        caseData.getCcdState() == STAYED;

    public static final Predicate<CaseData> claimWithdrawn = caseData ->
        caseData.getWithdrawClaim() != null
            && caseData.getCcdState() == CLOSED;

    public static final Predicate<CaseData> respondentAgreedExtension = caseData ->
        caseData.getRespondentSolicitor1AgreedDeadlineExtension() != null;

    public static final Predicate<CaseData> claimDiscontinued = caseData ->
        caseData.getDiscontinueClaim() != null
            && caseData.getCcdState() == CLOSED;

    public static final Predicate<CaseData> claimTakenOffline = caseData ->
        caseData.getCcdState() == PROCEEDS_WITH_OFFLINE_JOURNEY;

    public static final Predicate<CaseData> caseProceedsInCaseman = caseData ->
        caseData.getClaimProceedsInCaseman() != null;

    private FlowPredicate() {
        //Utility class
    }

}
