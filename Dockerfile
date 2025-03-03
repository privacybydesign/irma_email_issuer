FROM node:20 AS webappbuild

# Build the webapp
COPY ./webapp/ /webapp/
RUN mkdir -p /www

WORKDIR /webapp
RUN yarn install
RUN ./build.sh en && mv build /www/en
RUN ./build.sh nl && mv build /www/nl
# Let root redirect to the english version
RUN cp /webapp/redirect-en.html /www/index.html

# --------------------------------------------------------

FROM gradle:7.6-jdk11 AS javabuild

# Build the java app
COPY ./ /app/
WORKDIR /app
RUN gradle build

# --------------------------------------------------------

FROM tomee:9.1-jre11
RUN apt-get update && apt-get install gettext-base

WORKDIR /server

# Copy the webapp to the webapps directory
RUN rm -rf /usr/local/tomee/webapps/*
COPY --from=webappbuild /www/ /usr/local/tomee/webapps/ROOT/

# Copy the war file to the webapps directory
COPY --from=javabuild /app/build/libs/irma_email_issuer.war /usr/local/tomee/webapps/

# Copy template files to the email templates directory
COPY ./src/main/resources/email-en.html /email-templates/email-en.html
COPY ./src/main/resources/email-nl.html /email-templates/email-nl.html

COPY --from=javabuild /app/start.sh ./start.sh
RUN chmod +x ./start.sh

RUN mkdir /usr/local/keys
RUN mkdir /irma-config
ENV IRMA_CONF="/irma-config/"
ENV EMAIL_TEMPLATE_DIR="/email-templates/"
EXPOSE 8080

CMD [ "/bin/sh", "-C", "./start.sh" ]
