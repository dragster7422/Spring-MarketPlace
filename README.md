# Spring MarketPlace

A full-featured marketplace application built with Java and Spring Boot, allowing users to buy and sell products with advanced search capabilities powered by Elasticsearch.

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Notes](#notes)

## ✨ Features

### User Management
- User registration and authentication
- Secure password encryption with BCrypt
- Profile management (edit username, email, password)
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

## 🛠 Tech Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.5.6** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence layer
- **Hibernate** - ORM framework
- **PostgreSQL** - Relational database
- **Elasticsearch 8.11.0** - Search engine
- **Lombok** - Code generation for POJOs
- **Jakarta Validation** - Bean validation
- **Maven** - Build automation

### Frontend
- **Thymeleaf** - Server-side template engine
- **Bootstrap 5.3.3** - CSS framework
- **HTML5/CSS3** - Markup and styling

### DevOps & Tools
- **Docker & Docker Compose** - Containerization

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing

## 📝 Notes

- Images are stored locally in the `uploads/` directory
- First user registered can be promoted to admin manually in database
- Elasticsearch indexing happens automatically on product creation/update/deletion

## 👤 Author

Danil - [GitHub Profile](https://github.com/dragster7422)