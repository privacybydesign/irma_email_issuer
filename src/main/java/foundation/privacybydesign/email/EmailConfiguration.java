package foundation.privacybydesign.email;

import foundation.privacybydesign.common.BaseConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import org.irmacard.api.common.util.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class EmailConfiguration extends BaseConfiguration {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    static EmailConfiguration instance;
    static final String CONFIG_FILENAME = "config.json";

    static {
    	BaseConfiguration.confDirName = "irma_email_issuer";
    }

    private Map<String, String> verify_email_subject = null;
    private Map<String, String> server_url = null;
    private String mail_host = "";
    private int mail_port = 25;
    private String mail_user = "";
    private String mail_password = "";
    private String mail_from_address = "";
    private boolean mail_starttls_required = true;
    private String secret_key = "";
    private long token_validity = 0;
    private String server_name = "";
    private String human_readable_name = "";
    private String scheme_manager = "";
    private String email_issuer = "";
    private String email_credential = "";
    private String email_attribute = "";
    private String domain_attribute = "";

    private String default_language = "nl";

    private HashMap<String, Client> clients = null;

    public static EmailConfiguration getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        try {
            String json = new String(getResource(CONFIG_FILENAME));
            instance = GsonUtil.getGson().fromJson(json, EmailConfiguration.class);
        } catch (IOException e) {
            System.out.println("could not load configuration");
            instance = new EmailConfiguration();
        }
    }

    public String getVerifyEmailSubject(String language) { return verify_email_subject.get(language); }

    public String getVerifyEmailBody(String language) {
        try {
            return new String(EmailConfiguration.getResource("email-" + language + ".html"));
        } catch (IOException e) {
            logger.error("Failed to read email file");
            throw new RuntimeException(e);
        }
    }

    public PrivateKey getPrivateKey() throws KeyManagementException {
        return BaseConfiguration.getPrivateKey("sk.der");
    }

    public String getMailHost() { return mail_host; }

    public int getMailPort() { return mail_port; }

    public String getMailUser() { return mail_user; }

    public String getMailPassword() { return mail_password; }

    public String getMailFrom() { return mail_from_address; }

    public boolean getStarttlsRequired() { return mail_starttls_required; }

    public String getSecretKey() { return secret_key; }

    public long getEmailTokenValidity() { return token_validity; }

    public String getServerName() { return server_name; }

    public String getHumanReadableName() { return human_readable_name; }

    public String getSchemeManager() { return scheme_manager; }

    public String getEmailIssuer() { return email_issuer; }

    public String getEmailCredential() { return email_credential; }

    public String getEmailAttribute() { return email_attribute; }

    public String getDomainAttribute() { return domain_attribute; }

    public SignatureAlgorithm getJwtAlgorithm() { return SignatureAlgorithm.RS256; }

    public Client getClient(String token) {
        return clients.get(token);
    }

    public String getDefaultLanguage() {
        return default_language;
    }

    public String getServerURL(String language) {
        return server_url.get(language);
    }
}
