#!/bin/sh
target="$0"
while [ -L "$target" ]; do
    link=$(readlink "$target") || exit 1
    case "$link" in
        /*) target="$link" ;;
        *)  target=$(dirname "$target")/"$link" ;;
    esac
done
DIR=$(cd "$(dirname "$target")" && pwd)
exec java -jar "$DIR/@JAR@" "$@"
