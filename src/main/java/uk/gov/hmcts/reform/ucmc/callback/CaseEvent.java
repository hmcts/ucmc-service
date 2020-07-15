package uk.gov.hmcts.reform.ucmc.callback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseEvent {
    CREATE_CASE("CREATE_CLAIM"),
    REQUEST_EXTENSION("REQUEST_EXTENSION");

    private final String value;
}
