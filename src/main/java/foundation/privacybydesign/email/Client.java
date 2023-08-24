package foundation.privacybydesign.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

public class Client {
	private HashMap<String, String> email_files;
	private HashMap<String, String> email_subject;
	private String return_url;
	private String reply_to_email;
	private String name;

	private static Logger logger = LoggerFactory.getLogger(Client.class);

	public String getEmailFile(String lang) {
		return "clientmails/" + email_files.get(lang);
	}

	public String getEmail(String lang)  {
		try {
			return new String(EmailConfiguration.getResource(getEmailFile(lang)));
		} catch (Exception e) {
			logger.error("Failed to read email file");
			throw new RuntimeException(e);
		}
	}

	public String getEmailSubject(String lang) {
		return email_subject.get(lang);
	}

	public String getReturnURL() {
		return return_url;
	}

	public String getReplyToEmail() {
		return reply_to_email;
	}

	public String getName() {
		return name;
	}
}
