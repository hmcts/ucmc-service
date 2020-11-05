package uk.gov.hmcts.reform.unspec.sendgrid;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendGridClient {

    private final SendGrid sendGrid;

    @Retryable(value = EmailSendFailedException.class, backoff = @Backoff(delay = 100, maxDelay = 500))
    public void sendEmail(String from, EmailData emailData) {
        verifyData(from, emailData);
        try {
            Email sender = new Email(from);
            String subject = emailData.getSubject();
            Email recipient = new Email(emailData.getTo());
            Content content = new Content(MediaType.TEXT_PLAIN_VALUE, getMessage(emailData));
            Mail mail = new Mail(sender, subject, recipient, content);
            if (emailData.hasAttachments()) {
                emailData.getAttachments().stream()
                    .map(SendGridClient::toSendGridAttachments)
                    .forEach(mail::addAttachments);
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (!HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful()) {
                throw new EmailSendFailedException(new HttpException(String.format(
                    "SendGrid returned a non-success response (%d); body: %s",
                    response.getStatusCode(),
                    response.getBody()
                )));
            }
        } catch (IOException exception) {
            throw new EmailSendFailedException(exception);
        }
    }

    @SuppressWarnings("unused")
    @Recover
    public void logSendMessageWithAttachmentFailure(
        EmailSendFailedException exception,
        String from,
        EmailData emailData
    ) {
        String errorMessage = String.format(
            "sendEmail failure:  failed to send email with details: %s due to %s",
            emailData.toString(), exception.getMessage()
        );
        log.error(errorMessage, exception);
    }

    private static String getMessage(EmailData emailData) {
        // SendGrid will not allow empty messages, but it's fine with blank messages.
        String message = emailData.getMessage();
        if (message == null || message.isBlank()) {
            message = " ";
        }
        return message;
    }

    private static Attachments toSendGridAttachments(EmailAttachment attachment) {
        try {
            Attachments.Builder builder = new Attachments.Builder(
                attachment.getFilename(),
                attachment.getData().getInputStream()
            );
            builder.withType(attachment.getContentType());
            builder.withDisposition("attachment");
            return builder.build();
        } catch (IOException e) {
            throw new EmailSendFailedException(
                "Could not open input stream for attachment " + attachment.getFilename(),
                e
            );
        }
    }

    private void verifyData(String from, EmailData emailData) {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("from cannot be null or blank");
        }
        if (emailData == null) {
            throw new IllegalArgumentException("emailData cannot be null");
        }
        String to = emailData.getTo();
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("emailData.to cannot be null or blank");
        }
        String subject = emailData.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("emailData.subject cannot be null or blank");
        }
    }
}
