
FROM yarnpkg/node-yarn:latest as webappbuild

ARG LANGUAGE=en

# Build the webapp
COPY ./webapp/ /webapp/
WORKDIR /webapp
RUN yarn install && ./build.sh ${LANGUAGE}

FROM gradle:7.6-jdk11 as javabuild

# Build the java app
COPY ./ /app/
WORKDIR /app
RUN gradle build

FROM tomee:9.0-jre11

# Copy the webapp to the webapps directory
COPY --from=webappbuild /webapp/build/ /usr/local/tomee/webapps/ROOT/

# Copy the war file to the webapps directory
COPY --from=javabuild /app/build/libs/irma_email_issuer-1.1.0.war /usr/local/tomee/webapps/

EXPOSE 8080