#!/bin/sh

curl -f http://localhost:${API_HTTP_PORT}/api/healthcheck || exit 1
