package foundation.privacybydesign.email;

import foundation.privacybydesign.common.BaseConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import org.irmacard.api.common.util.GsonUtil;

import java.io.IOException;

/**
 * Created by ayke on 19-6-17.
 */
public class EmailConfiguration extends BaseConfiguration {
    static EmailConfiguration instance;
    static final String CONFIG_FILENAME = "config.json";

    private String web_client_url = "";
    private String verify_email_subject = "";
    private String verify_email_body = "";
    private String mail_host = "";
    private int mail_port = 25;
    private String mail_user = "";
    private String mail_password = "";
    private String mail_from_address = "";
    private String secret_key = "";
    private long token_validity = 0;
    private String server_name = "";
    private String human_readable_name = "";
    private String scheme_manager = "";
    private String email_issuer = "";
    private String email_credential = "";
    private String email_attribute = "";

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

    public String getWebclientUrl() { return web_client_url; }

    public String getVerifyEmailSubject() { return verify_email_subject; }

    public String getVerifyEmailBody() { return verify_email_body; }

    public String getMailHost() { return mail_host; }

    public int getMailPort() { return mail_port; }

    public String getMailUser() { return mail_user; }

    public String getMailPassword() { return mail_password; }

    public String getMailFrom() { return mail_from_address; }

    public String getSecretKey() { return secret_key; }

    public long getEmailTokenValidity() { return token_validity; }

    public String getServerName() { return server_name; }

    public String getHumanReadableName() { return human_readable_name; }

    public String getSchemeManager() { return scheme_manager; }

    public String getEmailIssuer() { return email_issuer; }

    public String getEmailCredential() { return email_credential; }

    public String getEmailAttribute() { return email_attribute; }

    public SignatureAlgorithm getJwtAlgorithm() { return SignatureAlgorithm.RS256; }
}
