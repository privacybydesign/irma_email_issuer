package foundation.privacybydesign.email.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store rate limits in memory. Useful for debugging and rate limits that
 * aren't very long.
 *
 * How it works:
 * How much budget a user has, is expressed in a timestamp. The timestamp is
 * initially some period in the past, but with every usage (countEmail)
 * this timestamp is incremented. For e-mail addresses this
 * amount is exponential.
 *
 * An algorithm with a similar goal is the Token Bucket algorithm. This
 * algorithm probably works well, but seemed harder to implement.
 * https://en.wikipedia.org/wiki/Token_bucket
 */
public class MemoryRateLimit extends RateLimit {
    private static final long SECOND = 1000; // 1000ms = 1s
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;

    private static MemoryRateLimit instance;

    private final Map<String, Limit> emailLimits;

    public MemoryRateLimit() {
        emailLimits = new ConcurrentHashMap<>();
    }

    public static MemoryRateLimit getInstance() {
        if (instance == null) {
            instance = new MemoryRateLimit();
        }
        return instance;
    }

    // Is the user over the rate limit per e-mail address?
    @Override
    protected synchronized long nextTryEmail(String email, long now) {
        // Rate limiter durations (sort-of logarithmic):
        // 1 10 seconds
        // 2 1 minute
        // 3 5 minutes
        // 4 1 hour
        // 5 12 hours
        // 6+ 2 per day
        // Keep log 2 days for proper limiting.
        Limit limit = emailLimits.get(email);
        if (limit == null) {
            limit = new Limit(now);
            emailLimits.put(email, limit);
        }
        long nextTry; // timestamp when the next request is allowed
        switch (limit.tries) {
            case 0: // try 1: always succeeds
                nextTry = limit.timestamp;
                break;
            case 1: // try 2: allowed after 10 seconds
                nextTry = limit.timestamp + 10 * SECOND;
                break;
            case 2: // try 3: allowed after 1 minute
                nextTry = limit.timestamp + MINUTE;
                break;
            case 3: // try 4: allowed after 5 minutes
                nextTry = limit.timestamp + 5 * MINUTE;
                break;
            case 4: // try 5: allowed after 1 hour
                nextTry = limit.timestamp + HOUR;
                break;
            case 5: // try 6: allowed after 12 hours
                nextTry = limit.timestamp + 12 * HOUR;
                break;
            default:
                throw new IllegalStateException("invalid tries count");
        }
        return nextTry;
    }

    // Count the usage of this rate limit - adding to the budget for this
    // e-mail address.
    @Override
    protected synchronized void countEmail(String email, long now) {
        long nextTry = nextTryEmail(email, now);
        Limit limit = emailLimits.get(email);
        if (nextTry > now) {
            throw new IllegalStateException("counting rate limit while over the limit");
        }
        limit.tries = Math.min(limit.tries + 1, 6); // add 1, max at 6
        // If the last usage was e.g. ≥2 days ago, we should allow them 2
        // extra tries this day.
        long lastTryDaysAgo = (now - limit.timestamp) / DAY;
        long bonusTries = limit.tries - lastTryDaysAgo;
        if (bonusTries >= 1) {
            limit.tries = (int) bonusTries;
        }
        limit.timestamp = now;
    }

    @Override
    public void periodicCleanup() {
        long now = System.currentTimeMillis();
        // Use enhanced for loop, because an iterator makes sure concurrency issues
        // cannot occur.
        for (Map.Entry<String, Limit> entry : emailLimits.entrySet()) {
            if (entry.getValue().timestamp < now - 2 * DAY) {
                emailLimits.remove(entry.getKey());
            }
        }
    }
}
