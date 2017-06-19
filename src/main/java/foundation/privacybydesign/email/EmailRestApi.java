package foundation.privacybydesign.email;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by ayke on 19-6-17.
 */
@Path("")
public class EmailRestApi {
    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello world!";
    }
}
