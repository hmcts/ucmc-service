package uk.gov.hmcts.reform.ucmc.enums;

import uk.gov.hmcts.reform.ucmc.model.ClaimValue;

public enum AllocatedTrack {
    SMALL_CLAIM,
    FAST_CLAIM,
    MULTI_CLAIM;

    //TODO: logic for when no claim value -> checking with Darren and Luke
    public static AllocatedTrack getAllocatedTrack(ClaimValue claimValue, ClaimType claimType) {
        if (claimType.isPersonalInjury()) {
            if (claimValue.getHigherValue() < 1000) {
                return SMALL_CLAIM;
            } else if (claimValue.getHigherValue() >= 1000 && claimValue.getHigherValue() < 25000) {
                return FAST_CLAIM;
            }
        }

        if (claimValue.getHigherValue() < 10000) {
            return SMALL_CLAIM;
        } else if (claimValue.getHigherValue() >= 10000 && claimValue.getHigherValue() < 25000) {
            return FAST_CLAIM;
        } else {
            return MULTI_CLAIM;
        }
    }
}
