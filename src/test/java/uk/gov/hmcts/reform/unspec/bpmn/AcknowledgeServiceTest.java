package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AcknowledgeServiceTest extends BpmnBaseTest {

    public static final String NOTIFY_APPLICANT_SOLICITOR_1 = "NOTIFY_APPLICANT_SOLICITOR1_FOR_SERVICE_ACKNOWLEDGEMENT";

    public AcknowledgeServiceTest() {
        super("service_acknowledgement.bpmn", "ServiceAcknowledgementHandling");
    }

    @Test
    void shouldSuccessfullyCompleteAcknowledgeService() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //complete the start business process
        ExternalTask startBusinessTask = getNextExternalTask(START_BUSINESS_TOPIC);
        completeExternalTask(startBusinessTask, START_BUSINESS_TOPIC, START_BUSINESS_EVENT);

        //complete the notification to claimant
        ExternalTask claimantNotificationTask = getNextExternalTask(PROCESS_CASE_EVENT_TOPIC);
        completeExternalTask(claimantNotificationTask, PROCESS_CASE_EVENT_TOPIC, NOTIFY_APPLICANT_SOLICITOR_1);

        assertNoExternalTasksLeft();
    }
}
