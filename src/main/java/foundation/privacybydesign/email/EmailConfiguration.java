package foundation.privacybydesign.email;

import org.irmacard.api.common.util.GsonUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

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

    public String getWebclientUrl() { return web_client_url; }

    public String getMailHost() { return mail_host; }

    public int getMailPort() { return mail_port; }

    public String getMailUser() { return mail_user; }

    public String getMailPassword() { return mail_password; }

    public String getMailFrom() { return mail_from_address; }

    public String getSecretKey() { return secret_key; }

    public long getEmailTokenValidity() { return token_validity; }
}
