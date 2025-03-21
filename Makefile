.PHONY: all clean build test install integrationTests

all: clean test

clean:
	./mvnw clean

build:
	./mvnw compile

test:
	./mvnw test

install:
	./mvnw install

# set the SENTRY_AUTH_TOKEN environment variable to a valid auth token and possibly change
# the org and project slugs in `src/test/java/io/sentry/integration/uploadSourceBundle/PomUtils.kt`
# to successfully run the integration tests that use `sentry-cli`
integrationTests: install
	./mvnw test -Dtest=*IT

