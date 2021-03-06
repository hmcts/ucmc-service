package uk.gov.hmcts.reform.unspec.bpmn;

import org.camunda.bpm.engine.externaltask.ExternalTask;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.unspec.service.flowstate.FlowState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.MAKE_PBA_PAYMENT;
import static uk.gov.hmcts.reform.unspec.handler.tasks.StartBusinessProcessTaskHandler.FLOW_STATE;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.AWAITING_CASE_NOTIFICATION;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.PAYMENT_FAILED;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.PAYMENT_SUCCESSFUL;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.PENDING_CASE_ISSUED;

class CreateClaimTest extends BpmnBaseTest {

    private static final String MESSAGE_NAME = "CREATE_CLAIM";
    private static final String PROCESS_ID = "CREATE_CLAIM_PROCESS_ID";
    private static final String NOTIFY_RESPONDENT_SOLICITOR_1_FAILED_PAYMENT
        = "NOTIFY_APPLICANT_SOLICITOR1_FOR_FAILED_PAYMENT";
    private static final String NOTIFY_RESPONDENT_SOLICITOR_1_FAILED_PAYMENT_ACTIVITY_ID
        = "CreateClaimPaymentFailedNotifyApplicantSolicitor1";
    private static final String MAKE_PAYMENT_ACTIVITY_ID = "CreateClaimMakePayment";
    private static final String PROCESS_PAYMENT_TOPIC = "processPayment";
    private static final String GENERATE_CLAIM_FORM = "GENERATE_CLAIM_FORM";
    private static final String CLAIM_FORM_ACTIVITY_ID = "GenerateClaimForm";
    private static final String NOTIFY_APPLICANT_SOLICITOR_1_CLAIM_PROCEEDS_OFFLINE
        = "NOTIFY_APPLICANT_SOLICITOR1_FOR_RESPONDENT_LITIGANT_IN_PERSON";
    private static final String NOTIFY_APPLICANT_SOLICITOR_1_CLAIM_PROCEEDS_OFFLINE_ACTIVITY_ID
        = "CreateClaimProceedsOfflineNotifyApplicantSolicitor1";
    private static final String NOTIFY_RPA_ON_CASE_HANDED_OFFLINE = "NOTIFY_RPA_ON_CASE_HANDED_OFFLINE";
    private static final String NOTIFY_RPA_ON_CASE_HANDED_OFFLINE_ACTIVITY_ID = "NotifyRoboticsOnCaseHandedOffline";
    private static final String CASE_ASSIGNMENT_EVENT = "ASSIGN_CASE_TO_APPLICANT_SOLICITOR1";
    private static final String CASE_ASSIGNMENT_ACTIVITY = "CaseAssignmentToApplicantSolicitor1";

    public CreateClaimTest() {
        super("create_claim.bpmn", "CREATE_CLAIM_PROCESS_ID");
    }

