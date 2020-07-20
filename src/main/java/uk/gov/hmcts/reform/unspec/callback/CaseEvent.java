package uk.gov.hmcts.reform.unspec.callback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseEvent {
    CREATE_CASE("CREATE_CLAIM"),
    CONFIRM_SERVICE("CONFIRM_SERVICE"),
    MOVE_TO_STAYED("MOVE_TO_STAYED");

    private final String value;
}
