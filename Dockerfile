FROM openjdk:8u151-jdk-alpine3.7
MAINTAINER Lubos Kozmon <lubosh91@gmail.com>

# Default config
ENV SERVER_HTTP_PORT=9000 \
    SESSION_TTL_MILLIS=900000 \
    ZK_CLIENT_TTL_MILLIS=5000 \
    ZK_CONNECT_TIMEOUT_MILLIS=5000

# Copy setup files
WORKDIR /setup
COPY . .

# Build project
RUN apk --no-cache --repository http://dl-3.alpinelinux.org/alpine/edge/testing/ add sbt=1.1.4-r0 \
  && sbt play/stage \
  # Copy app files
  && cp -r docker/files/. / \
  && cp -r play/target/universal/stage/. /app \
  && echo $SOURCE_BRANCH > /app/.version \
  # Make scripts executable
  && chmod +x \
    /app/run.sh \
    /app/healthcheck.sh \
  # Clean up
  && rm -rf \
    /setup \
    ~/.sbt \
    ~/.ivy2

WORKDIR /app

# Add health check
HEALTHCHECK --interval=5m --timeout=3s \
    CMD ./healthcheck.sh

# Server HTTP port
EXPOSE 9000

CMD ["./run.sh"]
