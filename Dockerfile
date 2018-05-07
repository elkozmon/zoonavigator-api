FROM openjdk:8u151-jdk-alpine3.7 as sbt
MAINTAINER Lubos Kozmon <lubosh91@gmail.com>

# Copy source files
WORKDIR /src
COPY . .

# Install sbt
RUN apk \
    --no-cache \
    --repository http://dl-3.alpinelinux.org/alpine/edge/testing/ \
    add \
    sbt=1.1.4-r0

# Build project
RUN sbt play/stage

# Make scripts executable
RUN chmod +x \
    docker/files/app/run.sh \
    docker/files/app/healthcheck.sh

FROM openjdk:8u151-jdk-alpine3.7

# Default config
ENV SERVER_HTTP_PORT=9000 \
    SESSION_TTL_MILLIS=900000 \
    ZK_CLIENT_TTL_MILLIS=5000 \
    ZK_CONNECT_TIMEOUT_MILLIS=5000

# Copy app files
COPY --from=sbt /src/docker/files /
COPY --from=sbt /src/play/target/universal/stage /app

WORKDIR /app

# Install curl
RUN apk --no-cache add curl

# Add health check
HEALTHCHECK --interval=5m --timeout=3s \
    CMD ./healthcheck.sh

# Server HTTP port
EXPOSE 9000

CMD ["./run.sh"]
