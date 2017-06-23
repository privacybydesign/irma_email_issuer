package foundation.privacybydesign.email;

import javax.crypto.Mac;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by ayke on 22-6-17.
 */
public class EmailTokens {
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

    // Create a token: a value with a creation time and signature.
    // Can be used to create e.g. authentication tokens.
    public String createToken(String value) {
        // We could also use a bigger radix for the timestamp to make it
        // smaller (for example, a radix of 36 uses only 6 bytes instead of 10).
        String timestamp = Long.toString(System.currentTimeMillis() / 1000);

        return value + ":" + timestamp + ":" + signToken(value + ":" + timestamp);
    }

    // Sign a token (value+timestamp) with a HMAC. Output the digest of the
    // HMAC as base64 url-encoded string.
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

    // Verify the token. If it is verified and not expired, return the value.
    // Otherwise, return null.
    public String verifyToken(String token) {
        // Parse token
        String[] parts = token.split(":");
        if (parts.length != 3) {
            // invalid syntax
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
            return null;
        }

        // Verify expired tokens
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime > creationTime+tokenValidity) {
            // Token is no longer valid.
            return null;
        }

        // Verify signature
        String calculatedDigestText = signToken(value + ":" + timestamp);
        if (isEqualsConstantTime(digestText.toCharArray(),
                calculatedDigestText.toCharArray())) {
            return value;
        } else {
            return null;
        }
    }

    // Compare two byte arrays in constant time. I haven't been able to
    // quickly find a Java API for this (which would be a much better idea
    // than reinventing the wheel).
    private static boolean isEqualsConstantTime(char[] a, char[] b) {
        // I hope this is safe...
        // https://codahale.com/a-lesson-in-timing-attacks/
        // https://golang.org/src/crypto/subtle/constant_time.go (ConstantTimeCompare)
        // In Go, they also take special care to compare the result byte
        // bit-for-bit in constant time.

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
