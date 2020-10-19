package uk.gov.hmcts.reform.unspec.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.unspec.utils.MonetaryConversions;

import java.math.BigDecimal;

@Data
@Builder
public class ClaimValue {

    private final BigDecimal statementOfValueInPennies;

    @JsonCreator
    public ClaimValue(@JsonProperty("statementOfValueInPennies") BigDecimal statementOfValueInPennies) {
        this.statementOfValueInPennies = statementOfValueInPennies;
    }

    public BigDecimal toPounds() {
        return MonetaryConversions.penniesToPounds(this.statementOfValueInPennies);
    }

    public String formData() {
        return "up to £" + this.toPounds();
    }
}
