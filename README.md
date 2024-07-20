##  Location Based Search via PostGIS

### Installation
```sh
docker-compose up -d
```
### Default values
- `SERVER_IP=localhost`
- `POSTGRESQL_PORT=54321`
- `POSTGRESQL_DB=boilerplate`
- `POSTGRESQL_SCHEMA=public`
- `POSTGRESQL_USER=postgres`
- `POSTGRESQL_PASSWORD=senocak`


### CURLs

1. ### findNearestCities by city id
```sh
  CURL http://localhost:8089/api/v1/city/1/300000
  ```
2. ### findNearestCities by lat and lng
```sh
  CURL http://localhost:8089/api/v1/city/39.9208/32.8541/84000
  ```
3. ### findNearestDistricts by district id
```sh
  CURL http://localhost:8089/api/v1/district/71/40000
  ```
4. ### findNearestDistricts by lat and lng
```sh
  CURL http://localhost:8089/api/v1/district/39.9208/32.8541/4000
  ```