
# Email server

Add an email address for use in your [IRMA app](https://github.com/privacybydesign/irma_mobile).

## Setting up the server

### Prerequisite

Go, Gradle v4, JDK, Yarn, and an installation of IRMA server:

```bash
git clone git@github.com:privacybydesign/irmago.git
cd irmago/
go install ./irma
```

### Install

1. Clone repository
```bash
git clone git@github.com:privacybydesign/irma_email_issuer.git
cd irma_email_issuer/
```
2. Generate JWT keys for the issuer
```bash
./utils/keygen.sh ./src/main/resources/sk ./src/main/resources/pk
```
3. Build the project
```bash
gradle build
```
4. Copy `src/main/resources/config.sample.json` to `build/resources/main/config.json` and modify it
```bash
cp src/main/resources/config.sample.json build/resources/main/config.json
sed -i 's/"secret_key": "",/"secret_key": "thisisjustavalueandnotarealsecretsomemorecharactersuntilwehave64",/' ./build/resources/main/config.json
```
5. Create and configure front end
```bash
( cd webapp/
yarn install
./build.sh nl
cat > build/assets/config.js <<EOD
var config = {
  IRMASERVER: 'http://localhost:8088',
  EMAILSERVER: 'http://localhost:8080/irma_email_issuer',
};
EOD
)
cp -a webapp/ src/main/
```
6. Configure mail delivery in `build/resources/main/config.json`

### Run

1. Start IRMA server (in the root directory of this project)
```bash
~/go/bin/irma server --static-path ./webapp/build
```
2. Run the application with `gradle appRun`
3. Navigate to `http://localhost:8088/` with CORS disabled
(for example: `chromium --disable-web-security --user-data-dir=/tmp/chromium-disable-web-security`)
