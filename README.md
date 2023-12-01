
# irma_email_issuer

Add an e-mail address for use in your [Yivi app](https://github.com/privacybydesign/irmamobile).

## Running (development)
The easiest way to run the irma_email_issuer for development purposes is via Docker.

### Configuration
Various configuration files, keys and settings need to be in place to be able to build and run the apps.

1. To generate the required keys for the issuer, run:
```bash
$ utils/keygen.sh ./src/main/resources/sk ./src/main/resources/pk
```

2. Create the Java app configuration:
Copy the file `src/main/resources/config.sample.json` to `src/main/resources/config.json`.

### Run
Use docker-compose up combined with your localhost IP address as environment variable to spin up the containers:
```bash
$ IP=192.168.1.105 docker-compose up
```
Note: do not use `127.0.0.1` or `0.0.0.0` as IP addresses as this will result in the app not being able to find the issuer.

By default, docker-compose caches docker images, so on a second run the previous built images will be used. A fresh build can be enforced using the --build flag.
```bash
$ IP=192.168.1.105 docker-compose up --build
```

The configuration should be mounted in the `/config` directory of the container. The `docker-compose.yml` file already contains this configuration.

## Manual
The Java api and JavaScript frontend can be built and run manually using the following commands:

1. Generate JWT keys for the issuer
```bash
$ utils/keygen.sh ./src/main/resources/sk ./src/main/resources/pk
```

2. Copy the file `src/main/resources/config.sample.json` to `src/main/resources/config.json` and modify it.

3. Build the webapp:
```bash
$ cd webapp && yarn install && yarn build en && cd ../
```
If you want to build another language, for example Dutch, change `build en` to `build nl`.

4. Copy the file `webapp/config.example.js` to `webapp/build/assets/config.js` and modify it 

5. Run the following command in the root directory of this project:
```bash
$ gradle appRun
```

To open the webapp navigate to http://localhost:8080. The API is accessible via http://localhost:8080/irma_email_issuer/api

## Test
You can run the tests, defined in `src/test/java/foundation/privacybydesign/email`, using the following command:
```bash
$ gradle test
```
