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

integrationTests: install
	./mvnw test -Dtest=*IT

