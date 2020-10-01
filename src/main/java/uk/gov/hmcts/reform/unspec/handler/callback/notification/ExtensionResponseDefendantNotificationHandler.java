package uk.gov.hmcts.reform.unspec.handler.callback.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.config.properties.notification.NotificationsProperties;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.service.NotificationService;

import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE;

@Service
@RequiredArgsConstructor
public class ExtensionResponseDefendantNotificationHandler extends CallbackHandler implements NotificationData  {

    private static final List<CaseEvent> EVENTS = List.of(NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE);
    public static final String NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE_TASK_ID =
        "NotifyDefendantSolicitorForExtensionResponse";
    private static final String REFERENCE_TEMPLATE = "extension-response-defendant-notification-%s";

    private final NotificationService notificationService;
    private final NotificationsProperties notificationsProperties;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return Map.of(
            CallbackType.ABOUT_TO_SUBMIT, this::notifyDefendantSolicitorForExtensionResponse
        );
    }

    @Override
    public String camundaActivityId() {
        return NOTIFY_RESPONDENT_SOLICITOR1_FOR_EXTENSION_RESPONSE_TASK_ID;
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse notifyDefendantSolicitorForExtensionResponse(CallbackParams callbackParams) {
        CaseData caseData = caseDetailsConverter.toCaseData(callbackParams.getRequest().getCaseDetails());

        notificationService.sendMail(
            "defendant-solicitor@example.com",
            notificationsProperties.getSolicitorResponseToCase(),
            addProperties(caseData),
            String.format(REFERENCE_TEMPLATE, caseData.getLegacyCaseReference())
        );
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    @Override
    public Map<String, String> addProperties(CaseData caseData) {
        return Map.of(
            CLAIM_REFERENCE_NUMBER, caseData.getLegacyCaseReference(),
            SOLICITOR_REFERENCE, caseData.getSolicitorReferences().getRespondentSolicitor1Reference()
        );
    }
}
