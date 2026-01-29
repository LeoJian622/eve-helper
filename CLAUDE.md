# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EVE Helper is a Java Spring Boot application for EVE Online players to read and analyze character/corporation orders, assets, and market data. The project integrates with the EVE Online ESI (EVE Swagger Interface) API and has been migrated to Domain-Driven Design (DDD) architecture.

**Current Branch**: `ddd-migration` - The DDD architecture migration has been finalized (0.0.2-SNAPSHOT).

## Build and Development Commands

### Build and Run
```bash
# Build the project
mvn clean package

# Run the application (dev profile active by default)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

### Development
```bash
# Compile only
mvn compile

# Clean build artifacts
mvn clean

# Skip tests during build
mvn package -DskipTests

# Generate MapStruct mappers (happens automatically during compile)
mvn compile
```

### Access Points
- **Application**: http://localhost:9999
- **Swagger UI**: http://localhost:9999/swagger-ui.html
- **API Docs**: http://localhost:9999/v3/api-docs

## Architecture Overview

### DDD Layered Architecture

The project follows Domain-Driven Design with four distinct layers:

```
xyz.foolcat.eve.evehelper/
├── domain/          # Domain Layer - Core business logic (no dependencies)
├── application/     # Application Layer - Use case coordination
├── infrastructure/  # Infrastructure Layer - Technical implementation
├── interfaces/      # Interface Layer - User interaction (REST, WebSocket)
└── shared/          # Shared Layer - Common components
```

**Dependency Rule**: `Interfaces → Application → Domain ← Infrastructure`

### Domain Layer (`domain/`)

Contains the core business logic and domain models:

- **`model/entity/`**: Domain entities with unique identifiers
  - `eve/`: EVE Online game data entities (IndustryBlueprints, IndustryActivityMaterials, etc.)
  - `system/`: Application entities (Blueprints, Assets, EveAccount, SysUser, etc.)
- **`model/valueobject/`**: Immutable value objects
- **`model/aggregate/`**: Aggregate roots managing entity clusters
- **`repository/`**: Repository interfaces (implementations in infrastructure)
  - `eve/`: EVE game data repositories
  - `system/`: Application data repositories
- **`service/`**: Domain services for business logic
  - `esi/`: ESI API integration services
  - `eve/`: EVE game data services
  - `system/`: System business logic services
  - `thread/`: Async operation services (e.g., MarketOrderAsyncService)
- **`specification/`**: Business rule specifications (e.g., BlueprintsSpecification)

### Application Layer (`application/`)

Orchestrates use cases and coordinates domain objects:

- **`service/`**: Application services (e.g., BlueprintsApplicationService)
- **`dto/`**: Data Transfer Objects for inter-layer communication
- **`assembler/`**: MapStruct assemblers for object mapping (26+ assemblers)
  - `eve/`: EVE data assemblers
  - `system/`: System data assemblers
- **`command/`**: Command handlers (CQRS write operations)
- **`query/`**: Query handlers (CQRS read operations, e.g., BlueprintsQueryHandler)
- **`validator/`**: Input validation logic

### Infrastructure Layer (`infrastructure/`)

Technical implementations and external integrations:

- **`persistence/`**: Database access implementation
  - `mapper/`: MyBatis mappers (26+ XML mappers)
    - `eve/`: EVE game data mappers
    - `system/`: Application data mappers
  - `repository/`: Repository implementations
  - `entity/`: Persistence Objects (PO) - database entities
- **`external/`**: External service integrations
  - `esi/`: EVE Online ESI API client (30+ API classes)
    - OAuth 2.0 with PKCE authentication
    - Support for Serenity (Chinese) and Tranquility servers
  - `onebot/`: OneBot messaging integration
- **`config/`**: Configuration classes
  - `druid/`: Database connection pool configuration
  - `mybatis/`: MyBatis Plus configuration
  - `security/`: Spring Security configuration

### Interfaces Layer (`interfaces/`)

User-facing interfaces:

- **`web/controller/`**: REST controllers (9 controllers)
  - BlueprintsController, AssetsController, CharacterController
  - JobController, MarketController, MiningController
  - ObserverController, UserController
- **`web/filter/`**: Web filters (e.g., JwtAuthorizationTokenFilter)
- **`web/advice/`**: Global exception handling
- **`web/vo/`**: View Objects for API responses
- **`websocket/`**: WebSocket endpoints

### Shared Layer (`shared/`)

Common components used across all layers:

- **`kernel/`**: Core components
  - `base/`: Base classes
  - `enums/`: Enumerations
  - `exception/`: Exception definitions
  - `constants/`: Constants
  - `annotation/`: Custom annotations
- **`result/`**: Unified result wrapper
- **`util/`**: Common utilities

## Key Technical Details

### Multi-Database Configuration

Two separate MySQL databases:
- **`eve`**: EVE Online static game data (read-only reference data)
- **`eve_helper`**: Application runtime data (user data, orders, assets)

MyBatis Plus is configured with separate mappers for each database.

### Authentication & Authorization

**JWT Authentication**:
- RSA key pair signing (stored in `eve-jwt.jks`)
- Token expiration: 7200 seconds (2 hours)
- Custom filter: `JwtAuthorizationTokenFilter`
- Token format: `Bearer <token>`

**OAuth 2.0 (ESI Integration)**:
- Authorization Code flow with PKCE
- Refresh tokens stored in database
- Access tokens cached in Redis (19-minute TTL)
- Extensive scopes for character and corporation data

**RBAC Authorization**:
- `RbacAuthorizationManager`: Custom authorization manager
- URL-permission-role mapping cached in Redis
- RESTful permission format: `METHOD:PATH`
- White list for public endpoints configured in `application-dev.yml`

### ESI API Integration

Located in `infrastructure/external/esi/`:

- **30+ specialized API classes**: CharacterApi, CorporationApi, MarketApi, IndustryApi, AssetsApi, WalletApi, etc.
- **OAuth flow**: `AuthorizeOAuth` handles token generation and refresh
- **Error handling**: Custom `EsiException` for API errors
- **Caching**: Character/corporation info cached in Redis
- **Automatic token refresh**: Transparent to application layer

### MapStruct Object Mapping

26+ assemblers handle conversions between:
- Domain Entity ↔ Persistence Object (PO)
- DTO ↔ View Object (VO)
- Domain Entity ↔ DTO

Assemblers are automatically generated during compilation via annotation processing.

### Caching Strategy

- **Redis**: Primary cache store
- **TTL**: 3000 seconds default
- **Cached data**:
  - Permission-role mappings
  - ESI access tokens
  - Character/corporation information

### Async Processing

- `@EnableScheduling`: Scheduled tasks enabled
- `AsyncConfiguration`: Async method execution
- Thread pools for market order processing
- Domain service: `MarketOrderAsyncService`

## Development Guidelines

### Working with Domain Models

1. **Entities** are in `domain/model/entity/` (eve/ and system/)
2. **Repository interfaces** are in `domain/repository/` (implementations in infrastructure)
3. **Domain services** contain business logic that doesn't fit in entities
4. Never let domain layer depend on infrastructure or application layers

### Adding New Features

1. Start with domain model (entities, value objects, aggregates)
2. Define repository interfaces in domain layer
3. Create application service to orchestrate use case
4. Implement repository in infrastructure layer
5. Create MapStruct assembler for object mapping
6. Add controller in interfaces layer
7. Write MyBatis mapper XML for database queries

### CQRS Pattern

- **Commands**: Write operations (create, update, delete)
- **Queries**: Read operations with pagination support
- Query handlers in `application/query/` (e.g., BlueprintsQueryHandler)
- Separate models for reads and writes when beneficial

### Security Considerations

- All endpoints require JWT authentication except those in white list
- RBAC checks performed by `RbacAuthorizationManager`
- ESI tokens are sensitive - stored encrypted in database
- Never commit credentials or tokens to repository

### Database Migrations

- MyBatis Plus handles basic CRUD operations
- Custom queries in mapper XML files
- Use `MyMetaObjectHandler` for automatic timestamp fields
- Batch operations available for bulk insert/update

## Important Files

- **`pom.xml`**: Maven dependencies and build configuration
- **`src/main/resources/application.yml`**: Profile selection (active: dev)
- **`src/main/resources/application-dev.yml`**: Development configuration
- **`DDD_ARCHITECTURE_MIGRATION.md`**: DDD migration guide and mapping
- **`DDD_MIGRATION_PLAN.md`**: Migration plan and status
- **`eve-jwt.jks`**: JWT signing key (not in repository)

## Technology Stack

- **Java 11**: Language version
- **Spring Boot 2.7.18**: Core framework
- **Spring Security**: Authentication and authorization
- **Spring WebFlux**: Reactive HTTP client for ESI
- **MyBatis Plus 3.5.4**: ORM with pagination
- **Druid 1.2.8**: Database connection pooling
- **MySQL 8.0.28**: Database
- **Redis**: Caching and session storage
- **Lombok 1.18.22**: Boilerplate reduction
- **MapStruct 1.5.5**: Object mapping
- **Hutool 5.8.25**: Java utility library
- **SpringDoc OpenAPI 1.7.0**: API documentation

## Common Patterns

### Repository Pattern
```java
// Interface in domain/repository/
public interface BlueprintsRepository {
    Blueprints save(Blueprints blueprints);
}

// Implementation in infrastructure/persistence/repository/
public class BlueprintsRepositoryImpl implements BlueprintsRepository {
    // Uses MyBatis mapper and assembler
}
```

### Specification Pattern
```java
// In domain/specification/
public class BlueprintsSpecification {
    public boolean isSatisfiedBy(Blueprints blueprints) {
        // Business rule validation
    }
}
```

### Application Service Pattern
```java
// In application/service/
public class BlueprintsApplicationService {
    // Orchestrates domain objects and repositories
    // Handles transactions
    // Returns DTOs
}
```

## ESI OAuth Scopes

The application requires extensive ESI scopes for both character and corporation data. See README.md for the complete list of required scopes including:
- Character: assets, blueprints, wallet, market orders, industry jobs, etc.
- Corporation: assets, contracts, industry jobs, market orders, wallets, etc.

## Notes

- The project is actively maintained and follows DDD principles
- All Chinese comments and documentation are intentional (target audience)
- The codebase uses both English and Chinese naming conventions
- MapStruct processors must run during compilation for assemblers to work
- Redis must be running for the application to start
- Both MySQL databases (eve and eve_helper) must be accessible
