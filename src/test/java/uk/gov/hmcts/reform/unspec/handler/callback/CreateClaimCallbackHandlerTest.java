package uk.gov.hmcts.reform.unspec.handler.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.config.ClaimIssueConfiguration;
import uk.gov.hmcts.reform.unspec.config.MockDatabaseConfiguration;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.model.Party;
import uk.gov.hmcts.reform.unspec.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDetailsBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.PartyBuilder;
import uk.gov.hmcts.reform.unspec.service.flowstate.StateFlowEngine;
import uk.gov.hmcts.reform.unspec.validation.DateOfBirthValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.MID;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CREATE_CLAIM;
import static uk.gov.hmcts.reform.unspec.enums.AllocatedTrack.MULTI_CLAIM;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.unspec.enums.CaseState.PROCEEDS_WITH_OFFLINE_JOURNEY;
import static uk.gov.hmcts.reform.unspec.enums.YesOrNo.NO;
import static uk.gov.hmcts.reform.unspec.handler.callback.CreateClaimCallbackHandler.CONFIRMATION_SUMMARY;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE_TIME_AT;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDateTime;
import static uk.gov.hmcts.reform.unspec.utils.PartyUtils.getPartyNameBasedOnType;

@SpringBootTest(classes = {
    CreateClaimCallbackHandler.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class,
    ClaimIssueConfiguration.class,
    MockDatabaseConfiguration.class,
    ValidationAutoConfiguration.class,
    DateOfBirthValidator.class,
    StateFlowEngine.class},
    properties = {"reference.database.enabled=false"})
class CreateClaimCallbackHandlerTest extends BaseCallbackHandlerTest {

    public static final String REFERENCE_NUMBER = "000LR001";

    @Autowired
    private CreateClaimCallbackHandler handler;

    @Value("${unspecified.response-pack-url}")
    private String responsePackLink;

    @Nested
    class AboutToStartCallback {

