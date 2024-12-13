
package foundation.privacybydesign.email.ratelimit;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import foundation.privacybydesign.email.redis.Redis;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

class RedisRateLimit extends RateLimit {
    private static final long SECOND = 1000; // 1000ms = 1s
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;

    private static Logger LOG = LoggerFactory.getLogger(RedisRateLimit.class);
    final private static String NAMESPACE = "rate-limit";

    private static RedisRateLimit instance;

    private JedisSentinelPool pool;

    RedisRateLimit() {
        pool = Redis.createSentinelPoolFromEnv();
    }

    static RateLimit getInstance() {
        if (instance == null) {
            instance = new RedisRateLimit();
        }
        return instance;
    }

    @Override
    protected long nextTryEmail(String email, long now) {
        // Rate limiter durations (sort-of logarithmic):
        // 1 10 second
        // 2 5 minute
        // 3 1 hour
        // 4 24 hour
        // 5+ 1 per day
        // Keep log 5 days for proper limiting.

        final String key = Redis.createKey(NAMESPACE, email);

        Limit limit;

        try (var jedis = pool.getResource()) {
            limit = limitFromRedis(jedis, key);
            if (limit == null) {
                limit = new Limit(now);
                limitToRedis(jedis, key, limit);
            }
        }
        //
        // Limit limit = phoneLimits.get(phone);
        // if (limit == null) {
        // limit = new Limit(now);
        // phoneLimits.put(phone, limit);
        // }
        long nextTry; // timestamp when the next request is allowed
        switch (limit.tries) {
            case 0: // try 1: always succeeds
                nextTry = limit.timestamp;
                break;
            case 1: // try 2: allowed after 10 seconds
                nextTry = limit.timestamp + 10 * SECOND;
                break;
            case 2: // try 3: allowed after 5 minutes
                nextTry = limit.timestamp + 5 * MINUTE;
                break;
            case 3: // try 4: allowed after 3 hours
                nextTry = limit.timestamp + 3 * HOUR;
                break;
            case 4: // try 5: allowed after 24 hours
                nextTry = limit.timestamp + 24 * HOUR;
                break;
            default:
                throw new IllegalStateException("invalid tries count");
        }
        return nextTry;
    }

    @Override
    protected void countEmail(String email, long now) {
        long nextTry = nextTryEmail(email, now);
        final String key = Redis.createKey(NAMESPACE, email);

        try (var jedis = pool.getResource()) {
            Limit limit = limitFromRedis(jedis, key);
            if (limit == null) {
                return;
            }
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
            limitToRedis(jedis, key, limit);
        }
    }

    @Override
    public void periodicCleanup() {
        long now = System.currentTimeMillis();

        final String pattern = Redis.createNamespace(NAMESPACE) + "*";
        ScanParams scanParams = new ScanParams().match(pattern);
        String cursor = "0";

        try (var jedis = pool.getResource()) {
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                List<String> keys = scanResult.getResult();
                cursor = scanResult.getCursor();

                for (String key : keys) {
                    Limit limit = limitFromRedis(jedis, key);
                    if (limit != null && limit.timestamp < now - 5 * DAY) {
                        jedis.del(key);
                    }
                }
            } while (!cursor.equals("0")); // continue until the cursor wraps around
        }
    }

    void limitToRedis(Jedis jedis, String key, Limit limit) {
        final String ts = Long.toString(limit.timestamp);
        final String tries = Long.toString(limit.tries);
        jedis.hset(key, "timestamp", ts);
        jedis.hset(key, "tries", tries);
    }

    Limit limitFromRedis(Jedis jedis, String key) {
        try {
            final long ts = Long.parseLong(jedis.hget(key, "timestamp"));
            final int tries = Integer.parseInt(jedis.hget(key, "tries"));
            return new Limit(ts, tries);
        } catch (NumberFormatException e) {
            LOG.error("failed to parse int: " + e.getMessage());
            return null;
        }
    }
}
