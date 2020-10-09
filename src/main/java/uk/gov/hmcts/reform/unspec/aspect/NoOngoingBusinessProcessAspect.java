package uk.gov.hmcts.reform.unspec.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.callback.UserType;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;

import java.util.List;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.unspec.enums.BusinessProcessStatus.FINISHED;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NoOngoingBusinessProcessAspect {

    public static final String ERROR_MESSAGE = "There is a technical issue causing a delay. "
        + "You do not need to do anything. Please come back later.";

    private final CaseDetailsConverter caseDetailsConverter;

    @Around("execution(* *(*)) && @annotation(NoOngoingBusinessProcess) && args(callbackParams))")
    public Object checkOngoingBusinessProcess(
        ProceedingJoinPoint joinPoint,
        CallbackParams callbackParams
    ) throws Throwable {
        CaseEvent caseEvent = CaseEvent.valueOf(callbackParams.getRequest().getEventId());
        CaseDetails caseDetails = callbackParams.getRequest().getCaseDetails();
        CaseData caseData = caseDetailsConverter.toCaseData(caseDetails);
        if (hasNoOngoingBusinessProcess(caseData) || caseEvent.getUserType() == UserType.CAMUNDA) {
            return joinPoint.proceed();
        } else {
            log.info(format(
                "%s is not allowed on the case %s due to ongoing business process",
                caseEvent.getDisplayName(), caseData.getCcdCaseReference()
            ));
            return AboutToStartOrSubmitCallbackResponse.builder()
                .errors(List.of(ERROR_MESSAGE))
                .build();
        }
    }

    private boolean hasNoOngoingBusinessProcess(CaseData caseData) {
        return (caseData.getBusinessProcess() == null
            || caseData.getBusinessProcess().getStatus() == null
            || caseData.getBusinessProcess().getStatus() == FINISHED);
    }
}
