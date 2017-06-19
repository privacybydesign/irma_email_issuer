package foundation.privacybydesign.email;

/**
 * Created by ayke on 19-6-17.
 */

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.*;

@ApplicationPath("/")
public class EmailService extends ResourceConfig {
    public EmailService() {
        register(EmailRestApi.class);
    }
}
