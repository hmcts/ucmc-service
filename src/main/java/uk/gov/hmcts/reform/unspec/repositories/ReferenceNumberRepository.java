package uk.gov.hmcts.reform.unspec.repositories;

import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface ReferenceNumberRepository {

    @SqlQuery("SELECT next_legal_rep_reference_number()")
    String getReferenceNumber();

}

