package com.github.senocak.sks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<SpringKotlinSpatialApplication>(*args)
}

@SpringBootApplication
class SpringKotlinSpatialApplication

/*
@RestController
@RequestMapping("/api/v1")
@SpringBootApplication
class SpringKotlinSpatialApplication(
    private val cityRepository: CityRepository,
    private val districtRepository: DistrictRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val geoOperations: GeoOperations<String, Any>,
    private val jedisPool: JedisPool
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()
    private val vehicleLocationsCity: String = "vehicle_location_city"
    private val vehicleLocationsDistrict: String = "vehicle_location_district"
    private val connection: RedisConnection? = redisTemplate.connectionFactory?.connection

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        connection?.serverCommands()?.flushDb()
        connection?.serverCommands()?.flushAll()

        cityRepository.findAll()
        .map { city: City ->
            if (city.location == null)
                city.location = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
            //add(latitude = city.lat.toDouble(), longitude = city.lng.toDouble(), vehicleName = city.title)
            jedisPool.resource.use {
                it.geoadd(vehicleLocationsCity, city.lng.toDouble(), city.lat.toDouble(), city.title);
            }
            city
        }
        .run { cityRepository.saveAll(this) }
        districtRepository.findAll()
            .map { district: District ->
                if (district.location == null)
                    district.location = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
                add(latitude = district.lat.toDouble(), longitude = district.lng.toDouble(), vehicleName = district.title)
                jedisPool.resource.use {
                    it.geoadd(vehicleLocationsDistrict, district.lng.toDouble(), district.lat.toDouble(), district.title);
                }
                district
            }
            .run { districtRepository.saveAll(this) }
    }

    @PostMapping("city")
    fun create(@RequestParam lng: Double, @RequestParam lat: Double, @RequestParam title: String): City {
        val city = City(title = title, lat = lat.toBigDecimal(), lng = lng.toBigDecimal())
        city.location = geometryFactory.createPoint(Coordinate(lng, lat))
        return cityRepository.save(city)
    }

    @GetMapping("/city/{lat}/{lng}/{distance}")
    fun findNearestCities(
        @PathVariable lat: Float,
        @PathVariable lng: Float,
        @PathVariable distance: Int
    ): Iterable<City> {
        val point: Point = geometryFactory.createPoint(Coordinate(lng.toDouble(), lat.toDouble()))
        return cityRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/city/{id}/{distance}")
    fun findNearestCitiesByCityId(
        @PathVariable id: String,
        @PathVariable distance: Int
    ): Iterable<City> {
        val city: City = cityRepository.findById(id).orElseThrow { RuntimeException("City with id $id not found") }
        val point: Point = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
        return cityRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/district/{lat}/{lng}/{distance}")
    fun findNearestDistricts(
        @PathVariable lat: Float,
        @PathVariable lng: Float,
        @PathVariable distance: Int
    ): Iterable<District> {
        val point: Point = geometryFactory.createPoint(Coordinate(lng.toDouble(), lat.toDouble()))
        return districtRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/district/{id}/{distance}")
    fun findNearestDistrictsByDistrictId(
        @PathVariable id: String,
        @PathVariable distance: Int
    ): Iterable<District> {
        val district: District = districtRepository.findById(id).orElseThrow { RuntimeException("District with id $id not found") }
        val point: Point = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
        return districtRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/vehicle/cities")
    fun getAllVehicleLocationsForCities(): MutableCollection<String> {
        /*
        TODO: following not working
        val redisKeys: MutableSet<ByteArray>? = connection?.keyCommands()?.keys("$vehicleLocationsCity*".toByteArray())
        val keysList: MutableList<String> = ArrayList()
        if (!redisKeys.isNullOrEmpty()) {
            val it: Iterator<ByteArray> = redisKeys.iterator()
            while (it.hasNext()) {
                val data: ByteArray = it.next()
                keysList.add(String(data, 0, data.size))
            }
        }
        return keysList
         */
        val cities: MutableSet<String>
        jedisPool.resource.use {
            cities = it.zrange(vehicleLocationsCity, 0, -1)
        }
        return cities
    }

    @GetMapping("/vehicle/districts")
    fun getAllVehicleLocationsForDistricts(): MutableSet<String> {
        val districts: MutableSet<String>
        jedisPool.resource.use { it: Jedis ->
            districts = it.zrange(vehicleLocationsDistrict, 0, -1)
        }
        return districts
    }

    @GetMapping("/vehicle/{type}/{lat}/{lng}/{distance}")
    fun findNearestVehicles(
        @PathVariable type: String,
        @PathVariable lat: Double,
        @PathVariable lng: Double,
        @PathVariable distance: Double
    ): MutableList<GeoRadiusResponse> {
        val georadius: MutableList<GeoRadiusResponse>
        jedisPool.resource.use { it: Jedis ->
            georadius = it.georadius(if (type == "districts") vehicleLocationsDistrict else vehicleLocationsCity,
                lng, lat, distance, GeoUnit.KM)
        }
        return georadius
    }

    @GetMapping("/vehicle/{lat}/{lng}/{distance}")
    fun findNearestVehiclesByGeoOperations(
        @PathVariable lat: Double,
        @PathVariable lng: Double,
        @PathVariable distance: Double
    ): List<VehicleLocation> = findNearestVehicles(longitude = lng, latitude = lat, km = distance)

    private fun findNearestVehicles(longitude: Double, latitude: Double, km: Double): List<VehicleLocation> {
        val args: RedisGeoCommands.GeoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeCoordinates().includeDistance().sortAscending().limit(10)

        val circle = Circle(org.springframework.data.geo.Point(longitude, latitude), Distance(km, Metrics.KILOMETERS))
        val response: GeoResults<RedisGeoCommands.GeoLocation<Any>>? = geoOperations.radius(vehicleLocationsDistrict, circle, args)

        val vehicleLocationResponses: MutableList<VehicleLocation> = arrayListOf()
        response?.content?.forEach { data: GeoResult<RedisGeoCommands.GeoLocation<Any>> ->
            vehicleLocationResponses.add(element =
            VehicleLocation(name = "${data.content.name}", latitude = data.content.point.y, longitude = data.content.point.x)
                .also { it.averageDistance =  data.distance}
                .also { it.hash =  geoOperations.hash(vehicleLocationsDistrict, data.content.name)!!.stream().findFirst().get()}
            )
        }
        return vehicleLocationResponses
    }

    private fun add(latitude: Double, longitude: Double, vehicleName: String): Long? {
        val point: org.springframework.data.geo.Point = org.springframework.data.geo.Point(longitude, latitude)
        return geoOperations.add(vehicleLocationsDistrict, point, vehicleName)
    }
}


*/