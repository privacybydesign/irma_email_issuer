package foundation.privacybydesign.email;

import javax.mail.internet.AddressException;

/**
 * Created by ayke on 19-6-17.
 */
public class EmailProvider {
    public static void main(String[] args) throws AddressException {
        System.out.println("Sending test email...");
        EmailSender.send(EmailConfiguration.getInstance().getMailFrom(), "test email", "Test " + "message");
        System.out.println("Done.");
    }
}