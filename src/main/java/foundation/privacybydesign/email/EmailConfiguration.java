package foundation.privacybydesign.email;

import io.jsonwebtoken.SignatureAlgorithm;
import org.irmacard.api.common.util.GsonUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by ayke on 19-6-17.
 */
public class EmailConfiguration {
    static EmailConfiguration instance;
    static final String CONFIG_FILENAME = "config.json";

    private String web_client_url = "";
    private String mail_host = "";
    private int mail_port = 25;
    private String mail_user = "";
    private String mail_password = "";
    private String mail_from_address = "";
    private String secret_key = "";
    private long token_validity = 0;
    private String private_key_path = "";
    private String server_name = "";
    private String human_readable_name = "";
    private String scheme_manager = "";
    private String email_issuer = "";
    private String email_credential = "";
    private String email_attribute = "";

    private PrivateKey privateKey = null;

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

    public static byte[] getResource(String filename) throws IOException {
        URL url = EmailConfiguration.class.getClassLoader().getResource(filename);
        if (url == null)
            throw new IOException("Could not load file " + filename);

        URLConnection urlCon = url.openConnection();
        urlCon.setUseCaches(false);
        return convertSteamToByteArray(urlCon.getInputStream(), 2048);
    }

    public static byte[] convertSteamToByteArray(InputStream stream, int size) throws IOException {
        byte[] buffer = new byte[size];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int line;
        while ((line = stream.read(buffer)) != -1) {
            os.write(buffer, 0, line);
        }
        stream.close();

        os.flush();
        os.close();
        return os.toByteArray();
    }

    public PrivateKey getPrivateKey() throws KeyManagementException {
        if (privateKey == null) {
            privateKey = loadPrivateKey(private_key_path);
        }
        return privateKey;
    }


    private PrivateKey loadPrivateKey(String filename) throws KeyManagementException {
        try {
            return decodePrivateKey(EmailConfiguration.getResource(filename));
        } catch (IOException e) {
            throw new KeyManagementException(e);
        }
    }

    private PrivateKey decodePrivateKey(byte[] rawKey) throws KeyManagementException {
        try {
            if (rawKey == null || rawKey.length == 0)
                throw new KeyManagementException("Could not read private key");

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(rawKey);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (NoSuchAlgorithmException |InvalidKeySpecException e) {
            throw new KeyManagementException(e);
        }
    }


    public String getWebclientUrl() { return web_client_url; }

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
