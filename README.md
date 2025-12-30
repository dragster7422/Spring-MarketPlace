# Spring MarketPlace

A full-featured marketplace application built with Java and Spring Boot, allowing users to buy and sell products with advanced search capabilities powered by Elasticsearch.

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Notes](#notes)

## ✨ Features

### User Management
- User registration and authentication
- Secure password encryption with BCrypt
- Profile management (edit: username, email, password)
- Account deletion with cascade cleanup
- Role-based access control (USER, ADMIN)

### Product Management
- Create, read, update, and delete products
- Multiple image uploads per product (preview + additional images)
- Product search with Elasticsearch integration
- Pagination for product listings
- Price management with decimal precision

### Admin Panel
- User management dashboard
- Ban/unban users
- Role assignment (add/remove ADMIN role)
- User deletion
- Search users by username or email
- Elasticsearch reindexing functionality

### Search & Discovery
- Full-text search across product titles and descriptions
- Elasticsearch-powered search with fallback to database
- Seller profile pages
- Product listings by seller

### Additional Features
- Form validation
- CSRF protection (default)

## 🛠 Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Security**
- **Spring Data JPA**
- **Hibernate**
- **PostgreSQL**
- **Elasticsearch 8.11.0**
- **Lombok**
- **Jakarta Validation**

### Frontend
- **Thymeleaf** - Server-side template engine
- **Bootstrap 5.3.3** - CSS framework
- **HTML5/CSS3**

### DevOps & Tools
- **Docker & Docker Compose**
- **Maven**
- **Kibana 8.11.0** - Elasticsearch visualization

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **H2 Database** - In-memory database for tests

## 📦 Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose** (for running PostgreSQL and Elasticsearch)
- **Git**

## 🚀 Installation

### 1. Create DB in PostgreSQL

`spring_marketplace`

### 2. Clone the Repository

```bash
git clone https://github.com/dragster7422/Spring-MarketPlace.git
cd Spring-MarketPlace
```

### 3. Start Docker Services

Start PostgreSQL and Elasticsearch using Docker Compose:

```bash
docker compose up -d
```

### 4. Configure Database Connection

Create an `application-local.properties` file in `src/main/resources/` and set the password to match your PostgreSQL server:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/spring_marketplace
spring.datasource.username=postgres
spring.datasource.password=
```

This will start:
- PostgreSQL on port `5432`
- Elasticsearch on port `9200`
- Kibana on port `5601`

### 5. Build the Project

```bash
mvn clean install
```

### 6. Run the Application

```bash
mvn spring-boot:run
```

### Accessing the Application

- **Main Application**: http://localhost:8080
- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601

### Default Admin Account

- First user registered can be promoted to admin manually in database

## 📁 Project Structure

```
spring-marketplace/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── configurations/     # Security and web configurations
│   │   │   ├── controllers/        
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   ├── elasticsearch/      # Elasticsearch documents
│   │   │   ├── models/             # JPA entities
│   │   │   │   └── enums/          # Enumerations
│   │   │   ├── repositories/       # JPA and Elasticsearch repositories
│   │   │   └── services/           # Business logic
│   │   └── resources/
│   │       ├── static/css/         # Stylesheets
│   │       ├── templates/          # Thymeleaf templates
│   │       ├── application.properties
│   │       └── application-test.properties
│   └── test/                       # Test files
├── uploads/                        # Product images (auto-created)
├── docker-compose.yml              # Docker services
├── pom.xml                         # Maven configuration
└── README.md
```

## 🔌 API Endpoints

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page with product listings |
| GET | `/product/{id}` | Product details |
| GET | `/seller/{id}` | Seller profile and products |
| GET | `/login` | Login page |
| GET | `/register` | Registration page |
| POST | `/register` | User registration |

### Authenticated Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile` | User profile |
| GET | `/profile/edit` | Edit profile page |
| POST | `/profile/update` | Update profile |
| POST | `/profile/delete` | Delete account |
| GET | `/profile/products` | User's products |
| GET | `/product/add` | Add product page |
| POST | `/product/add` | Create product |
| GET | `/product/{id}/edit` | Edit product page |
| POST | `/product/{id}/edit` | Update product |
| POST | `/product/{id}/delete` | Delete product |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/profile/admin/dashboard` | Admin dashboard |
| POST | `/profile/admin/dashboard/user/{id}/toggle-ban` | Ban/unban user |
| POST | `/profile/admin/dashboard/user/{id}/add-role` | Add role to user |
| POST | `/profile/admin/dashboard/user/{id}/remove-role` | Remove role from user |
| POST | `/profile/admin/dashboard/user/{id}/delete` | Delete user |
| POST | `/profile/admin/search/reindex` | Reindex all products in Elasticsearch |

## 🧪 Testing

The project includes comprehensive unit and integration tests.

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=UserServiceTest
```

### Test Configuration

Tests use H2 in-memory database and have Elasticsearch disabled. Configuration is in `application-test.properties`.

## 📝 Notes

- Images are stored locally in the `uploads/` directory
- First user registered can be promoted to admin manually in database
- Elasticsearch indexing happens automatically on product creation/update/deletion

## 👤 Author

Danil - [GitHub Profile](https://github.com/dragster7422)