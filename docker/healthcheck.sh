#!/bin/sh
set -eu
code=$(curl --silent --max-time 4 --output /dev/null --write-out '%{http_code}' "http://127.0.0.1:${PORT}/") || exit 1
[ "$code" = "401" ]
