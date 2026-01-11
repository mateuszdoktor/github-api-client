# GitHub Repositories API

A simple Spring Boot application that acts as a proxy for GitHub API, allowing you to list non-fork repositories for a given user with their branches information.

## Requirements

- Java 25
- Gradle

## Running the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`.

## API Endpoints

### Get User Repositories

```
GET /api/users/{username}/repositories
```

Returns all non-fork repositories for the specified GitHub user, including branch information.

#### Response

```json
[
  {
    "name": "repository-name",
    "owner": {
      "login": "username"
    },
    "fork": false,
    "branches": [
      {
        "name": "main",
        "commit": {
          "sha": "abc123def456"
        }
      }
    ]
  }
]
```

#### Error Response (User Not Found)

```json
{
  "status": 404,
  "message": "User not found: username"
}
```

## Running Tests

```bash
./gradlew test
```

## Technology Stack

- Java 25
- Spring Boot 4.0.1
- Spring WebMVC
- Spring RestClient
- WireMock (for integration tests)
