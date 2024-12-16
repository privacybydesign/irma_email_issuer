package foundation.privacybydesign.email.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for rate limiting. Subclasses provide storage methods (memory
 * for easier debugging and database for production).
 */
public abstract class RateLimit {
    private static Logger logger = LoggerFactory.getLogger(RateLimit.class);

    /** Take an e-mail address and rate limit it.
     * @param email e-mail address
     * @return the number of milliseconds that the client should wait - 0 if
     *         it shouldn't wait.
     */
    public long rateLimited(String email) {
        long now = System.currentTimeMillis();
        long retryAfter = nextTryEmail(email, now);

        if (retryAfter > now) {
            logger.warn("Denying request: email rate limit email exceeded");
            // Don't count this request if it has been denied.
            return retryAfter - now;
        }

        countEmail(email, now);
        return 0;
    }

    protected abstract long nextTryEmail(String email, long now);
    protected abstract void countEmail(String email, long now);
    public abstract void periodicCleanup();
}

class Limit {
    long timestamp;
    int tries;

    Limit(long timestamp, int tries) {
        this.timestamp = timestamp;
        this.tries = tries;
    }

    Limit(long now) {
        tries = 0;
        timestamp = now;
    }
}
