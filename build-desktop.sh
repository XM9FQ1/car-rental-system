#!/bin/bash
set -e
cd "$(dirname "$0")"
VERSION=3.0.0
./mvnw -q -Pfx clean package -DskipTests
rm -rf target/jpackage-input target/dist
mkdir -p target/jpackage-input
cp target/carrental-0.0.1-SNAPSHOT.jar target/jpackage-input/carrental.jar
jpackage --type dmg \
  --name "Car Rental" \
  --app-version "$VERSION" \
  --vendor "NYE BAI0168 team" \
  --description "Car Rental System - desktop application" \
  --input target/jpackage-input \
  --main-jar carrental.jar \
  --dest target/dist \
  --java-options "--enable-native-access=ALL-UNNAMED"
echo "Ready: target/dist/Car Rental-$VERSION.dmg"
