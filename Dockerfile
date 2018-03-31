FROM hseeberger/scala-sbt:8u151-2.12.4-1.0.2 as sbt
MAINTAINER Lubos Kozmon <lubosh91@gmail.com>

# Make stage files
WORKDIR /app
COPY . .
RUN sbt play/stage

FROM openjdk:8

ARG ZOONAV_VERSION
ENV ZOONAV_VERSION=$ZOONAV_VERSION

# Default config
ENV SERVER_HTTP_PORT=9000 \
    SESSION_TTL_MILLIS=900000 \
    ZK_CLIENT_TTL_MILLIS=5000 \
    ZK_CONNECT_TIMEOUT_MILLIS=5000

# Server HTTP port
EXPOSE 9000

# Copy setup files
COPY ./docker/copy /

RUN chmod +x \
    /app/run.sh \
    /app/healthcheck.sh

# Add health check
HEALTHCHECK --interval=5m --timeout=3s \
    CMD /app/healthcheck.sh

# Copy stage files
COPY --from=sbt /app/play/target/universal/stage /app

CMD ["/app/run.sh"]
