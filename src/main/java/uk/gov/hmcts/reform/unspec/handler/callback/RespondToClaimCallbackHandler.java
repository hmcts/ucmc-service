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
import uk.gov.hmcts.reform.unspec.model.Party;
import uk.gov.hmcts.reform.unspec.validation.groups.DateOfBirthGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.DEFENDANT_RESPONSE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.DATE;
import static uk.gov.hmcts.reform.unspec.helpers.DateFormatHelper.formatLocalDateTime;

@Service
@RequiredArgsConstructor
public class RespondToClaimCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(DEFENDANT_RESPONSE);
    public static final String RESPONDENT = "respondent";
    public static final String CLAIMANT_RESPONSE_DEADLINE = "claimantResponseDeadline";

    private final ObjectMapper mapper;
    private final Validator validator;

    @Override
    protected Map<CallbackType, Callback> callbacks() {
        return Map.of(
            CallbackType.MID, this::validateDateOfBirth,
            CallbackType.ABOUT_TO_SUBMIT, this::setClaimantResponseDeadline,
            CallbackType.SUBMITTED, this::buildConfirmation
        );
    }

    private CallbackResponse validateDateOfBirth(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        Party respondent = mapper.convertValue(data.get(RESPONDENT), Party.class);
        List<String> errors = validator.validate(respondent, DateOfBirthGroup.class).stream()
            .map(ConstraintViolation::getMessage)
            .collect(toList());

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .errors(errors)
            .build();
    }

    private CallbackResponse setClaimantResponseDeadline(CallbackParams callbackParams) {
        Map<String, Object> data = callbackParams.getRequest().getCaseDetails().getData();
        //TODO: There will be in separate ticket for response deadline when requirement is confirmed
        LocalDate claimantResponseDeadLine = LocalDate.now();
        data.put(CLAIMANT_RESPONSE_DEADLINE, claimantResponseDeadLine.atTime(16, 0));

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
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
