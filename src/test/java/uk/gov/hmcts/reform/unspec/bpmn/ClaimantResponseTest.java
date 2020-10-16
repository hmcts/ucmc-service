package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClaimantResponseTest extends BpmnBaseTest {

    private static final String RESPONDENT_SOLICITOR_1
        = "NOTIFY_RESPONDENT_SOLICITOR1_FOR_CASE_TRANSFERRED_TO_LOCAL_COURT";
    private static final String RESPONDENT_ACTIVITY_ID = "ClaimantResponseEmailRespondentSolicitor1";
    private static final String CLAIMANT_SOLICITOR_1
        = "NOTIFY_APPLICANT_SOLICITOR1_FOR_CASE_TRANSFERRED_TO_LOCAL_COURT";
    private static final String CLAIMANT_ACTIVITY_ID = "ClaimantResponseEmailApplicantSolicitor1";

    public ClaimantResponseTest() {
        super("claimant_response.bpmn", "CLAIMANT_RESPONSE_PROCESS_ID");
    }

    @Test
    void shouldSuccessfullyCompleteClaimantResponse() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage("CLAIMANT_RESPONSE").getKey())
            .isEqualTo("CLAIMANT_RESPONSE_PROCESS_ID");

        //complete the start business process
        ExternalTask startBusinessTask = getNextExternalTask(START_BUSINESS_TOPIC);
        completeExternalTask(startBusinessTask, START_BUSINESS_TOPIC, START_BUSINESS_EVENT, START_BUSINESS_ACTIVITY);

        //complete the notification
        ExternalTask forRespondent = getNextExternalTask(PROCESS_CASE_EVENT_TOPIC);
        completeExternalTask(forRespondent, PROCESS_CASE_EVENT_TOPIC, RESPONDENT_SOLICITOR_1, RESPONDENT_ACTIVITY_ID);

        //complete the notification
        ExternalTask forClaimant = getNextExternalTask(PROCESS_CASE_EVENT_TOPIC);
        completeExternalTask(forClaimant, PROCESS_CASE_EVENT_TOPIC, CLAIMANT_SOLICITOR_1, CLAIMANT_ACTIVITY_ID);

        assertNoExternalTasksLeft();
    }

}