        @Test
        void shouldReturnNoError_WhenAboutToStartIsInvoked() {
            CaseDetails caseDetails = CaseDetailsBuilder.builder().atStateClaimDraft().build();
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_START, caseDetails).build();

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).isNull();
        }
    }

    @Nested
    class MidEventApplicantCallback {

        private static final String PAGE_ID = "applicant";

        @Test
        void shouldReturnError_whenIndividualDateOfBirthIsInTheFuture() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft()
                .applicant1(PartyBuilder.builder().individual()
                                .individualDateOfBirth(LocalDate.now().plusDays(1))
                                .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).containsExactly("The date entered cannot be in the future");
        }

        @Test
        void shouldReturnError_whenSoleTraderDateOfBirthIsInTheFuture() {
            CaseData caseData = CaseDataBuilder.builder().atStateServiceConfirmed()
                .applicant1(PartyBuilder.builder().individual()
                                .soleTraderDateOfBirth(LocalDate.now().plusDays(1))
                                .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).containsExactly("The date entered cannot be in the future");
        }

        @Test
        void shouldReturnNoError_whenIndividualDateOfBirthIsInThePast() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft()
                .applicant1(PartyBuilder.builder().individual()
                                .individualDateOfBirth(LocalDate.now().minusDays(1))
                                .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }

        @Test
        void shouldReturnNoError_whenSoleTraderDateOfBirthIsInThePast() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimDraft()
                .applicant1(PartyBuilder.builder().individual()
                                .soleTraderDateOfBirth(LocalDate.now().minusDays(1))
                                .build())
                .build();
            CallbackParams params = callbackParamsOf(caseData, MID, PAGE_ID);

            var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class AboutToSubmitCallback {

        private CallbackParams params;
        private CaseData caseData;

        @Nested
        class Respondent1DoesNotHaveLegalRepresentation {

            @BeforeEach
            void setup() {
                caseData = CaseDataBuilder.builder().atStateClaimDraft().respondent1Represented(NO).build();
                params = CallbackParamsBuilder.builder().of(CallbackType.ABOUT_TO_SUBMIT, caseData).build();
            }

            @Test
            void shouldSetStateAsProceedsWithOfflineJourney_whenRespondentIsNotRepresented() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getState()).isEqualTo(PROCEEDS_WITH_OFFLINE_JOURNEY.toString());
            }
        }

        @Nested
        class Respondent1HasLegalRepresentation {

            @BeforeEach
            void setup() {
                caseData = CaseDataBuilder.builder().atStateClaimDraft().build();
                params = CallbackParamsBuilder.builder().of(CallbackType.ABOUT_TO_SUBMIT, caseData).build();
            }

            @Test
            void shouldAddClaimIssuedDateAndSubmittedAt_whenInvoked() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData()).containsEntry("legacyCaseReference", REFERENCE_NUMBER);
                assertThat(response.getData()).containsKey("claimSubmittedDateTime");
            }

            @Test
            void shouldAddAllocatedTrack_whenInvoked() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData()).containsEntry("allocatedTrack", MULTI_CLAIM.name());
            }

            @Test
            void shouldUpdateRespondentAndApplicantWithPartyNameAndPartyTypeDisplayValue_whenInvoked() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                Party respondent1 = caseData.getRespondent1();
                Party applicant1 = caseData.getApplicant1();

                assertThat(response.getData())
                    .extracting("respondent1")
                    .extracting("partyName", "partyTypeDisplayValue")
                    .containsExactly(getPartyNameBasedOnType(respondent1), respondent1.getType().getDisplayValue());

                assertThat(response.getData())
                    .extracting("applicant1")
                    .extracting("partyName", "partyTypeDisplayValue")
                    .containsExactly(getPartyNameBasedOnType(applicant1), applicant1.getType().getDisplayValue());
            }

            @Test
            void shouldUpdateBusinessProcess_whenInvoked() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getData())
                    .extracting("businessProcess")
                    .extracting("camundaEvent", "status")
                    .containsOnly(CREATE_CLAIM.name(), "READY");
            }

            @Test
            void shouldSetStateAsPendingCaseIssued_whenRespondentIsRepresented() {
                var response = (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

                assertThat(response.getState()).isEqualTo(PENDING_CASE_ISSUED.toString());
            }
        }
    }

    @Nested
    class SubmittedCallback {

        @Nested
        class Respondent1DoesNotHaveLegalRepresentation {

            @Test
            void shouldReturnExpectedSubmittedCallbackResponse_whenRespondent1DoesNotHaveRepresentation() {
                CaseData caseData = CaseDataBuilder.builder().atStateClaimCreated().respondent1Represented(NO).build();
                CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
                SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

                assertThat(response).usingRecursiveComparison().isEqualTo(
                    SubmittedCallbackResponse.builder()
                        .confirmationHeader("Your claim will now progress offline")
                        .confirmationBody("You do not need to do anything")
                        .build());
            }
        }

        @Nested
        class Respondent1HasLegalRepresentation {

            @Test
            void shouldReturnExpectedSubmittedCallbackResponse_whenRespondent1HasRepresentation() {
                CaseData caseData = CaseDataBuilder.builder().atStateClaimCreated().build();
                CallbackParams params = callbackParamsOf(caseData, SUBMITTED);
                SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

                LocalDateTime serviceDeadline = now().plusDays(112).atTime(23, 59);

                String body = format(
                    CONFIRMATION_SUMMARY,
                    format("/cases/case-details/%s#CaseDocuments", CASE_ID),
                    responsePackLink,
                    formatLocalDateTime(serviceDeadline, DATE_TIME_AT)
                );

                assertThat(response).usingRecursiveComparison().isEqualTo(
                    SubmittedCallbackResponse.builder()
                        .confirmationHeader(format(
                            "# Your claim has been issued%n## Claim number: %s",
                            REFERENCE_NUMBER
                        ))
                        .confirmationBody(body)
                        .build());
            }
        }
    }
}
