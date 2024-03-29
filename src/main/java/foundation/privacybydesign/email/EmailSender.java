package foundation.privacybydesign.email;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

/**
 * Simple class to send emails. Mail host/port/auth is configured in
 * EmailConfiguration.
 *
 * TODO: join this with org.irmacard.keyshare.web.email.EmailSender
 */
public class EmailSender {

    /**
     * Send an email using a configured SMTP server.
     *
     * @param toAddresses Email 'To' address - where to send the mail to
     * @param subject Email subject
     * @param body Email text body
     * @throws AddressException
     */
    public static void send(String toAddresses, String subject, String body) throws AddressException, MessagingException {
        send(toAddresses, subject, body, null, false);
    }

    public static void send(String toAddresses, String subject, String body, String replyto, boolean html, Object... o) throws AddressException, MessagingException{
        InternetAddress[] addresses = InternetAddress.parse(toAddresses);
        if (addresses.length != 1)
            throw new AddressException("Invalid amount of (comma-separated) addresses given (should be 1)");

        Properties props = new Properties();
        // Require STARTTLS
        // For that, only mail.smtp.starttls.required has to be set:
        // https://github.com/javaee/javamail/blob/master/mail/src/main/java/com/sun/mail/smtp/SMTPTransport.java#L734
        //props.put("mail.smtp.starttls.enabled", "true");
        props.put("mail.smtp.starttls.required", EmailConfiguration.getInstance().getStarttlsRequired());
        props.put("mail.smtp.host", EmailConfiguration.getInstance().getMailHost());
        props.put("mail.smtp.port", EmailConfiguration.getInstance().getMailPort());

        Session session;
        if (EmailConfiguration.getInstance().getMailUser().length() > 0) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.checkserveridentity", "true");
            
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EmailConfiguration.getInstance().getMailUser(),
                            EmailConfiguration.getInstance().getMailPassword());
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EmailConfiguration.getInstance().getMailFrom()));
        message.setRecipients(Message.RecipientType.TO, addresses);
        message.setSubject(subject);
        if (replyto != null && replyto.length() > 0)
            message.setReplyTo(new Address[]{new InternetAddress(replyto)});

        if (o != null && o.length > 0)
            body = String.format(body, o);

        if (html) {
            message.setContent(body, "text/html");
        } else {
            message.setText(body);
        }

        Transport.send(message);
    }
}
