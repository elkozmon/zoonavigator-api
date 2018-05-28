#!/bin/sh

# Handle deprecated config
# TODO Remove in 1.0.0
[ -n "$JVM_XMS" ] \
  && echo "'JVM_XMS' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'JAVA_XMS' instead." \
  && JAVA_XMS=$JVM_XMS

# TODO Remove in 1.0.0
[ -n "$JVM_XMX" ] \
  && echo "'JVM_XMX' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'JAVA_XMX' instead." \
  && JAVA_XMX=$JVM_XMX

# TODO Remove in 1.0.0
[ -n "$SERVER_HTTP_PORT" ] \
  && echo "'SERVER_HTTP_PORT' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'API_HTTP_PORT' instead." \
  && API_HTTP_PORT=$SERVER_HTTP_PORT

# TODO Remove in 1.0.0
[ -n "$APP_SECRET" ] \
  && echo "'APP_SECRET' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'API_SECRET' instead." \
  && API_SECRET=$APP_SECRET

# TODO Remove in 1.0.0
[ -n "$SESSION_TTL_MILLIS" ] \
  && echo "'SESSION_TTL_MILLIS' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'API_SESSION_TIMEOUT_MILLIS' instead." \
  && API_SESSION_TIMEOUT_MILLIS=$SESSION_TTL_MILLIS

# TODO Remove in 1.0.0
[ -n "$ZK_CLIENT_TTL_MILLIS" ] \
  && echo "'ZK_CLIENT_TTL_MILLIS' configuration variable is deprecated. It will be removed in version 1.0.0. Use 'ZK_CLIENT_TIMEOUT_MILLIS' instead." \
  && ZK_CLIENT_TIMEOUT_MILLIS=$ZK_CLIENT_TTL_MILLIS

# Remove pid file
rm -f ./RUNNING_PID

# Generate random app secret if not defined
export API_SECRET=${API_SECRET:-$(cat /dev/urandom | tr -dc 'a-zA-Z0-9~!@#$%^&*_-' | fold -w 64 | head -n 1)}

# Java config
JAVA_OPTS="$JAVA_OPTS \
  -J-XX:+UnlockExperimentalVMOptions \
  -J-XX:+UseCGroupMemoryLimitForHeap \
  -J-server \
  -J-Dzookeeper.kinit=/usr/bin/kinit"

[ -n "$JAVA_XMS" ] && JAVA_OPTS="$JAVA_OPTS -J-Xms$JAVA_XMS"
[ -n "$JAVA_XMX" ] && JAVA_OPTS="$JAVA_OPTS -J-Xmx$JAVA_XMX"

[ -n "$JAVA_JAAS_LOGIN_CONFIG" ] && JAVA_OPTS="$JAVA_OPTS -J-Djava.security.auth.login.config=$JAVA_JAAS_LOGIN_CONFIG"

[ -n "$JAVA_KRB5_DEBUG" ] && JAVA_OPTS="$JAVA_OPTS -J-Dsun.security.krb5.debug=$JAVA_KRB5_DEBUG"
[ -n "$JAVA_KRB5_REALM" ] && JAVA_OPTS="$JAVA_OPTS -J-Djava.security.krb5.realm=$JAVA_KRB5_REALM"
[ -n "$JAVA_KRB5_KDC" ] && JAVA_OPTS="$JAVA_OPTS -J-Djava.security.krb5.kdc=$JAVA_KRB5_KDC"

# ZooKeeper config
[ -n "$ZK_SASL_CLIENT" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.sasl.client=$ZK_SASL_CLIENT"
[ -n "$ZK_SASL_CLIENT_CONFIG" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.sasl.clientconfig=$ZK_SASL_CLIENT_CONFIG"
[ -n "$ZK_SASL_CLIENT_USERNAME" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.sasl.client.username=$ZK_SASL_CLIENT_USERNAME"
[ -n "$ZK_SERVER_REALM" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.server.realm=$ZK_SERVER_REALM"
[ -n "$ZK_CLIENT_SECURE" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.client.secure=$ZK_CLIENT_SECURE"
[ -n "$ZK_CLIENT_CNXN_SOCKET" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.clientCnxnSocket=$ZK_CLIENT_CNXN_SOCKET"
[ -n "$ZK_SSL_KEYSTORE_PATH" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.ssl.keyStore.location=$ZK_SSL_KEYSTORE_PATH"
[ -n "$ZK_SSL_KEYSTORE_PASSWORD" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.ssl.keyStore.password=$ZK_SSL_KEYSTORE_PASSWORD"
[ -n "$ZK_SSL_TRUSTSTORE_PATH" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.ssl.trustStore.location=$ZK_SSL_TRUSTSTORE_PATH"
[ -n "$ZK_SSL_TRUSTSTORE_PASSWORD" ] && JAVA_OPTS="$JAVA_OPTS -J-Dzookeeper.ssl.trustStore.password=$ZK_SSL_TRUSTSTORE_PASSWORD"

# Start application
./bin/zoonavigator-play ${JAVA_OPTS} &
APP_PID=$!

# Trap terminate signals
trap 'kill "$APP_PID"' TERM QUIT

# Wait for application to terminate
wait "$APP_PID"
