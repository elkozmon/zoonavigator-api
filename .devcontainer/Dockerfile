ARG SCALA_SHORT_VERSION="2.13"
ARG SCALA_VERSION="${SCALA_SHORT_VERSION}.2"
ARG SBT_VERSION="1.3.12"
ARG AMMONITE_VERSION="2.1.4"

FROM openjdk:11

# Install Scala

ARG SCALA_VERSION

RUN mkdir /usr/local/scala/ \
  && curl -sL https://downloads.lightbend.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz \
    | tar xfz - -C /usr/local/scala/

ENV SCALA_HOME /usr/local/scala/scala-$SCALA_VERSION
ENV PATH "$SCALA_HOME/bin:$PATH"

# Install SBT

ARG SBT_VERSION

RUN mkdir /usr/local/sbt/ \
  && curl -sL https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz \
    | tar xfz - -C /usr/local/sbt/ --strip-components 1

ENV PATH "/usr/local/sbt/bin:$PATH"

# Install Ammonite

ARG AMMONITE_VERSION
ARG SCALA_SHORT_VERSION

RUN curl -sL -o /usr/local/bin/amm https://github.com/lihaoyi/Ammonite/releases/download/$AMMONITE_VERSION/$SCALA_SHORT_VERSION-$AMMONITE_VERSION \
  && chmod +x /usr/local/bin/amm