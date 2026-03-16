# Spring Blog Backend

REST API for a simple blog application built with Spring Boot.

## Features

- Create, update and delete posts
- Tag management (separate table)
- Comment management
- Post search and pagination
- Image upload/download for posts

## Tech Stack

- Java 21
- Spring Boot 3
- PostgreSQL 17
- H2 (for integration tests)
- Gradle
- Embedded Tomcat

## Build

./gradlew build

Executable JAR file will be generated in /build/libs directory.

## Run

Run application:

./gradlew bootRun

or run the generated JAR: java -jar build/libs/spring-blog-backend-1.0.jar

Application base URL:
http://localhost:8080

## Tests

Run tests:
./gradlew test