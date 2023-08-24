package foundation.privacybydesign.email;

import foundation.privacybydesign.email.EmailTokens;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test class for valid and invalid (malformed, expired, tampered) signatures.
 *
 * TODO: maybe we should throw an error in the EmailTokens class so we know
 * that we're actually testing the right thing (and the token isn't simply
 * malformed while we're testing an expired one).
 *
 * TODO: mock the time for better testability.
 */
public class EmailTokensTest {
    private EmailTokens signer = new EmailTokens("password", 60);
    private String validToken = signer.createToken("testtoken");

    @Test
    public void testValidToken() {
        assertEquals("valid token failed to verify",
                signer.verifyToken(validToken), "testtoken");
    }

    @Test
    public void testExpiredToken() {
        assertNull("expired token must be null",
                signer.verifyToken("testtoken:1499377646:sadRkftPRiBhl1eXrgUwDFwYOfwm-Zkdg_ubOABkVXM"));
    }

    @Test
    public void testMalformedToken() {
        assertNull("malformed token must be null",
                signer.verifyToken(validToken.replace(
                        "testtoken:",
                        "testtoken.")));
    }

    @Test
    public void testTamperedTimeToken1() {
        assertNull("token with a changed time must be null",
                signer.verifyToken(validToken.replace(
                        "testtoken:1",
                        "testtoken:2")));
    }

    @Test
    public void testTamperedTimeToken2() {
        assertNull("token with a changed time must be null",
                signer.verifyToken(validToken.replace(
                        "testtoken:1",
                        "testtoken:0")));
    }

    @Test
    public void testTamperedMessageToken() {
        assertNull("token with a changed payload must be null",
                signer.verifyToken(validToken.replace(
                        "testtoken:",
                        "testmessg:")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmailTokensWithoutPassword() {
        // A password *must* be provided, so this will throw an error.
        new EmailTokens("", 60);
    }
}