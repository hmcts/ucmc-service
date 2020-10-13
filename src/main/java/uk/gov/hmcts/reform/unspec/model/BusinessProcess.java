package uk.gov.hmcts.reform.unspec.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.unspec.enums.BusinessProcessStatus;

import static java.util.Optional.ofNullable;

@Data
@Builder
public class BusinessProcess {

    private String processInstanceId;
    private BusinessProcessStatus status;
    private String activityId;

    @JsonIgnore
    public boolean hasSimilarProcessInstanceId(String processInstanceId) {
        return this.getProcessInstanceId().equals(processInstanceId);
    }

    @JsonIgnore
    public BusinessProcessStatus getStatusOrDefault() {
        return ofNullable(this.getStatus()).orElse(BusinessProcessStatus.READY);
    }

    @JsonIgnore
    public BusinessProcess start() {
        this.status = BusinessProcessStatus.STARTED;
        this.activityId = null;
        return this;
    }

    @JsonIgnore
    public BusinessProcess updateProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @JsonIgnore
    public BusinessProcess updateActivityId(String activityId) {
        this.activityId = activityId;
        return this;
    }
}
