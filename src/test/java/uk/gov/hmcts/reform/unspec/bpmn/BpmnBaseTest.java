package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.externaltask.LockedExternalTask;
import org.camunda.bpm.engine.management.JobDefinition;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration;

public abstract class BpmnBaseTest {

    public static final String WORKER_ID = "test-worker";
    public static final String START_BUSINESS_TOPIC = "START_BUSINESS_PROCESS";
    public static final String START_BUSINESS_EVENT = "START_BUSINESS_PROCESS";
    public static final String PROCESS_CASE_EVENT_TOPIC = "processCaseEvent";

    public final String bpmnFileName;
    public final String processId;
    public Deployment deployment;
    public static ProcessEngine engine;

    public ProcessInstance processInstance;

    public BpmnBaseTest(String bpmnFileName, String processId) {
        this.bpmnFileName = bpmnFileName;
        this.processId = processId;
    }

    @BeforeAll
    static void startEngine() {
        ProcessEngineConfiguration configuration = createStandaloneInMemProcessEngineConfiguration();

        engine = configuration.buildProcessEngine();
    }

    @BeforeEach
    void setup() {
        //deploy process
        deployment = engine.getRepositoryService().createDeployment().addClasspathResource(bpmnFileName).deploy();
        processInstance = engine.getRuntimeService().startProcessInstanceByKey(processId);
    }

    @AfterEach
    void tearDown() {
        engine.getRepositoryService().deleteDeployment(deployment.getId());
    }

    @AfterAll
    static void shutDown() {
        engine.close();
    }

    /**
     * Retrieves an explicit representation of a task to trigger a process execution, i.e whenever a wait
     * state is triggered in a bmpn diagram, for example, a timer event or a user task.
     *
     * @return a list of jobs in the current execution.
     */
    public List<JobDefinition> getJobs() {
        return engine.getManagementService().createJobDefinitionQuery().list();
    }

    /**
     * Retrieves a list of topic names defined within the bpmn diagram.
     *
     * @return the list of topics in the current execution.
     */
    public List<String> getTopics() {
        return engine.getExternalTaskService().getTopicNames();
    }

    /**
     * Retrieves a list of external tasks.
     *
     * @return a list of external tasks available in the current execution.
     */
    public List<ExternalTask> getExternalTasks() {
        return engine.getExternalTaskService().createExternalTaskQuery().list();
    }

    /**
     * Fetches an external task by topic name and locks it to a worker ready for handling.
     *
     * @param topicName the name of the topic to fetch.
     * @return a list of external tasks locked to the worked id.
     */
    public List<LockedExternalTask> fetchAndLockTask(String topicName) {
        return engine.getExternalTaskService()
            .fetchAndLock(1, WORKER_ID)
            .topic(topicName, 100)
            .execute();
    }

    /**
     * Completes an external task with the given id.
     *
     * @param taskId the id of the external task to complete.
     */
    public void completeTask(String taskId) {
        engine.getExternalTaskService().complete(taskId, WORKER_ID);
    }

    /**
     * Get external task for topic name.
     */
    public ExternalTask getNextExternalTask(String topicName) {
        assertThat(getTopics()).containsOnly(topicName);

        List<ExternalTask> externalTasks = getExternalTasks();
        assertThat(externalTasks).hasSize(1);

        ExternalTask externalTask = externalTasks.get(0);
        assertThat(externalTask.getTopicName()).isEqualTo(topicName);

        return externalTask;
    }

    /**
     * Completes the external task with topic name.
     *
     * @param externalTask the id of the external task to complete.
     * @param topicName    is taskName.
     * @param caseEvent    is input variable for external task.
     */
    public void completeExternalTask(ExternalTask externalTask, String topicName, String caseEvent) {
        assertThat(externalTask.getTopicName()).isEqualTo(topicName);

        List<LockedExternalTask> lockedProcessTask = fetchAndLockTask(topicName);

        assertThat(lockedProcessTask).hasSize(1);

        assertThat(lockedProcessTask.get(0).getVariables())
            .containsEntry("caseId", "1601986692564009")
            .containsEntry("caseEvent", caseEvent);

        completeTask(lockedProcessTask.get(0).getId());
    }

    public void assertNoExternalTasksLeft() {
        List<ExternalTask> externalTasksAfter = getExternalTasks();
        assertThat(externalTasksAfter).isEmpty();
    }
}
