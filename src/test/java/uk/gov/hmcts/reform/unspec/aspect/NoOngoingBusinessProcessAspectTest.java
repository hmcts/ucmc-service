package uk.gov.hmcts.reform.unspec.aspect;

import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.enums.BusinessProcessStatus;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.BusinessProcess;
import uk.gov.hmcts.reform.unspec.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDetailsBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.unspec.aspect.NoOngoingBusinessProcessAspect.ERROR_MESSAGE;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.CONFIRM_SERVICE;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.RESPOND_EXTENSION;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.START_BUSINESS_PROCESS;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    NoOngoingBusinessProcessAspect.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class NoOngoingBusinessProcessAspectTest {

    @Autowired
    NoOngoingBusinessProcessAspect aspect;

    @Nested
    class NormalUserEvent {

        @MockBean
        ProceedingJoinPoint proceedingJoinPoint;

        @Test
        @SneakyThrows
        void shouldProceedToMethodInvocation_whenNoOngoingBusinessProcess() {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(RESPOND_EXTENSION.name())
                             .caseDetails(CaseDetailsBuilder.builder().atStateExtensionRequested().build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }

        @SneakyThrows
        @ParameterizedTest
        @NullSource
        @EnumSource(value = BusinessProcessStatus.class, names = "FINISHED", mode = EnumSource.Mode.INCLUDE)
        void shouldProceedToMethodInvocation_whenBusinessProcessStatusIsNullOrFinished(BusinessProcessStatus status) {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(CONFIRM_SERVICE.name())
                             .caseDetails(CaseDetailsBuilder.builder().data(
                                 CaseDataBuilder.builder()
                                     .atStateClaimCreated()
                                     .businessProcess(BusinessProcess.builder().status(status).build())
                                     .build()).build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }

        @SneakyThrows
        @ParameterizedTest
        @EnumSource(value = BusinessProcessStatus.class, names = "FINISHED", mode = EnumSource.Mode.EXCLUDE)
        void shouldNotProceedToMethodInvocation_whenOngoingBusinessProcess(BusinessProcessStatus status) {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder()
                .errors(List.of(ERROR_MESSAGE))
                .build();

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(CONFIRM_SERVICE.name())
                             .caseDetails(CaseDetailsBuilder.builder().data(
                                 CaseDataBuilder.builder()
                                     .atStateClaimCreated()
                                     .businessProcess(BusinessProcess.builder().status(status).build())
                                     .build()).build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint, never()).proceed();
        }

        @SneakyThrows
        @ParameterizedTest
        @EnumSource(value = BusinessProcessStatus.class, names = "FINISHED", mode = EnumSource.Mode.EXCLUDE)
        void shouldProceedToMethodInvocation_whenOngoingBusinessProcessOnSubmittedCallback(BusinessProcessStatus status
        ) {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(SUBMITTED)
                .request(CallbackRequest.builder()
                             .eventId(CONFIRM_SERVICE.name())
                             .caseDetails(CaseDetailsBuilder.builder().data(
                                 CaseDataBuilder.builder()
                                     .atStateClaimCreated()
                                     .businessProcess(BusinessProcess.builder().status(status).build())
                                     .build()).build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }
    }

    @Nested
    class CamundaUserEvent {

        @MockBean
        ProceedingJoinPoint proceedingJoinPoint;

        @Test
        @SneakyThrows
        void shouldProceedToMethodInvocation_whenNoOngoingBusinessProcess() {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(START_BUSINESS_PROCESS.name())
                             .caseDetails(CaseDetailsBuilder.builder().atStateExtensionRequested().build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }

        @SneakyThrows
        @ParameterizedTest
        @NullSource
        @EnumSource(value = BusinessProcessStatus.class, names = "FINISHED", mode = EnumSource.Mode.INCLUDE)
        void shouldProceedToMethodInvocation_whenBusinessProcessStatusIsNullOrFinished(BusinessProcessStatus status) {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder().build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(START_BUSINESS_PROCESS.name())
                             .caseDetails(CaseDetailsBuilder.builder().data(
                                 CaseDataBuilder.builder()
                                     .atStateClaimCreated()
                                     .businessProcess(BusinessProcess.builder().status(status).build())
                                     .build()).build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }

        @SneakyThrows
        @ParameterizedTest
        @EnumSource(value = BusinessProcessStatus.class, names = "FINISHED", mode = EnumSource.Mode.EXCLUDE)
        void shouldProceedToMethodInvocation_whenOngoingBusinessProcess(BusinessProcessStatus status) {
            AboutToStartOrSubmitCallbackResponse response = AboutToStartOrSubmitCallbackResponse.builder()
                .errors(List.of(ERROR_MESSAGE))
                .build();
            when(proceedingJoinPoint.proceed()).thenReturn(response);

            CallbackParams callbackParams = CallbackParamsBuilder.builder()
                .type(ABOUT_TO_START)
                .request(CallbackRequest.builder()
                             .eventId(START_BUSINESS_PROCESS.name())
                             .caseDetails(CaseDetailsBuilder.builder().data(
                                 CaseDataBuilder.builder()
                                     .atStateClaimCreated()
                                     .businessProcess(BusinessProcess.builder().status(status).build())
                                     .build()).build())
                             .build())
                .build();
            Object result = aspect.checkOngoingBusinessProcess(proceedingJoinPoint, callbackParams);

            assertThat(result).isEqualTo(response);
            verify(proceedingJoinPoint).proceed();
        }
    }
}
