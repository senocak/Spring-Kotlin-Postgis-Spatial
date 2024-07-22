package com.github.senocak.sks

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.data.geo.Circle
import org.springframework.data.geo.Distance
import org.springframework.data.geo.GeoResult
import org.springframework.data.geo.GeoResults
import org.springframework.data.geo.Metrics
import org.springframework.data.redis.connection.RedisGeoCommands
import org.springframework.data.redis.core.GeoOperations
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    runApplication<SpringKotlinSpatialApplication>(*args)
}

@RestController
@RequestMapping("/api/v1")
@SpringBootApplication
class SpringKotlinSpatialApplication(
    val cityRepository: CityRepository,
    val districtRepository: DistrictRepository,
    private val geoOperations: GeoOperations<String, Any>,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()
    private val vehicleLocation: String = "vehicle_location"

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        add(latitude = 39.92083330, longitude = 33.23083330, vehicleName = "ELMADAĞ")
        add(latitude = 39.94583330, longitude = 32.66944440, vehicleName = "ETİMESGUT")
        add(latitude = 39.02905060, longitude = 33.81042220, vehicleName = "EVREN")
        cityRepository.findAll()
        .map { city ->
            if (city.location == null)
                city.location = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
            city
        }
        .run { cityRepository.saveAll(this) }
        districtRepository.findAll()
            .map { district ->
                if (district.location == null)
                    district.location = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
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

    @PostMapping("/vehicle")
    fun addVehicleLocation(@RequestBody request: VehicleLocation): Long? =
        add(latitude = request.latitude, longitude = request.longitude, vehicleName = request.name)

    @GetMapping("/vehicle/{lat}/{lng}/{distance}")
    fun findNearestVehiclesByDistrictId(
        @PathVariable lat: Double,
        @PathVariable lng: Double,
        @PathVariable distance: Double
    ): List<VehicleLocation> = findNearestVehicles(longitude = lng, latitude = lat, km = distance)

    private fun add(latitude: Double, longitude: Double, vehicleName: String): Long? {
        val point: org.springframework.data.geo.Point = org.springframework.data.geo.Point(longitude, latitude)
        return geoOperations.add(vehicleLocation, point, vehicleName)
    }

    private fun findNearestVehicles(longitude: Double, latitude: Double, km: Double): List<VehicleLocation> {
        val args: RedisGeoCommands.GeoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeCoordinates().includeDistance().sortAscending().limit(10)

        val circle = Circle(org.springframework.data.geo.Point(longitude, latitude), Distance(km, Metrics.KILOMETERS))
        val response: GeoResults<RedisGeoCommands.GeoLocation<Any>>? = geoOperations.radius(vehicleLocation, circle, args)

        val vehicleLocationResponses: MutableList<VehicleLocation> = arrayListOf()
        response?.content?.forEach { data: GeoResult<RedisGeoCommands.GeoLocation<Any>> ->
            vehicleLocationResponses.add(element =
            VehicleLocation(name = "${data.content.name}", latitude = data.content.point.y, longitude = data.content.point.x)
                .also { it.averageDistance =  data.distance}
                .also { it.hash =  geoOperations.hash(vehicleLocation, data.content.name)!!.stream().findFirst().get()}
            )
        }
        return vehicleLocationResponses
    }
}


data class VehicleLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    var averageDistance: Distance? = null
    var hash: String? = null
}

