# Spring Boot Kotlin Spatial Search with PostGIS, Redis, and MongoDB

This project demonstrates location-based search capabilities using PostGIS and Redis Geo features, along with MongoDB document storage capabilities in a Spring Boot Kotlin application.

## Technologies
- Spring Boot 3.3.1
- Kotlin 1.9.24
- PostGIS 14-3.3
- Redis 7.0.5
- MongoDB 7.0
- Java 17

## Features
- Spatial search using PostGIS
- Geo-location search using Redis
- MongoDB geo-spatial queries and indexing
- Support for Cities and Districts
- Distance-based nearest location search
- Document storage with MongoDB

## Installation

### Prerequisites
- Docker and Docker Compose
- Java 17 or higher

### Setup
1. Start the required services:
```sh
docker-compose up -d
```

2. Install PostGIS extension inside PostgreSQL container:
```postgresql
CREATE EXTENSION postgis;
```

### Configuration
Default configuration values:
```properties
# PostgreSQL
SERVER_IP=localhost
POSTGRESQL_PORT=54321
POSTGRESQL_DB=boilerplate
POSTGRESQL_SCHEMA=public
POSTGRESQL_USER=postgres
POSTGRESQL_PASSWORD=senocak

# MongoDB
MONGO_PORT=27017
MONGO_USER=anil
MONGO_DB=boilerplate
MONGO_PASSWORD=senocak

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=senocak
```

## API Documentation

### PostGIS Endpoints

#### Cities
- **List All Cities**
  ```
  GET /api/v1/postgis/cities
  ```

- **Create City**
  ```
  POST /api/v1/postgis/city?lng=<longitude>&lat=<latitude>&title=<cityName>
  ```

- **Find Nearest Cities by Coordinates**
  ```
  GET /api/v1/postgis/city/{lat}/{lng}/{distance}
  ```

- **Find Nearest Cities by City ID**
  ```
  GET /api/v1/postgis/city/{id}/{distance}
  ```

#### Districts
- **List All Districts**
  ```
  GET /api/v1/postgis/districts
  ```

- **Find Nearest Districts by Coordinates**
  ```
  GET /api/v1/postgis/district/{lat}/{lng}/{distance}
  ```

- **Find Nearest Districts by District ID**
  ```
  GET /api/v1/postgis/district/{id}/{distance}
  ```

### Redis Geo Endpoints

#### Cities
- **List All Cities**
  ```
  GET /api/v1/redis/cities
  ```

#### Districts
- **List All Districts**
  ```
  GET /api/v1/redis/districts
  ```

#### Geo Search
- **Find Nearest Locations (Jedis)**
  ```
  GET /api/v1/redis/{type}/{lat}/{lng}/{distance}
  ```
  - type: "cities" or "districts"

- **Find Nearest Locations (GeoOperations)**
  ```
  GET /api/v1/redis/{lat}/{lng}/{distance}
  ```

### MongoDB Endpoints

#### Cities
- **List All Cities**
  ```
  GET /api/v1/mongo/cities
  ```

- **Find Nearest Cities by Coordinates**
  ```
  GET /api/v1/mongo/city/{lat}/{lng}/{distance}
  ```

- **Find Nearest Cities by City ID**
  ```
  GET /api/v1/mongo/city/{id}/{distance}
  ```

#### Districts
- **List All Districts**
  ```
  GET /api/v1/mongo/districts
  ```

- **Find Nearest Districts by Coordinates**
  ```
  GET /api/v1/mongo/district/{lat}/{lng}/{distance}
  ```

- **Find Nearest Districts by District ID**
  ```
  GET /api/v1/mongo/district/{id}/{distance}
  ```

## Example Responses

### MongoDB Find Nearest Cities
```sh
curl http://localhost:8089/api/v1/mongo/city/39.9208/32.8541/84000
```

```json
[
  {
    "id": "6",
    "title": "ANKARA",
    "location": {
      "type": "Point",
      "coordinates": [32.85411, 39.92077]
    },
    "bounds": {
      "northeastLat": 40.076332,
      "northeastLng": 33.007912,
      "southwestLat": 39.795617,
      "southwestLng": 32.583389
    }
  }
]
```

### PostGIS Find Nearest Cities
```sh
curl http://localhost:8089/api/v1/postgis/city/39.9208/32.8541/84000
```

```json
[
  {
    "title": "ANKARA",
    "lat": 39.92077000,
    "lng": 32.85411000,
    "id": "6",
    "northeastLat": 40.076332,
    "northeastLng": 33.007912,
    "southwestLat": 39.795617,
    "southwestLng": 32.583389
  }
]
```

### Find Nearest Cities by ID
```sh
curl http://localhost:8089/api/v1/postgis/city/1/100000
```

```json
[
  {
    "title": "ADANA",
    "lat": 37.00000000,
    "lng": 35.32133330,
    "id": "1",
    "northeastLat": 37.07200400,
    "northeastLng": 35.46199500,
    "southwestLat": 36.93552300,
    "southwestLng": 35.17470600
  }
]
```

## Contributing
Feel free to open issues and pull requests for any improvements.

## License
This project is open source and available under the MIT License.
