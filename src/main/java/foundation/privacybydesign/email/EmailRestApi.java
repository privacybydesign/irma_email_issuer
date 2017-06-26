package foundation.privacybydesign.email;

import foundation.privacybydesign.email.filters.RateLimit;
import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.CredentialRequest;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.api.common.issuing.IssuingRequest;
import org.irmacard.credentials.info.CredentialIdentifier;

import javax.mail.internet.AddressException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by ayke on 19-6-17.
 */
@Path("")
public class EmailRestApi {
    private static final String ERR_ADDRESS_MALFORMED = "error:email-address-malformed";
    private static final String ERR_INVALID_TOKEN = "error:invalid-token";
    private static final String OK_RESPONSE = "OK"; // value doesn't really matter

    private EmailTokens signer;

    public EmailRestApi() {
        EmailConfiguration conf = EmailConfiguration.getInstance();
        signer = new EmailTokens(conf.getSecretKey(), conf.getEmailTokenValidity());
    }

    @POST
    @Path("/send-email-token")
    @Produces(MediaType.TEXT_PLAIN)
    @RateLimit
    public Response sendEmailToken(@FormParam("email") String emailAddress) {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        // Test email with signature
        String token = signer.createToken(emailAddress);

        String url = conf.getWebclientUrl() + "#verify-email/" + token;
        String mailBody = conf.getVerifyEmailBody() + "\n\n" + url;

        try {
            EmailSender.send(emailAddress, conf.getVerifyEmailSubject(), mailBody);
        } catch (AddressException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity
                    (ERR_ADDRESS_MALFORMED).build();
        }
        return Response.status(Response.Status.OK).entity
                (OK_RESPONSE).build();
    }

    @POST
    @Path("/verify-email-token")
    @Produces(MediaType.TEXT_PLAIN)
    public Response verifyEmailToken(@FormParam("token") String token) throws KeyManagementException {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        String emailAddress = signer.verifyToken(token);
        if (emailAddress == null) {
            // cannot verify (may be expired or have an invalid signature)
            // TODO: inform the user if it's expired vs other errors
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ERR_INVALID_TOKEN).build();
        }

        // Mostly copied from https://github.com/credentials/irma_keyshare_server/blob/master/src/main/java/org/irmacard/keyshare/web/WebClientResource.java
        ArrayList<CredentialRequest> credentials = new ArrayList<>(1);
        HashMap<String, String> attrs = new HashMap<>(1);
        attrs.put(conf.getEmailAttribute(), emailAddress);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        credentials.add(new CredentialRequest((int)CredentialRequest
                .floorValidityDate(calendar.getTimeInMillis(), true),
                new CredentialIdentifier(
                        conf.getSchemeManager(),
                        conf.getEmailIssuer(),
                        conf.getEmailCredential()
                ),
                attrs
        ));

        IdentityProviderRequest ipRequest = new IdentityProviderRequest("",
                new IssuingRequest(null, null, credentials), 120);
        String jwt = ApiClient.getSignedIssuingJWT(ipRequest,
                conf.getServerName(),
                conf.getHumanReadableName(),
                conf.getJwtAlgorithm(),
                conf.getPrivateKey());
        return Response.status(Response.Status.OK)
                .entity(jwt).build();
    }
}
