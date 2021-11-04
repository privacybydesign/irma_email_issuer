
# Email server

Add an email address for use in your [IRMA app](https://github.com/privacybydesign/irma_mobile).


## Setting up the server

1. Generate JWT keys for the issuer
```bash
./utils/keygen.sh ./src/main/resources/sk ./src/main/resources/pk
```
2. Copy the file `src/main/resources/config.sample.json` to
 `build/resources/main/config.json` and modify it.
3. Run `gradle appRun` in the root directory of this project.
4. Navigate to `http://localhost:8080/irma_email_issuer/api/hello`
