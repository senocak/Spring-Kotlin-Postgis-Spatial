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
  CURL http://localhost:8089/api/v1/city/1/100000
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
  },
  {
    "title": "MERSİN(İÇEL)",
    "lat": 36.80000000,
    "lng": 34.63333330,
    "id": "33",
    "northeastLat": 36.87827200,
    "northeastLng": 34.71670200,
    "southwestLat": 36.69950300,
    "southwestLng": 34.45765500
  },
  {
    "title": "OSMANİYE",
    "lat": 37.06805000,
    "lng": 36.26158900,
    "id": "80",
    "northeastLat": 37.10406100,
    "northeastLng": 36.28794100,
    "southwestLat": 37.04694000,
    "southwestLng": 36.20803400
  }
]
```
2. ### findNearestCities by lat and lng
```sh
  CURL http://localhost:8089/api/v1/city/39.9208/32.8541/84000
  ```
```json
[
  {
    "title": "ANKARA",
    "lat": 39.92077000,
    "lng": 32.85411000,
    "id": "6",
    "northeastLat": 40.10098100,
    "northeastLng": 33.02486600,
    "southwestLat": 39.72282100,
    "southwestLng": 32.49909700
  },
  {
    "title": "KIRIKKALE",
    "lat": 39.84682100,
    "lng": 33.51525100,
    "id": "71",
    "northeastLat": 39.87284300,
    "northeastLng": 33.59796800,
    "southwestLat": 39.81380200,
    "southwestLng": 33.46828900
  }
]
```
3. ### findNearestDistricts by district id
```sh
  CURL http://localhost:8089/api/v1/district/71/40000
  ```
```json
[
  {
    "city_id": {
      "title": "ANKARA",
      "lat": 39.92077000,
      "lng": 32.85411000,
      "id": "6",
      "northeastLat": 40.10098100,
      "northeastLng": 33.02486600,
      "southwestLat": 39.72282100,
      "southwestLng": 32.49909700
    },
    "title": "HAYMANA",
    "lat": 39.43606900,
    "lng": 32.49663900,
    "id": "71",
    "northeastLat": 39.44984300,
    "northeastLng": 32.52402400,
    "southwestLat": 39.42249400,
    "southwestLng": 32.49041900
  },
  {
    "city_id": {
      "title": "ANKARA",
      "lat": 39.92077000,
      "lng": 32.85411000,
      "id": "6",
      "northeastLat": 40.10098100,
      "northeastLng": 33.02486600,
      "southwestLat": 39.72282100,
      "southwestLng": 32.49909700
    },
    "title": "POLATLI",
    "lat": 39.58416670,
    "lng": 32.14722220,
    "id": "78",
    "northeastLat": 39.60536700,
    "northeastLng": 32.17215200,
    "southwestLat": 39.53243100,
    "southwestLng": 32.09856600
  }
]
```
4. ### findNearestDistricts by lat and lng
```sh
  CURL http://localhost:8089/api/v1/district/39.9208/32.8541/4000
```
```json
[
  {
    "city_id": {
      "title": "ANKARA",
      "lat": 39.92077000,
      "lng": 32.85411000,
      "id": "6",
      "northeastLat": 40.10098100,
      "northeastLng": 33.02486600,
      "southwestLat": 39.72282100,
      "southwestLng": 32.49909700
    },
    "title": "ALTINDAĞ",
    "lat": 39.92077000,
    "lng": 32.85411000,
    "id": "59",
    "northeastLat": 40.10098100,
    "northeastLng": 33.02486600,
    "southwestLat": 39.72282100,
    "southwestLng": 32.49909700
  },
  {
    "city_id": {
      "title": "ANKARA",
      "lat": 39.92077000,
      "lng": 32.85411000,
      "id": "6",
      "northeastLat": 40.10098100,
      "northeastLng": 33.02486600,
      "southwestLat": 39.72282100,
      "southwestLng": 32.49909700
    },
    "title": "GÖLBAŞI",
    "lat": 39.92077000,
    "lng": 32.85411000,
    "id": "69",
    "northeastLat": 40.10098100,
    "northeastLng": 33.02486600,
    "southwestLat": 39.72282100,
    "southwestLng": 32.49909700
  }
]
```