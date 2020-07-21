package uk.gov.hmcts.reform.unspec.utils;

import org.apache.commons.lang.StringUtils;
import uk.gov.hmcts.reform.unspec.model.Party;

public class PartyNameUtils {

    private PartyNameUtils() {
        //NO-OP
    }

    public static String getPartyNameBasedOnType(Party party) {
        switch (party.getType()) {
            case COMPANY:
                return party.getCompanyName();
            case INDIVIDUAL:
                return
                    getTitle(party.getIndividualTitle())
                        + party.getIndividualFirstName()
                        + " "
                        + party.getIndividualLastName();
            case SOLE_TRADER:
                return getTitle(party.getSoleTraderTitle())
                    + party.getSoleTraderFirstName()
                    + " "
                    + party.getSoleTraderLastName();
            case ORGANISATION:
                return party.getOrganisationName();
            default:
                throw new IllegalArgumentException("Invalid Party Type " + party.getType());

        }
    }

    private static String getTitle(String title) {
        return StringUtils.isBlank(title) ? "" : title + " ";
    }
}
