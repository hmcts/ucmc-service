package uk.gov.hmcts.reform.unspec.handler.callback;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CallbackType;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.enums.DefendantResponseType;
import uk.gov.hmcts.reform.unspec.model.BusinessProcess;
import uk.gov.hmcts.reform.unspec.model.Party;
import uk.gov.hmcts.reform.unspec.validation.DateOfBirthValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.DEFENDANT_RESPONSE;
import static uk.gov.hmcts.reform.unspec.enums.DefendantResponseType.FULL_DEFENCE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDateTime;
import static uk.gov.hmcts.reform.unspec.model.BusinessProcessStatus.READY;

@Service
@RequiredArgsConstructor
public class RespondToClaimCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(DEFENDANT_RESPONSE);
    public static final String RESPONDENT = "respondent1";
    public static final String CLAIMANT_RESPONSE_DEADLINE = "applicantSolicitorResponseDeadlineToRespondentSolicitor1";

    private final ObjectMapper mapper;
    private final DateOfBirthValidator dateOfBirthValidator;

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return Map.of(
            CallbackType.ABOUT_TO_START, this::emptyCallbackResponse,
            CallbackType.MID, this::validateDateOfBirth,
            CallbackType.MID_SECONDARY, this::emptyCallbackWorkaround,
            CallbackType.ABOUT_TO_SUBMIT, this::setClaimantResponseDeadline,
            CallbackType.SUBMITTED, this::buildConfirmation
        );
    }

    private CallbackResponse emptyCallbackWorkaround(CallbackParams callbackParams) {
        return AboutToStartOrSubmitCallbackResponse.builder().build();
    }

    private CallbackResponse validateDateOfBirth(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        Party respondent = mapper.convertValue(data.get(RESPONDENT), Party.class);
        List<String> errors = dateOfBirthValidator.validate(respondent);

        return AboutToStartOrSubmitCallbackResponse.builder()
            .errors(errors)
            .build();
    }

    private CallbackResponse setClaimantResponseDeadline(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        //TODO: There will be in separate ticket for response deadline when requirement is confirmed
        LocalDate claimantResponseDeadLine = LocalDate.now();
        data.put(CLAIMANT_RESPONSE_DEADLINE, claimantResponseDeadLine.atTime(16, 0));

        var response = mapper.convertValue(data.get("respondent1ClaimResponseType"), DefendantResponseType.class);
        if (response == FULL_DEFENCE) {
            data.put("businessProcess",
                     BusinessProcess.builder().activityId("DefendantResponseHandling").status(READY).build());
        } else {
            data.put("businessProcess",
                     BusinessProcess.builder().activityId("CaseHandedOfflineHandling").status(READY).build());
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private SubmittedCallbackResponse buildConfirmation(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        LocalDateTime responseDeadline = mapper.convertValue(data.get(CLAIMANT_RESPONSE_DEADLINE), LocalDateTime.class);

        String claimNumber = "TBC";

        String body = format(
            "<br />The claimant has until %s to proceed. We will let you know when they respond.",
            formatLocalDateTime(responseDeadline, DATE)
        );

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(format(
                "# You've submitted your response%n## Claim number: %s",
                claimNumber
            ))
            .confirmationBody(body)
            .build();
    }
}
