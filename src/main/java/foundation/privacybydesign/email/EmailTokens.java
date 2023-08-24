package foundation.privacybydesign.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.SecretKeySpec;
import jakarta.xml.bind.DatatypeConverter;
/**
 * Create and verify tokens that can be used in email verification messages.
 *
 * Based on this idea:
 *   https://aykevl.nl/2015/01/south-stateless-authenticated-sessions-http-golang
 * In short, a token has the format payload:timestamp:signature
 * where the payload can be any email address (email addresses may not
 * contain colons according to the HTML5 <input type=email> field so it
 * should be fine). The timestamp is the creation timestamp. The signature is
 * over the payload:timestamp part. This way it is relatively easy to expire
 * tokens (set a different validity) or revoke (change the key).
 *
 * The tokens aren't too long, just 55 bytes + the payload. And by using the
 * URL version of base64 the potentially problematic '/' and '+' characters
 * are avoided so tokens can be easily put in a URL without escaping.
 */
public class EmailTokens {
    private static Logger logger = LoggerFactory.getLogger(EmailTokens.class);
    private static String SIGNING_ALGORITHM = "HmacSHA256";

    private Mac mac;
    private long tokenValidity;

    public EmailTokens(String signingKey, long tokenValidity) {
        this.tokenValidity = tokenValidity;

        // HMAC calculated using this sample:
        // https://gist.github.com/ishikawa/88599/3195bdeecabeb38aa62872ab61877aefa6aef89e
        SecretKeySpec key = new SecretKeySpec(signingKey.getBytes(), SIGNING_ALGORITHM);
        try {
            mac = Mac.getInstance(SIGNING_ALGORITHM);
            mac.init(key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // This should not normally happen
            throw new RuntimeException("Unknown key?");
        }
    }

    /**
     * Create a token: a value with a creation time and signature.
     * Can be used to create e.g. authentication tokens.
     */
    public String createToken(String value) {
        // We could also use a bigger radix for the timestamp to make it
        // smaller (for example, a radix of 36 uses only 6 bytes instead of 10).
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);

        return value + ":" + timestamp + ":" + signToken(value + ":" + timestamp);
    }

    /**
     * Sign a token (value+timestamp) with a HMAC. Output the digest of the
     * HMAC as base64 url-encoded string.
     */
    private String signToken(String token) {
        // See https://aykevl.nl/2015/01/south-stateless-authenticated-sessions-http-golang
        // for background on the system.
        byte[] digestBytes = mac.doFinal(token.getBytes());
        String digest = DatatypeConverter.printBase64Binary(digestBytes);
        digest = digest.substring(0, 43); // strip tailing '='
        digest = digest // convert to base64 URL encoding
                .replace('+', '-')
                .replace('/', '_');

        return digest;
    }

    /**
     * Verify the token. If it is verified and not expired, return the value.
     * Otherwise, return null.
     */
    public String verifyToken(String token) {
        // Parse token
        String[] parts = token.split(":");
        if (parts.length != 3) {
            // invalid syntax
            logger.error("Token {} does not have 3 parts", token);
            return null;
        }
        String value = parts[0];
        String timestamp = parts[1];
        String digestText = parts[2];

        long creationTime;
        try {
            creationTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            // Invalid syntax
            logger.error("Token {} has non-integer creation time", token);
            return null;
        }

        // Verify expired tokens
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime > creationTime+tokenValidity) {
            // Token is no longer valid.
            logger.error("Token {} has expired", token);
            return null;
        }

        // Verify signature
        String calculatedDigestText = signToken(value + ":" + timestamp);
        if (isEqualsConstantTime(digestText.toCharArray(),
                calculatedDigestText.toCharArray())) {
            return value;
        } else {
            logger.error("Token {} has invalid HMAC", token);
            return null;
        }
    }

    /**
     * Compare two byte arrays in constant time.
     */
    public static boolean isEqualsConstantTime(char[] a, char[] b) {
        if (a.length != b.length) {
            return false;
        }

        byte result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
    
}