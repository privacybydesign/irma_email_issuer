package foundation.privacybydesign.email;

import javax.mail.internet.AddressException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by ayke on 19-6-17.
 */
@Path("")
public class EmailRestApi {
    private static final String EMAIL_ADDRESS_MALFORMED = "error:email-address-malformed";
    private static final String OK_RESPONSE = "OK"; // value doesn't really matter

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return EmailConfiguration.getInstance().getHello();
    }

    @POST
    @Path("/send-email-token")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEmailToken(@FormParam("email") String emailAddress) {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        // Test email with signature
        EmailTokens signer = new EmailTokens(conf.getSecretKey(), conf.getEmailTokenValidity());
        String token = signer.createToken(conf.getMailFrom());

        String mailBody = "Add an email address by clicking the following " +
                "link:\n\n  " + conf.getWebclientUrl() + "#verify-email/" +
                token;

        try {
            EmailSender.send(emailAddress, "mail verification", mailBody);
        } catch (AddressException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity
                    (EMAIL_ADDRESS_MALFORMED).build();
        }
        return Response.status(Response.Status.OK).entity
                (OK_RESPONSE).build();
    }
}
