.PHONY: all clean build test install integrationTests

all: clean test integrationTests

clean:
	./mvnw clean

build:
	./mvnw compile

test:
	./mvnw test

install:
	./mvnw install

integrationTests: install
	test/integration-test-server-start.sh &
	SENTRY_URL=http://127.0.0.1:8000 ./mvnw clean verify -PintegrationTests
	curl -s http://127.0.0.1:8000/STOP || true
