package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RespondExtensionTest extends BpmnBaseTest {

    public static final String ACTIVITY_ID = "RespondExtensionEmailRespondentSolicitor1";
    public static final String NOTIFY_RESPONDENT_SOLICITOR_1 = "NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE";

    public RespondExtensionTest() {
        super("respond_extension.bpmn", "RESPOND_EXTENSION_PROCESS_ID");
    }

    @Test
    void shouldSuccessfullyCompleteResponseExtension() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage("RESPOND_EXTENSION").getKey())
            .isEqualTo("RESPOND_EXTENSION_PROCESS_ID");

        //complete the start business process
        ExternalTask startBusiness = assertNextExternalTask(START_BUSINESS_TOPIC);
        assertCompleteExternalTask(startBusiness, START_BUSINESS_TOPIC, START_BUSINESS_EVENT, START_BUSINESS_ACTIVITY);

        //complete the notification
        ExternalTask notificationTask = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(notificationTask, PROCESS_CASE_EVENT, NOTIFY_RESPONDENT_SOLICITOR_1, ACTIVITY_ID);

        //end business process
        ExternalTask endBusinessProcess = assertNextExternalTask(END_BUSINESS_PROCESS);
        completeBusinessProcess(endBusinessProcess);

        assertNoExternalTasksLeft();
    }
}
