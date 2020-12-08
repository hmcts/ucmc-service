package uk.gov.hmcts.reform.unspec.model.robotics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LitigiousParty {

    @JsonProperty("ID")
    private String id;
    private String type;
    private String name;
    private RoboticsAddresses addresses;
    private String contactDX;
    private String contactTelephoneNumber;
    private String contactFaxNumber;
    private String contactEmailAddress;
    private String preferredMethodOfCommunication;
    private String welshTranslation;
    private String reference;
    private String dateOfService;
    private String lastDateForService;
    private String dateOfBirth;
    private String solicitorOrganisationID;
}
