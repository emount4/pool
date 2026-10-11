#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if ! command -v java >/dev/null 2>&1; then
  echo 'Install Java 21 and add java to PATH.' >&2
  exit 1
fi
if [ ! -f app/pool-desktop.jar ]; then
  exec ./mvnw javafx:run
fi
case "$(uname -s)-$(uname -m)" in
  Linux-x86_64) platform=linux ;;
  Darwin-arm64|Darwin-aarch64) platform=mac-aarch64 ;;
  Darwin-x86_64) platform=mac ;;
  *) echo 'Unsupported platform. Build from source using ./mvnw javafx:run.' >&2; exit 1 ;;
esac
exec java -Dfile.encoding=UTF-8 -cp "app/pool-desktop.jar:app/lib/*:app/native/$platform/*" ru.mirea.pool.Main
