#!/bin/sh

# TODO Remove 'SERVER_HTTP_PORT' in 1.0.0
curl -f http://localhost:${SERVER_HTTP_PORT:-$API_HTTP_PORT}/api/healthcheck || exit 1
