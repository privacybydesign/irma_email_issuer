package foundation.privacybydesign.email.filters;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Apply this annotation to methods or resources to which {@link RateLimitRequestFilter}
 * should be applied.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {}
