package uk.gov.hmcts.reform.unspec.handler.callback.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.unspec.handler.callback.BaseCallbackHandlerTest;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.sampledata.CallbackParamsBuilder;
import uk.gov.hmcts.reform.unspec.sampledata.CaseDataBuilder;
import uk.gov.hmcts.reform.unspec.service.NotificationService;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.unspec.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.unspec.handler.callback.notification.NotificationData.CLAIM_REFERENCE_NUMBER;
import static uk.gov.hmcts.reform.unspec.handler.callback.notification.NotificationData.SOLICITOR_REFERENCE;

@SpringBootTest(classes = {
    CaseHandedOfflineClaimantNotificationHandler.class,
    JacksonAutoConfiguration.class
})
class CaseHandedOfflineClaimantNotificationHandlerTest extends BaseCallbackHandlerTest {

    @MockBean
    private NotificationService notificationService;
    @MockBean
    private NotificationsProperties notificationsProperties;
    @Autowired
    private CaseHandedOfflineClaimantNotificationHandler handler;

    @Nested
    class AboutToSubmitCallback {
        @BeforeEach
        void setup() {
            when(notificationsProperties.getSolicitorResponseToCase()).thenReturn("template-id");
            when(notificationsProperties.getClaimantSolicitorEmail()).thenReturn("claimantsolicitor@example.com");
            when(notificationsProperties.getDefendantSolicitorEmail()).thenReturn("defendantsolicitor@example.com");
        }

        @Test
        void shouldNotifyParties_whenInvoked() {
            CaseData caseData = CaseDataBuilder.builder().atStateClaimCreated().build();
            CallbackParams params = CallbackParamsBuilder.builder().of(ABOUT_TO_SUBMIT, caseData).build();

            handler.handle(params);

            verify(notificationService).sendMail(
                "claimantsolicitor@example.com",
                "template-id",
                Map.of(CLAIM_REFERENCE_NUMBER, "000LR001", SOLICITOR_REFERENCE, "claimant solicitor"),
                "case-handed-offline-claimant-notification-000LR001"
            );
        }
    }
}
