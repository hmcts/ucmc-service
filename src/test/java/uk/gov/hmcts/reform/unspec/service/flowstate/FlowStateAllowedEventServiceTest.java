package uk.gov.hmcts.reform.unspec.service.flowstate;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDetailsBuilder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.of;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.ACKNOWLEDGE_SERVICE;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.ADD_DEFENDANT_LITIGATION_FRIEND;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.ADD_OR_AMEND_CLAIM_DOCUMENTS;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CASE_PROCEEDS_IN_CASEMAN;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CLAIMANT_RESPONSE;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CREATE_CLAIM;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.DEFENDANT_RESPONSE;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.DISCONTINUE_CLAIM;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.MOVE_CLAIM_TO_STRUCK_OUT;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.MOVE_TO_STAYED;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.NOTIFY_DEFENDANT_OF_CLAIM;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.NOTIFY_DEFENDANT_OF_CLAIM_DETAILS;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.RESUBMIT_CLAIM;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.WITHDRAW_CLAIM;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.AWAITING_CASE_DETAILS_NOTIFICATION;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.AWAITING_CASE_NOTIFICATION;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.CLAIM_ISSUED;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.CLAIM_STAYED;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.DRAFT;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.FULL_DEFENCE;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.PAYMENT_FAILED;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.RESPONDED_TO_CLAIM;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.SERVICE_ACKNOWLEDGED;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    StateFlowEngine.class,
    FlowStateAllowedEventService.class
})
class FlowStateAllowedEventServiceTest {

    @Autowired
    FlowStateAllowedEventService flowStateAllowedEventService;

    static class GetFlowStateArguments implements ArgumentsProvider {

