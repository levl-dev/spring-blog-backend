# Spring Blog Backend

Backend part of blog application built with Spring Framework 6 (without Spring Boot).

## Tech Stack

- Java 21
- Spring Framework 6
- Spring Web MVC
- Spring JDBC
- PostgreSQL 17
- Maven
- Tomcat

## Build

mvn clean package

WAR file will be generated in /target directory.

## Run

Deploy generated WAR to external servlet container (Tomcat).

Application base URL:

http://localhost:8080/{artifact-name}

## Tests

mvn test