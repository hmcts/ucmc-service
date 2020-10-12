package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefendantResponseTest extends BpmnBaseTest {

    public static final String TOPIC_NAME = "processCaseEvent";

    public DefendantResponseTest() {
        super("camunda/defendant_response.bpmn", "DEFENDANT_RESPONSE");
    }

    @Test
    void caseEventTaskShouldFireCaseEventExternalTask_whenStarted() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert topic names
        assertThat(getTopics()).containsOnly(TOPIC_NAME);

        //assert message start event
        assertThat(getProcessDefinitionByMessage("DEFENDANT_RESPONSE")).isNotNull();

        //get external tasks
        List<ExternalTask> externalTasks = getExternalTasks();

        //assert task is as expected
        assertThat(externalTasks).hasSize(1);
        assertThat(externalTasks.get(0).getTopicName()).isEqualTo("processCaseEvent");

        //fetch and complete task
        List<LockedExternalTask> lockedExternalTasks = fetchAndLockTask(TOPIC_NAME);

        assertThat(lockedExternalTasks).hasSize(1);
        assertThat(lockedExternalTasks.get(0).getVariables())
            .containsEntry("CASE_EVENT", "NOTIFY_RESPONDENT_SOLICITOR1_FOR_CASE_HANDED_OFFLINE");
        assertThat(lockedExternalTasks.get(0).getActivityId()).isEqualTo(
            "DefendantResponseCaseHandedOfflineEmailRespondentSolicitor1");

        completeTask(lockedExternalTasks.get(0).getId());

        //fetch and complete task
        lockedExternalTasks = fetchAndLockTask(TOPIC_NAME);

        assertThat(lockedExternalTasks).hasSize(1);
        assertThat(lockedExternalTasks.get(0).getVariables())
            .containsEntry("CASE_EVENT", "NOTIFY_APPLICANT_SOLICITOR1_FOR_CASE_HANDED_OFFLINE");
        assertThat(lockedExternalTasks.get(0).getActivityId()).isEqualTo(
            "DefendantResponseCaseHandedOfflineEmailApplicantSolicitor1");
        completeTask(lockedExternalTasks.get(0).getId());

        //assert no external tasks left
        List<ExternalTask> externalTasksAfter = getExternalTasks();
        assertThat(externalTasksAfter).isEmpty();
    }
}
