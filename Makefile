.PHONY: all clean build test integrationTests

all: clean test

clean:
	./mvnw clean

build:
	./mvnw compile

test:
	./mvnw test

integrationTests:
	./mvnw test -Dtest=*IT

