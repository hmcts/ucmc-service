package uk.gov.hmcts.reform.unspec.model.dq;

import uk.gov.hmcts.reform.unspec.model.StatementOfTruth;
import uk.gov.hmcts.reform.unspec.model.documents.Document;

interface DQ {

    FileDirectionsQuestionnaire getFileDirectionQuestionnaire();

    DisclosureOfElectronicDocuments getDisclosureOfElectronicDocuments();

    String getDisclosureOfNonElectronicDocuments();

    DisclosureReport getDisclosureReport();

    Experts getExperts();

    Witnesses getWitnesses();

    Hearing getHearing();

    Document getDraftDirections();

    RequestedCourt getRequestedCourt();

    HearingSupport getHearingSupport();

    FurtherInformation getFurtherInformation();

    StatementOfTruth getStatementOfTruth();
}
