package uk.gov.hmcts.reform.ucmc.controllers;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.ccd.client.model.SubmittedCallbackResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.ucmc.helpers.DateFormatHelper.DATE_TIME_AT;
import static uk.gov.hmcts.reform.ucmc.helpers.DateFormatHelper.formatLocalDateTime;

@Api
@RestController
@RequestMapping("/create-claim")
public class CreateClaimController {

    @PostMapping("/submitted")
    public SubmittedCallbackResponse handleSubmitted() {
        String documentLink = "https://www.google.com";
        String responsePackLink = "https://formfinder.hmctsformfinder.justice.gov.uk/n9-eng.pdf";
        LocalDateTime serviceDeadline = LocalDate.now().plusDays(112).atTime(23, 59);
        String formattedServiceDeadline = formatLocalDateTime(serviceDeadline, DATE_TIME_AT);
        String claimNumber = "TBC";

        String body = format(
            "<br />Follow these steps to serve a claim:"
                + "\n* [Download the sealed claim form](%s) (PDF, 123KB)"
                + "\n* Send the form, particulars of claim and [a response pack](%s) (PDF, 266 KB) "
                + "to the defendant by %s"
                + "\n* Confirm service online within 21 days of sending the form, particulars and response pack, before"
                + " 4pm if you're doing this on the due day", documentLink, responsePackLink, formattedServiceDeadline);

        return SubmittedCallbackResponse.builder()
            .confirmationHeader(format("# Your claim has been issued\n## Claim number: %s", claimNumber))
            .confirmationBody(body)
            .build();
    }
}
