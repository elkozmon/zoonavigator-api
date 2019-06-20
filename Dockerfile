ARG SBT_VERSION=1.1.5
ARG SBT_GPG_KEY=99E82A75642AC823

FROM openjdk:8u151-jdk-alpine3.7 as sbt
MAINTAINER Lubos Kozmon <lubosh91@gmail.com>

ARG SBT_VERSION
ARG SBT_GPG_KEY

# Install sbt
RUN apk --no-cache add aria2 gnupg ca-certificates bash \
  && aria2c -x4 "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
  && aria2c -x4 "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz.asc" \
  && gpg --keyserver keyserver.ubuntu.com --recv-keys ${SBT_GPG_KEY} \
  && gpg --verify sbt-${SBT_VERSION}.tgz.asc sbt-${SBT_VERSION}.tgz \
  && tar xvfz sbt-${SBT_VERSION}.tgz -C /usr/local \
  && ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

# Copy source files
WORKDIR /src
COPY . .

# Build project
RUN sbt play/dist \
  && VERSION=$(ls play/target/universal/zoonavigator-play-*.zip | sed -E 's/.*zoonavigator-play-(.*).zip$/\1/') \
  && unzip play/target/universal/zoonavigator-play-$VERSION.zip \
  && mv zoonavigator-play-$VERSION /app

# Make scripts executable
RUN chmod +x \
    docker/files/app/run.sh \
    docker/files/app/healthcheck.sh

FROM openjdk:8u151-jdk-alpine3.7

# Default config
ENV API_HTTP_PORT=9000 \
    API_SESSION_TIMEOUT_MILLIS=900000 \
    ZK_CLIENT_TIMEOUT_MILLIS=5000 \
    ZK_CONNECT_TIMEOUT_MILLIS=5000

# Copy app files
COPY --from=sbt /src/docker/files /
COPY --from=sbt /app /app

WORKDIR /app

# Install dependencies
RUN apk --no-cache add curl krb5 bash

# Create non-root user
RUN addgroup -g 1000 zoonavigator-api && \
    adduser -D -u 1000 zoonavigator-api -G zoonavigator-api

# Add health check
HEALTHCHECK --interval=30s --timeout=3s \
    CMD ./healthcheck.sh

# Expose default HTTP port
EXPOSE 9000

# Ensure that our user can access /app
RUN chown -R zoonavigator-api:zoonavigator-api /app

# Cause our command to be executed as our user
USER zoonavigator-api:zoonavigator-api

CMD ["./run.sh"]
