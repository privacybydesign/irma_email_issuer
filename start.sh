# this is the startup script in the docker container,
# doing a bunch of config at runtime before starting the actual server

set -e # exit the script immediately when an error is encountered

# in some cases secrets from different places might be required to be used together
# so this provides the option to provide a config template with some environment variables
echo "creating config.json based on template"
envsubst < /config/config.json > $IRMA_CONF/config.json

echo "generating binary file for private key"
openssl rsa -in /irma-jwt-key/priv.pem -outform der -out /usr/local/keys/priv.der

echo "copying config files to web app dir"
for lang in 'en' 'nl'; do
    cp /config/config.js /usr/local/tomee/webapps/ROOT/$lang/assets/config.js;
done


echo "starting up server"
exec catalina.sh run
