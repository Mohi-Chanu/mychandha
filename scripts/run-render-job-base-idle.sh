#!/usr/bin/env sh
set -u

terminate=0
trap 'terminate=1' TERM INT

while [ "$terminate" -eq 0 ]; do
  sleep 30 &
  wait "$!" || true
done

exit 0
