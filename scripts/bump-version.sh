#!/bin/bash

# ./scripts/bump-version.sh <old version> <new version>
# eg ./scripts/bump-version.sh "6.0.0-alpha.1" "6.0.0-alpha.2"

set -eux

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR/..

OLD_VERSION="$1"
NEW_VERSION="$2"

MAVEN_FILEPATH="pom.xml"

# Replace <version> with the given version
VERSION_PATTERN="^  <version>.*<\/version>.*$"
perl -pi -e "s/$VERSION_PATTERN/  <version>$NEW_VERSION<\/version>/g" $MAVEN_FILEPATH
