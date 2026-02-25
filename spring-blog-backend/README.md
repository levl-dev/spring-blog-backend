# Spring Blog Backend

REST API for a simple blog application built with Spring Framework 6 (without Spring Boot).

## Features

- Create, update and delete posts
- Tag management (separate table)
- Comment management
- Post search and pagination
- Image upload/download for posts

## Tech Stack

- Java 21
- Spring Framework 6
- PostgreSQL 17
- H2 (for integration tests)
- Maven
- Tomcat

## Build

mvn clean package

WAR file will be generated in /target directory.

## Run

Deploy WAR to external servlet container (Tomcat).

Application base URL:
http://localhost:8080/{artifact-name}

## Tests

Run tests:
mvn test