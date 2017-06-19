
# Email server

Add an email address for use in your [IRMA app](https://github.com/credentials/irma_android_cardemu).


## Setting up the server

 1. Copy the file `src/main/resources/config.sample.json` to
 `build/resources/main/config.json` and modify it.
 2. Run `gradle appRun` in the root directory of this project.
 3. Navigate to `http://localhost:8080/irma_email_provider/api/hello`
