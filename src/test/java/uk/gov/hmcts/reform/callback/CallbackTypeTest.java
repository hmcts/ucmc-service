package uk.gov.hmcts.reform.callback;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.ucmc.callback.CallbackException;
import uk.gov.hmcts.reform.ucmc.callback.CallbackType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CallbackTypeTest {

    @Test
    public void shouldDeserialiseValidCallbacks() {
        assertThat(CallbackType.fromValue("mid")).isEqualTo(CallbackType.MID);
        assertThat(CallbackType.fromValue("about-to-start"))
            .isEqualTo(CallbackType.ABOUT_TO_START);
        assertThat(CallbackType.fromValue("about-to-submit"))
            .isEqualTo(CallbackType.ABOUT_TO_SUBMIT);
        assertThat(CallbackType.fromValue("submitted"))
            .isEqualTo(CallbackType.SUBMITTED);
    }

    @Test
    public void shouldThrowIfUnknownCallback() {
        assertThrows(CallbackException.class, () -> CallbackType.fromValue("nope"));
    }
}
