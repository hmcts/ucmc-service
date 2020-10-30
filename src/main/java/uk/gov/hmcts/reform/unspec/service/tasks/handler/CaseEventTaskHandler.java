package uk.gov.hmcts.reform.unspec.service.tasks.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.BusinessProcess;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.service.CoreCaseDataService;
import uk.gov.hmcts.reform.unspec.service.data.ExternalTaskInput;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CaseEventTaskHandler implements ExternalTaskHandler {

    private final CoreCaseDataService coreCaseDataService;
    private final CaseDetailsConverter caseDetailsConverter;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        updateBusinessProcessActivityId(externalTask);
        externalTaskService.complete(externalTask);
    }

    private void updateBusinessProcessActivityId(ExternalTask externalTask) {
        ExternalTaskInput externalTaskInput = objectMapper.convertValue(externalTask.getAllVariables(),
                                                                        ExternalTaskInput.class);
        String caseId = externalTaskInput.getCaseId();
        CaseEvent caseEvent = externalTaskInput.getCaseEvent();
        StartEventResponse startEventResponse = coreCaseDataService.startUpdate(caseId, caseEvent);
        CaseData data = caseDetailsConverter.toCaseData(startEventResponse.getCaseDetails());
        BusinessProcess businessProcess = data.getBusinessProcess().updateActivityId(externalTask.getActivityId());
        coreCaseDataService.submitUpdate(caseId, caseDataContent(startEventResponse, businessProcess));
    }

    private CaseDataContent caseDataContent(StartEventResponse startEventResponse, BusinessProcess businessProcess) {
        Map<String, Object> data = startEventResponse.getCaseDetails().getData();
        data.put("businessProcess", businessProcess);

        return CaseDataContent.builder()
            .eventToken(startEventResponse.getToken())
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .data(data)
            .build();
    }
}
