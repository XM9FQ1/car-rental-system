#!/bin/sh
# Builds the native JavaFX desktop application (Car Rental.app + .dmg) into target/dist
set -e
cd "$(dirname "$0")"
./mvnw -q -Pfx clean package -DskipTests
rm -rf target/jpackage-input target/dist
mkdir -p target/jpackage-input
cp target/carrental-0.0.1-SNAPSHOT.jar target/jpackage-input/carrental.jar
jpackage --type dmg --name "Car Rental" --app-version 2.0.0 --input target/jpackage-input --main-jar carrental.jar --dest target/dist --java-options "--enable-native-access=ALL-UNNAMED"
echo "Done: target/dist"
ls -lh target/dist
