package foundation.privacybydesign.email;

import org.junit.Test;

import foundation.privacybydesign.email.ratelimit.MemoryRateLimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Test whether the rate limiter works as expected.
 */
public class RateLimitTest {

    @Test
    public void testRateLimit() {

        var rateLimit = MemoryRateLimit.getInstance();
        var email = "test@test.nl";

        assertEquals("not rate limited", 0, rateLimit.rateLimited(email)); 
        
        assertNotEquals("rate limited", 0, rateLimit.rateLimited(email));
    }
}
