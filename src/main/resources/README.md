# Loan Management System

**Overview**
This is a Spring Boot-based Loan Management System designed for banks to manage customer loans. The system allows bank employees to create, list, and process loan payments while enforcing business rules and security constraints.

**Features**
Loan Creation: Create loans with validation for credit limits, interest rates, and installment counts

Loan Listing: View loans by customer with customerId

Payment Processing: Pay loan installments with early payment discounts and late payment penalties

Role-based Access Control: Different permissions for ADMIN and CUSTOMER roles

Data Persistence: H2 in-memory database with automatic schema generation

Distributed Locking: Redisson-based locking for concurrent operations

API Documentation: OpenAPI/Swagger documentation

**Technologies**
Java 17

Spring Boot 3.4.5

Spring Security

JWT Authentication

H2 Database

MapStruct

Lombok

Redisson for redis

OpenAPI 3.0

**Installation**
Prerequisites
Min Java 17 JDK

Maven 3.8+

(Optional) Docker for Redis if not using embedded mode(docker-compose.yml is in the project)

**Steps**
1) After install project build project "mvn clean install"
2) If you have docker installed, run "docker-compose up" (redis must be installed)
3) Run the application "mvn spring-boot:run"

**API Documentation**
After starting the application, access the Swagger UI at: http://localhost:8080/swagger-ui.html

**Database Access**
H2 database url http://localhost:8080/h2-console
Connection details:
JDBC URL: jdbc:h2:mem:creditdb
Username: test
Password: test

**Initial Database Data**
**user1**
username: admin
password: admin
roleType: ADMIN

**user2**
username: customer
password: customer
roleType: CUSTOMER



**Business Rules**
Loan Creation:

Customer must have sufficient credit limit

Installment count must be 6, 9, 12, or 24

Interest rate must be between 0.1 and 0.5

Installments are calculated as equal payments on the first day of each month

Payments:

Payments must cover full installments

Only installments due within 3 months can be paid

Early payments get discounts (0.1% per day early)

Late payments incur penalties (0.1% per day late)