    @Test
    void shouldSuccessfullyCompleteCreateClaim_whenPaymentWasSuccessful() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage(MESSAGE_NAME).getKey()).isEqualTo(PROCESS_ID);

        VariableMap variables = Variables.createVariables();

        //complete the start business process
        ExternalTask startBusiness = assertNextExternalTask(START_BUSINESS_TOPIC);
        assertCompleteExternalTask(
            startBusiness,
            START_BUSINESS_TOPIC,
            START_BUSINESS_EVENT,
            START_BUSINESS_ACTIVITY,
            variables
        );

        //complete the case assignment process
        completeCaseAssignment(variables);

        //complete the payment
        variables.putValue(FLOW_STATE, PAYMENT_SUCCESSFUL.fullName());

        ExternalTask paymentTask = assertNextExternalTask(PROCESS_PAYMENT_TOPIC);
        assertCompleteExternalTask(
            paymentTask,
            PROCESS_PAYMENT_TOPIC,
            MAKE_PBA_PAYMENT.name(),
            MAKE_PAYMENT_ACTIVITY_ID,
            variables
        );

        //complete the document generation
        variables.putValue(FLOW_STATE, AWAITING_CASE_NOTIFICATION.fullName());

        ExternalTask documentGeneration = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            documentGeneration,
            PROCESS_CASE_EVENT,
            GENERATE_CLAIM_FORM,
            CLAIM_FORM_ACTIVITY_ID,
            variables
        );

        //end business process
        ExternalTask endBusinessProcess = assertNextExternalTask(END_BUSINESS_PROCESS);
        completeBusinessProcess(endBusinessProcess);

        assertNoExternalTasksLeft();
    }

    @Test
    void shouldSuccessfullyCompleteCreateClaim_whenPaymentFailed() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage("CREATE_CLAIM").getKey())
            .isEqualTo("CREATE_CLAIM_PROCESS_ID");

        VariableMap variables = Variables.createVariables();
        variables.putValue(FLOW_STATE, PENDING_CASE_ISSUED.fullName());

        //complete the start business process
        ExternalTask startBusiness = assertNextExternalTask(START_BUSINESS_TOPIC);
        assertCompleteExternalTask(
            startBusiness,
            START_BUSINESS_TOPIC,
            START_BUSINESS_EVENT,
            START_BUSINESS_ACTIVITY,
            variables
        );

        //complete the case assignment process
        completeCaseAssignment(variables);

        //complete the payment
        variables.putValue(FLOW_STATE, PAYMENT_FAILED.fullName());

        ExternalTask paymentTask = assertNextExternalTask(PROCESS_PAYMENT_TOPIC);
        assertCompleteExternalTask(
            paymentTask,
            PROCESS_PAYMENT_TOPIC,
            MAKE_PBA_PAYMENT.name(),
            MAKE_PAYMENT_ACTIVITY_ID,
            variables
        );

        //complete the notification
        ExternalTask notificationTask = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            notificationTask,
            PROCESS_CASE_EVENT,
            NOTIFY_RESPONDENT_SOLICITOR_1_FAILED_PAYMENT,
            NOTIFY_RESPONDENT_SOLICITOR_1_FAILED_PAYMENT_ACTIVITY_ID
        );

        //end business process
        ExternalTask endBusinessProcess = assertNextExternalTask(END_BUSINESS_PROCESS);
        completeBusinessProcess(endBusinessProcess);

        assertNoExternalTasksLeft();
    }

    @ParameterizedTest
    @EnumSource(value = FlowState.Main.class,
        names = {"PROCEEDS_OFFLINE_UNREPRESENTED_DEFENDANT", "PENDING_CLAIM_ISSUED_UNREGISTERED_DEFENDANT"})
    void shouldSuccessfullyCompleteCreateClaim_whenClaimTakenOffline(FlowState.Main flowState) {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage(MESSAGE_NAME).getKey()).isEqualTo(PROCESS_ID);

        VariableMap variables = Variables.createVariables();

        //complete the start business process
        ExternalTask startBusiness = assertNextExternalTask(START_BUSINESS_TOPIC);
        assertCompleteExternalTask(
            startBusiness,
            START_BUSINESS_TOPIC,
            START_BUSINESS_EVENT,
            START_BUSINESS_ACTIVITY,
            variables
        );

        //complete the case assignment process
        completeCaseAssignment(variables);

        //complete the payment
        variables.putValue(FLOW_STATE, PAYMENT_SUCCESSFUL.fullName());

        ExternalTask paymentTask = assertNextExternalTask(PROCESS_PAYMENT_TOPIC);
        assertCompleteExternalTask(
            paymentTask,
            PROCESS_PAYMENT_TOPIC,
            MAKE_PBA_PAYMENT.name(),
            MAKE_PAYMENT_ACTIVITY_ID,
            variables
        );

        //complete the document generation
        variables.putValue(FLOW_STATE, flowState.fullName());

        ExternalTask documentGeneration = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            documentGeneration,
            PROCESS_CASE_EVENT,
            GENERATE_CLAIM_FORM,
            CLAIM_FORM_ACTIVITY_ID,
            variables
        );

        //complete the notification
        ExternalTask notificationTask = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            notificationTask,
            PROCESS_CASE_EVENT,
            NOTIFY_APPLICANT_SOLICITOR_1_CLAIM_PROCEEDS_OFFLINE,
            NOTIFY_APPLICANT_SOLICITOR_1_CLAIM_PROCEEDS_OFFLINE_ACTIVITY_ID
        );

        //complete the Robotics notification
        ExternalTask forRobotics = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            forRobotics,
            PROCESS_CASE_EVENT,
            NOTIFY_RPA_ON_CASE_HANDED_OFFLINE,
            NOTIFY_RPA_ON_CASE_HANDED_OFFLINE_ACTIVITY_ID
        );

        //end business process
        ExternalTask endBusinessProcess = assertNextExternalTask(END_BUSINESS_PROCESS);
        completeBusinessProcess(endBusinessProcess);

        assertNoExternalTasksLeft();
    }

    private void completeCaseAssignment(VariableMap variables) {
        ExternalTask caseAssignment = assertNextExternalTask(PROCESS_CASE_EVENT);
        assertCompleteExternalTask(
            caseAssignment,
            PROCESS_CASE_EVENT,
            CASE_ASSIGNMENT_EVENT,
            CASE_ASSIGNMENT_ACTIVITY,
            variables
        );
    }

    @Test
    void shouldAbort_whenStartBusinessProcessThrowsAnError() {
        //assert process has started
        assertFalse(processInstance.isEnded());

        //assert message start event
        assertThat(getProcessDefinitionByMessage(MESSAGE_NAME).getKey()).isEqualTo(PROCESS_ID);

        VariableMap variables = Variables.createVariables();

        //fail the start business process
        ExternalTask startBusiness = assertNextExternalTask(START_BUSINESS_TOPIC);
        assertFailExternalTask(startBusiness, START_BUSINESS_TOPIC, START_BUSINESS_EVENT, START_BUSINESS_ACTIVITY);

        assertNoExternalTasksLeft();
    }
}
