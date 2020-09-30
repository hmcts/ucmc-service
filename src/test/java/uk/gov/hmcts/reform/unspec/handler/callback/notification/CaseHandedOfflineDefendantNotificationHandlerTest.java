package uk.gov.hmcts.reform.unspec.handler.callback.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.unspec.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.service.NotificationService;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.unspec.handler.callback.notification.NotificationData.CLAIM_REFERENCE_NUMBER;
import static uk.gov.hmcts.reform.unspec.handler.callback.notification.NotificationData.SOLICITOR_NAME;

@SpringBootTest(classes = {
    CaseHandedOfflineDefendantNotificationHandler.class,
    CaseDetailsConverter.class,
    JacksonAutoConfiguration.class
})
class CaseHandedOfflineDefendantNotificationHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private NotificationService notificationService;
    @MockBean
    private NotificationsProperties notificationsProperties;
    @Autowired
    private CaseHandedOfflineDefendantNotificationHandler handler;

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(notificationsProperties.getSolicitorResponseToCase()).thenReturn("template-id");
        }

        @Test
        void shouldNotifyParties_whenInvoked() {
            String claimantReference = "Claimant ref";
            String defendantReference = "Claimant ref";
            String legacyCaseReference = "000LR001";
            Map<String, Object> data = Map.of(
                "solicitorReferences", Map.of(
                    "applicantSolicitor1Reference", claimantReference,
                    "respondentSolicitor1Reference", defendantReference),
                "legacyCaseReference", legacyCaseReference);

            CallbackParams params = callbackParamsOf(data, CallbackType.ABOUT_TO_SUBMIT);

            handler.handle(params);

            verify(notificationService).sendMail(
                "defendant-solicitor@example.com",
                "template-id",
                Map.of(CLAIM_REFERENCE_NUMBER, legacyCaseReference, SOLICITOR_NAME, defendantReference),
                "case-handed-offline-defendant-notification-000LR001"
            );
        }
    }
}
