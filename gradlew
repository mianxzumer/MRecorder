#!/bin/sh

##############################################################################
# Gradle startup script for POSIX
##############################################################################

# Attempt to set APP_HOME
APP_HOME=$( cd "${BASH_SOURCE[0]%/*}" && pwd )

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"

chmod +x gradlew
