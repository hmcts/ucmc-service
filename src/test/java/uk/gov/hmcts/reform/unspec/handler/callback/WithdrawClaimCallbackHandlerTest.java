package uk.gov.hmcts.reform.unspec.handler.callback;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CloseClaim;

import java.time.LocalDate;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.MID;

@SpringBootTest(classes = {
    WithdrawClaimCallbackHandler.class,
    ValidationAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    CaseDetailsConverter.class
})
class WithdrawClaimCallbackHandlerTest extends BaseCallbackHandlerTest {

    @Autowired
    private WithdrawClaimCallbackHandler handler;

    @Nested
    class MidEventWithdrawalReasonCallback {

        private static final String PAGE_ID = "withdrawal-reason";

        @Test
        void shouldReturnErrors_whenDateInFuture() {
            HashMap<String, Object> data = new HashMap<>();
            data.put("withdrawClaim", CloseClaim.builder().date(LocalDate.now().plusDays(1)).build());
            CallbackParams params = callbackParamsOf(data, MID, PAGE_ID);

            AboutToStartOrSubmitCallbackResponse response =
                (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).containsOnly("The date must not be in the future");
        }

        @Test
        void shouldReturnNoErrors_whenDateInPast() {
            HashMap<String, Object> data = new HashMap<>();
            data.put("withdrawClaim", CloseClaim.builder().date(LocalDate.now().minusDays(1)).build());
            CallbackParams params = callbackParamsOf(data, MID, PAGE_ID);

            AboutToStartOrSubmitCallbackResponse response =
                (AboutToStartOrSubmitCallbackResponse) handler.handle(params);

            assertThat(response.getErrors()).isEmpty();
        }
    }
}
