package foundation.privacybydesign.email;

import org.irmacard.api.common.ApiClient;
import org.irmacard.api.common.CredentialRequest;
import org.irmacard.api.common.issuing.IdentityProviderRequest;
import org.irmacard.api.common.issuing.IssuingRequest;
import org.irmacard.credentials.info.CredentialIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import foundation.privacybydesign.email.ratelimit.MemoryRateLimit;
import foundation.privacybydesign.email.ratelimit.RateLimit;
import jakarta.mail.internet.AddressException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * REST API for use by the web client.
 */
@Path("")
public class EmailRestApi {
    private static Logger logger = LoggerFactory.getLogger(EmailRestApi.class);
    private static RateLimit rateLimiter = MemoryRateLimit.getInstance();

    private static final String ERR_ADDRESS_MALFORMED = "error:email-address-malformed";
    private static final String ERR_INVALID_TOKEN = "error:invalid-token";
    private static final String ERR_INVALID_LANG = "error:invalid-language";
    private static final String OK_RESPONSE = "OK"; // value doesn't really matter
    private static final String ERR_RATE_LIMITED = "error:ratelimit";

    private EmailTokens signer;

    public EmailRestApi() {
        EmailConfiguration conf = EmailConfiguration.getInstance();
        signer = new EmailTokens(conf.getSecretKey(), conf.getEmailTokenValidity());
    }

    @POST
    @Path("/send-email")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEmail(@FormParam("email") String email,
                              @FormParam("language") String lang,
                              @HeaderParam("Authorization") String auth) {
        EmailConfiguration conf = EmailConfiguration.getInstance();
        Client client = conf.getClient(auth);
        if (client == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        lang = parseLanguage(lang);
     
        // We only accept lowercase email addresses.
        if (!email.equals(email.toLowerCase())) {
            logger.error("Address contains uppercase characters");
            return Response.status(Response.Status.BAD_REQUEST).entity(ERR_ADDRESS_MALFORMED).build();
        }

        try {

            long retryAfter = rateLimiter.rateLimited(email);
            if (retryAfter > 0) {
                // 429 Too Many Requests
                // https://tools.ietf.org/html/rfc6585#section-4
                return Response.status(429)
                        .entity(ERR_RATE_LIMITED)
                        .header("Retry-After", (int) Math.ceil(retryAfter / 1000.0))
                        .build();
            }

            String token = signer.createToken(email);

            String url = conf.getServerURL(lang) + "#verify-email/" + token
                    + "/" + URLEncoder.encode(client.getReturnURL(), StandardCharsets.UTF_8.toString());
            EmailSender.send(
                    email,
                    client.getEmailSubject(lang),
                    client.getEmail(lang),
                    client.getReplyToEmail(),
                    true,
                    url,
                    url
            );
        } catch (AddressException e) {
            logger.error("Invalid address: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(ERR_ADDRESS_MALFORMED).build();
        } catch (UnsupportedEncodingException e) {
            logger.error("Invalid return URL: {}: {}", client.getReturnURL(), e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Sending mail failed:\n{}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.OK).entity(OK_RESPONSE).build();
    }

    /**
     * Send an email with a token for verification. Send back whether it
     * succeeded.
     *
     * @param emailAddress the user address to send to
     * @return 200 OK for success or an error if it fails
     */
    @POST
    @Path("/send-email-token")
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendEmailToken(@FormParam("email") String emailAddress,
                                   @FormParam("language") String language) {
        EmailConfiguration conf = EmailConfiguration.getInstance();

        // We only accept lowercase email addresses.
        if (!emailAddress.equals(emailAddress.toLowerCase())) {
            logger.error("Address contains uppercase characters");
            return Response.status(Response.Status.BAD_REQUEST).entity(ERR_ADDRESS_MALFORMED).build();
        }

        long retryAfter = rateLimiter.rateLimited(emailAddress);
        if (retryAfter > 0) {
            // 429 Too Many Requests
            // https://tools.ietf.org/html/rfc6585#section-4
            return Response.status(429)
                    .entity(ERR_RATE_LIMITED)
                    .header("Retry-After", (int) Math.ceil(retryAfter / 1000.0))
                    .build();
        }

        // Test email with signature
        String token = signer.createToken(emailAddress);

        language = parseLanguage(language);

        String mailBodyTemplate = conf.getVerifyEmailBody(language);
        if (mailBodyTemplate == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity
                    (ERR_INVALID_LANG).build();
        }

        try {
            EmailSender.send(
                    emailAddress,
                    conf.getVerifyEmailSubject(language),
                    mailBodyTemplate,
                    null,
                    true,
                    conf.getServerURL(language) + "#verify-email/" + token
            );
        } catch (AddressException e) {
            logger.error("Invalid address: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity
                    (ERR_ADDRESS_MALFORMED).build();
        } catch (Exception e) {
            logger.error("Sending mail failed:\n{}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.OK).entity
                (OK_RESPONSE).build();
    }

    /**
     * Verify an email token and if it verifies, return an issuing JWT with
     * the email address.
     *
     * @param token the token as sent in the email to the user for verification
     * @return either 200 OK with the JWT, or a HTTP error with an error string.
     * @throws KeyManagementException
     */
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
        String[] emailParts = emailAddress.split("@");
        if (emailParts.length != 2) {
            logger.error("Invalid address");
            return Response.status(Response.Status.BAD_REQUEST).entity
                    (ERR_ADDRESS_MALFORMED).build();
        }


        // Mostly copied from https://github.com/credentials/irma_keyshare_server/blob/master/src/main/java/org/irmacard/keyshare/web/WebClientResource.java
        ArrayList<CredentialRequest> credentials = new ArrayList<>(1);
        HashMap<String, String> attrs = new HashMap<>(1);
        attrs.put(conf.getEmailAttribute(), emailAddress);
        attrs.put(conf.getDomainAttribute(), emailParts[1]);
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

    /**
     * Return a sanitized language code or the default language based on the given input
     * 
     * @param language the language code to sanitize
     * @return String with language code
     */
    private String parseLanguage(String language){
        if (language == null || language.length() == 0){
            language = EmailConfiguration.getInstance().getDefaultLanguage();
        }
        else {
            // Only allow letters in the language code to prevent path and other injection attacts
            language = language.replaceAll("[^a-zA-Z]", "");
        }
        return language;
    }
}