        @Override
        @SneakyThrows
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                of(CaseDataBuilder.builder().atStateClaimDraft().build(), DRAFT),
                of(CaseDataBuilder.builder().atStateAwaitingCaseNotification().build(), AWAITING_CASE_NOTIFICATION),
                of(
                    CaseDataBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    AWAITING_CASE_DETAILS_NOTIFICATION
                ),
                of(CaseDataBuilder.builder().atStateClaimCreated().build(), CLAIM_ISSUED),
                of(CaseDataBuilder.builder().atStateClaimStayed().build(), CLAIM_STAYED),
                of(CaseDataBuilder.builder().atStateServiceAcknowledge().build(), SERVICE_ACKNOWLEDGED),
                of(CaseDataBuilder.builder().atStateRespondedToClaim().build(), RESPONDED_TO_CLAIM),
                of(CaseDataBuilder.builder().atStateFullDefence().build(), CLAIM_STAYED)
            );
        }
    }

    @Nested
    class GetFlowState {

        @ParameterizedTest(name = "{index} => should return flow state {1} when case data {0}")
        @ArgumentsSource(GetFlowStateArguments.class)
        void shouldReturnValidState_whenCaseDataProvided(CaseData caseData, FlowState.Main flowState) {
            assertThat(flowStateAllowedEventService.getFlowState(caseData))
                .isEqualTo(flowState);
        }
    }

    @Nested
    class GetAllowedEventsForFlowState {

        @Test
        void shouldReturnValidEvents_whenFlowStateIsDraft() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(DRAFT.fullName()))
                .containsExactlyInAnyOrder(CREATE_CLAIM, WITHDRAW_CLAIM, DISCONTINUE_CLAIM);
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsAwaitingCaseNotification() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(AWAITING_CASE_NOTIFICATION.fullName()))
                .containsExactlyInAnyOrder(
                    NOTIFY_DEFENDANT_OF_CLAIM,
                    ADD_DEFENDANT_LITIGATION_FRIEND,
                    CASE_PROCEEDS_IN_CASEMAN,
                    ADD_OR_AMEND_CLAIM_DOCUMENTS
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsAwaitingCaseDetailsNotification() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(AWAITING_CASE_DETAILS_NOTIFICATION.fullName()))
                .containsExactlyInAnyOrder(
                    NOTIFY_DEFENDANT_OF_CLAIM_DETAILS,
                    ADD_DEFENDANT_LITIGATION_FRIEND,
                    CASE_PROCEEDS_IN_CASEMAN,
                    ADD_OR_AMEND_CLAIM_DOCUMENTS
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsClaimIssued() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(CLAIM_ISSUED.fullName()))
                .containsExactlyInAnyOrder(MOVE_TO_STAYED, ACKNOWLEDGE_SERVICE, ADD_DEFENDANT_LITIGATION_FRIEND,
                                           WITHDRAW_CLAIM, DISCONTINUE_CLAIM, CASE_PROCEEDS_IN_CASEMAN
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsServiceAcknowledge() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(SERVICE_ACKNOWLEDGED.fullName()))
                .containsExactlyInAnyOrder(DEFENDANT_RESPONSE, ADD_DEFENDANT_LITIGATION_FRIEND,
                                           WITHDRAW_CLAIM, DISCONTINUE_CLAIM, CASE_PROCEEDS_IN_CASEMAN
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsRespondToClaim() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(RESPONDED_TO_CLAIM.fullName()))
                .containsExactlyInAnyOrder(CLAIMANT_RESPONSE, ADD_DEFENDANT_LITIGATION_FRIEND, WITHDRAW_CLAIM,
                                           DISCONTINUE_CLAIM, MOVE_CLAIM_TO_STRUCK_OUT, CASE_PROCEEDS_IN_CASEMAN
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsFullDefence() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(FULL_DEFENCE.fullName()))
                .containsExactlyInAnyOrder(ADD_DEFENDANT_LITIGATION_FRIEND, WITHDRAW_CLAIM, DISCONTINUE_CLAIM,
                                           CASE_PROCEEDS_IN_CASEMAN
                );
        }

        @Test
        void shouldReturnValidEvents_whenFlowStateIsClaimStayed() {
            assertThat(flowStateAllowedEventService.getAllowedEvents(CLAIM_STAYED.fullName()))
                .containsExactlyInAnyOrder(WITHDRAW_CLAIM, DISCONTINUE_CLAIM);
        }
    }

    @Nested
    class IsEventAllowedOnFlowState {

        @ParameterizedTest
        @CsvSource({
            "DRAFT,CREATE_CLAIM",
            "CLAIM_ISSUED,MOVE_TO_STAYED",
            "CLAIM_ISSUED,ACKNOWLEDGE_SERVICE",
            "CLAIM_ISSUED,WITHDRAW_CLAIM",
            "CLAIM_ISSUED,CASE_PROCEEDS_IN_CASEMAN",
            "AWAITING_CASE_NOTIFICATION,NOTIFY_DEFENDANT_OF_CLAIM",
            "AWAITING_CASE_NOTIFICATION,ADD_DEFENDANT_LITIGATION_FRIEND",
            "AWAITING_CASE_NOTIFICATION,CASE_PROCEEDS_IN_CASEMAN",
            "AWAITING_CASE_NOTIFICATION,ADD_OR_AMEND_CLAIM_DOCUMENTS",
            "AWAITING_CASE_DETAILS_NOTIFICATION,NOTIFY_DEFENDANT_OF_CLAIM_DETAILS",
            "AWAITING_CASE_DETAILS_NOTIFICATION,ADD_DEFENDANT_LITIGATION_FRIEND",
            "AWAITING_CASE_DETAILS_NOTIFICATION,CASE_PROCEEDS_IN_CASEMAN",
            "AWAITING_CASE_DETAILS_NOTIFICATION,ADD_OR_AMEND_CLAIM_DOCUMENTS",
            "SERVICE_ACKNOWLEDGED,DEFENDANT_RESPONSE",
            "RESPONDED_TO_CLAIM,CLAIMANT_RESPONSE",
            "RESPONDED_TO_CLAIM,DISCONTINUE_CLAIM"
        })
        void shouldReturnTrue_whenEventIsAllowedAtGivenState(FlowState.Main flowState, CaseEvent caseEvent) {
            assertTrue(flowStateAllowedEventService.isAllowedOnState(flowState.fullName(), caseEvent));
        }

        @ParameterizedTest
        @CsvSource({
            "DRAFT,CASE_PROCEEDS_IN_CASEMAN",
            "CLAIM_STAYED,DEFENDANT_RESPONSE",
            "AWAITING_CASE_NOTIFICATION,NOTIFY_DEFENDANT_OF_CLAIM_DETAILS",
            "AWAITING_CASE_DETAILS_NOTIFICATION,ACKNOWLEDGE_SERVICE",
            "FULL_DEFENCE,ACKNOWLEDGE_SERVICE"
        })
        void shouldReturnFalse_whenEventIsNotAllowedAtGivenState(FlowState.Main flowState, CaseEvent caseEvent) {
            assertFalse(flowStateAllowedEventService.isAllowedOnState(flowState.fullName(), caseEvent));
        }
    }

    static class GetAllowedStatesForCaseEventArguments implements ArgumentsProvider {

        @Override
        @SneakyThrows
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                of(CREATE_CLAIM, new String[]{DRAFT.fullName()}),
                of(RESUBMIT_CLAIM, new String[]{PAYMENT_FAILED.fullName()}),
                of(MOVE_TO_STAYED, new String[]{CLAIM_ISSUED.fullName()}),
                of(ACKNOWLEDGE_SERVICE, new String[]{CLAIM_ISSUED.fullName()}),
                of(NOTIFY_DEFENDANT_OF_CLAIM, new String[]{AWAITING_CASE_NOTIFICATION.fullName()}),
                of(CLAIMANT_RESPONSE, new String[]{RESPONDED_TO_CLAIM.fullName()}),
                of(DEFENDANT_RESPONSE, new String[]{SERVICE_ACKNOWLEDGED.fullName()}),
                of(
                    WITHDRAW_CLAIM,
                    new String[]{DRAFT.fullName(), CLAIM_ISSUED.fullName(), CLAIM_STAYED.fullName(),
                        SERVICE_ACKNOWLEDGED.fullName(), PAYMENT_FAILED.fullName(), RESPONDED_TO_CLAIM.fullName(),
                        FULL_DEFENCE.fullName()
                    }
                ),
                of(
                    DISCONTINUE_CLAIM,
                    new String[]{DRAFT.fullName(), CLAIM_ISSUED.fullName(), CLAIM_STAYED.fullName(),
                        SERVICE_ACKNOWLEDGED.fullName(), PAYMENT_FAILED.fullName(),
                        RESPONDED_TO_CLAIM.fullName(), FULL_DEFENCE.fullName()
                    }
                ),
                of(
                    CASE_PROCEEDS_IN_CASEMAN,
                    new String[]{AWAITING_CASE_NOTIFICATION.fullName(), AWAITING_CASE_DETAILS_NOTIFICATION.fullName(),
                        CLAIM_ISSUED.fullName(), SERVICE_ACKNOWLEDGED.fullName(), RESPONDED_TO_CLAIM.fullName(),
                        FULL_DEFENCE.fullName()
                    }
                ),
                of(
                    ADD_DEFENDANT_LITIGATION_FRIEND,
                    new String[]{AWAITING_CASE_NOTIFICATION.fullName(), AWAITING_CASE_DETAILS_NOTIFICATION.fullName(),
                        CLAIM_ISSUED.fullName(), SERVICE_ACKNOWLEDGED.fullName(), RESPONDED_TO_CLAIM.fullName(),
                        FULL_DEFENCE.fullName()
                    }
                ),
                of(
                    ADD_OR_AMEND_CLAIM_DOCUMENTS,
                    new String[]{AWAITING_CASE_NOTIFICATION.fullName(), AWAITING_CASE_DETAILS_NOTIFICATION.fullName()}
                ),
                of(NOTIFY_DEFENDANT_OF_CLAIM_DETAILS, new String[]{AWAITING_CASE_DETAILS_NOTIFICATION.fullName()})
            );
        }
    }

    @Nested
    class GetAllowedStatesForCaseEvent {

        @ParameterizedTest
        @ArgumentsSource(GetAllowedStatesForCaseEventArguments.class)
        void shouldReturnValidStates_whenCaseEventIsGiven(CaseEvent caseEvent, String... flowStates) {
            assertThat(flowStateAllowedEventService.getAllowedStates(caseEvent))
                .containsExactlyInAnyOrder(flowStates);
        }
    }

    static class GetAllowedStatesForCaseDetailsArguments implements ArgumentsProvider {

        @Override
        @SneakyThrows
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                of(true, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), WITHDRAW_CLAIM),
                of(true, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), DEFENDANT_RESPONSE),
                of(true, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), DISCONTINUE_CLAIM),
                of(true, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), CASE_PROCEEDS_IN_CASEMAN),
                of(false, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), CREATE_CLAIM),
                of(false, CaseDetailsBuilder.builder().atStateServiceAcknowledge().build(), CLAIMANT_RESPONSE),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseNotification().build(),
                    NOTIFY_DEFENDANT_OF_CLAIM
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseNotification().build(),
                    ADD_DEFENDANT_LITIGATION_FRIEND
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseNotification().build(),
                    CASE_PROCEEDS_IN_CASEMAN
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseNotification().build(),
                    ADD_OR_AMEND_CLAIM_DOCUMENTS
                ),
                of(
                    false,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseNotification().build(),
                    NOTIFY_DEFENDANT_OF_CLAIM_DETAILS
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    NOTIFY_DEFENDANT_OF_CLAIM_DETAILS
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    ADD_DEFENDANT_LITIGATION_FRIEND
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    CASE_PROCEEDS_IN_CASEMAN
                ),
                of(
                    true,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    ADD_OR_AMEND_CLAIM_DOCUMENTS
                ),
                of(
                    false,
                    CaseDetailsBuilder.builder().atStateAwaitingCaseDetailsNotification().build(),
                    ACKNOWLEDGE_SERVICE
                )
            );
        }
    }

    @Nested
    class IsEventAllowedOnCaseDetails {

        @ParameterizedTest
        @ArgumentsSource(GetAllowedStatesForCaseDetailsArguments.class)
        void shouldReturnValidStates_whenCaseEventIsGiven(
            boolean expected,
            CaseDetails caseDetails,
            CaseEvent caseEvent
        ) {
            assertThat(flowStateAllowedEventService.isAllowed(caseDetails, caseEvent))
                .isEqualTo(expected);
        }
    }
}
