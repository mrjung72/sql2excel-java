#!/bin/sh
JAR=$(ls -1 "$(dirname "$0")"/*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
    echo "No jar file found." >&2
    exit 1
fi
exec java -jar "$JAR" "$@"
