package uk.gov.hmcts.reform.unspec.handler;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.enums.ServedDocuments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ConfirmServiceCallbackHandler.class, JacksonAutoConfiguration.class})
class ConfirmServiceCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private ConfirmServiceCallbackHandler handler;

    @Nested
    class AboutToStartCallback {

        @Test
        void shouldPrepopulateServedDocumentsList_whenAboutToStartEvent() {
            CallbackParams params = callbackParamsOf(new HashMap<>(), CallbackType.ABOUT_TO_START);
            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getData())
                .isEqualTo(Map.of("servedDocuments", List.of(ServedDocuments.CLAIM_FORM)));
        }
    }

    @Nested
    class MidEventCallback {

        @Test
        void shouldReturnError_whenWhitespaceInServedDocumentsOther() {
            Map<String, Object> data = new HashMap<>();
            data.put("servedDocumentsOther", " ");

            CallbackParams params = callbackParamsOf(data, CallbackType.MID);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getErrors()).containsExactly(
                "CONTENT TBC: please enter a valid value for other documents");
        }

        @Test
        void shouldReturnNoError_whenValidServedDocumentsOther() {
            Map<String, Object> data = new HashMap<>();
            data.put("servedDocumentsOther", "A valid document");

            CallbackParams params = callbackParamsOf(data, CallbackType.MID);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getData()).isEqualTo(data);
            assertThat(response.getErrors()).isEmpty();
        }
    }

    @Nested
    class AboutToSubmitCallback {

        @Test
        void shouldReturnExpectedAboutToSubmitCallbackResponse_whenValidData() {
            Map<String, Object> data = new HashMap<>();
            data.put("serviceMethod", "POST");
            data.put("serviceDate", "2099-06-23");

            CallbackParams params = callbackParamsOf(data, CallbackType.ABOUT_TO_SUBMIT);

            AboutToStartOrSubmitCallbackResponse response = (AboutToStartOrSubmitCallbackResponse) handler
                .handle(params);

            assertThat(response.getData()).isEqualTo(
                Map.of(
                    "deemedDateOfService", LocalDate.of(2099, 6, 25),
                    "responseDeadline", LocalDateTime.of(2099, 7, 9, 16, 0),
                    "serviceMethod", "POST",
                    "serviceDate", "2099-06-23"
                ));
        }
    }

    @Nested
    class SubmittedCallback {

        @Test
        void shouldReturnExpectedSubmittedCallbackResponse_whenValidData() {
            Map<String, Object> data = new HashMap<>();
            data.put("deemedDateOfService", "2099-06-25");

            CallbackParams params = callbackParamsOf(data, CallbackType.SUBMITTED);

            SubmittedCallbackResponse response = (SubmittedCallbackResponse) handler.handle(params);

            assertThat(response).isEqualToComparingFieldByField(
                SubmittedCallbackResponse.builder()
                    .confirmationHeader("# You've confirmed service")
                    .confirmationBody("<br /> Deemed date of service: 25 June 2099."
                                          + "<br />The defendant must respond before 4:00pm on 9 July 2099."
                                          + "\n\n[Download certificate of service](http://www.google.com) (PDF, 266 KB)")
                    .build());
        }
    }
}
