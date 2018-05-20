ARG SBT_VERSION=1.1.5
ARG SBT_GPG_KEY=99E82A75642AC823

FROM openjdk:8u151-jdk-alpine3.7
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

CMD ["sbt", "test"]
