package uk.gov.hmcts.reform.unspec.testingsupport;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.CaseUser;
import uk.gov.hmcts.reform.unspec.service.CoreCaseUserService;

import java.util.Set;
import javax.validation.constraints.NotNull;

@Api
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(
    path = "/testing-support",
    produces = MediaType.APPLICATION_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE
)
public class AssignCaseSupportController {

    private final CoreCaseUserService coreCaseUserService;

    @PostMapping("/assign-case/{caseId}")
    @ApiOperation("Assign case to defendant")
    public void assignCase(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorisation,
                           @PathVariable("caseId") String caseId) {
        coreCaseUserService.assignCase(caseId, authorisation);
    }
}
