#!/bin/sh

set -ex

# download cover files from all the tests
mkdir -p build/reports/jacoco/test
buildkite-agent artifact download "build/reports/jacoco/test/jacocoTestReport.xml" . --step ":java: Unit test with test services" --build "$BUILDKITE_BUILD_ID"

echo "download complete"

# report coverage
./gradlew coverallsJacoco

# cleanup
rm -rf ./build