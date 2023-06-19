package foundation.privacybydesign.email;

import foundation.privacybydesign.common.email.EmailTokens;

import jakarta.mail.internet.AddressException;

/**
 * Test console application. Quite useless now, but was useful while writing
 * this application.
 */
public class EmailProvider {
    public static void main(String[] args) throws AddressException {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        // Test email with signature
        EmailTokens signer = new EmailTokens(conf.getSecretKey(), conf.getEmailTokenValidity());
        String token = signer.createToken(conf.getMailFrom());

        String mailBody = String.format(conf.getVerifyEmailBody("en"),
                "#verify-email/" + token);

        System.out.println("Sending test email...");
        EmailSender.send(conf.getMailFrom(), "mail verification", mailBody);
        System.out.println("Done.");
    }
}
