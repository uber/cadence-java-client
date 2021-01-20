# Developing cadence-java-client

This doc is intended for contributors to `cadence-java-client` (hopefully that's you!)

**Note:** All contributors also need to fill out the [Uber Contributor License Agreement](http://t.uber.com/cla) before we can merge in any of your changes

## Development Environment

* Java 8.
* Thrift 0.9.3
* Gradle build tool
* Docker

## Licence headers

This project is Open Source Software, and requires a header at the beginning of
all source files. To verify that all files contain the header execute:

```lang=bash
./gradlew licenseCheck
```

To generate licence headers execute

```lang=bash
./gradlew licenseFormat
```

## Commit Messages

Overcommit adds some requirements to your commit messages. At Uber, we follow the
[Chris Beams](http://chris.beams.io/posts/git-commit/) guide to writing git
commit messages. Read it, follow it, learn it, love it.

## Test

Testing and building cadence-java-client requires running cadence docker locally, execute:

```bash
curl -O https://raw.githubusercontent.com/uber/cadence/master/docker/docker-compose.yml
docker-compose up
```

(If this does not work, see instructions for running the Cadence Server at https://github.com/uber/cadence/blob/master/README.md.)

Then run all the tests with:

```bash
./gradlew test
```

The test by default will run with TestEnvironment without Cadence service. If you want to run with Cadence serivce:
```bash
USE_DOCKER_SERVICE=true ./gradlew test
```
And sometimes it's important to test the non-sticky mode 
```bash
STICKY_OFF=true USE_DOCKER_SERVICE=true ./gradlew test
```

Also, if there is any Buildkite test failure that you cannot reproduce locally, 
follow [buildkite docker-compose](./docker/buildkite/README.md) instructions to run the tests.

##  Build & Publish
Build with:

```bash
./gradlew build
```

To test locally, change `build.gradle`:
1. Comment out the whole `publications`  
2. Change 
```
group = 'com.uber.cadence'
```` 
to 
```
group = 'com.local.cadence'
``` 
Then run the command
```bash
./gradlew publishToMavenLocal
```
Now you can use the local cadence-java-client in your laptop.
