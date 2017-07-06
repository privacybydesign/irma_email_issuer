package foundation.privacybydesign.email;

import foundation.privacybydesign.common.email.EmailTokens;

import javax.mail.internet.AddressException;

/**
 * Created by ayke on 19-6-17.
 */
public class EmailProvider {
    public static void main(String[] args) throws AddressException {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        // Test email with signature
        EmailTokens signer = new EmailTokens(conf.getSecretKey(), conf.getEmailTokenValidity());
        String token = signer.createToken(conf.getMailFrom());

        String mailBody = "Add an email address by clicking the following " +
                "link:\n\n  " + conf.getWebclientUrl() + "#verify-email/" +
                token;

        System.out.println("Sending test email...");
        EmailSender.send(conf.getMailFrom(), "mail verification", mailBody);
        System.out.println("Done.");
    }
}