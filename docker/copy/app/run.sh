#!/bin/sh

# Remove pid file
rm -f "/app/RUNNING_PID"

# Generate random crypto secret if not defined
export CRYPTO_SECRET=${CRYPTO_SECRET:-$(cat /dev/urandom | tr -dc 'a-zA-Z0-9~!@#$%^&*_-' | fold -w 64 | head -n 1)}

# JVM config
JVM_CONFIG="-J-server"

[ -n "$JVM_XMS" ] && JVM_CONFIG="$JVM_CONFIG -J-Xms$JVM_XMS"
[ -n "$JVM_XMX" ] && JVM_CONFIG="$JVM_CONFIG -J-Xmx$JVM_XMX"

# Start application
/app/bin/zoonavigator-play ${JVM_CONFIG} &
APP_PID=$!

# Trap terminate signals
trap 'kill "$APP_PID"' TERM QUIT

# Wait for application to terminate
wait "$APP_PID"
