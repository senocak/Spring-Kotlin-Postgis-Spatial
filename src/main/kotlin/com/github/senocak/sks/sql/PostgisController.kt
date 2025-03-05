package com.github.senocak.sks.sql

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/postgis")
class PostgisController(
    val cityRepository: CityRepository,
    val districtRepository: DistrictRepository,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        cityRepository.findAll()
        .map { city: City ->
            if (city.location == null)
                city.location = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
            city
        }
        .run { cityRepository.saveAll(this) }
        districtRepository.findAll()
            .map { district: District ->
                if (district.location == null)
                    district.location = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
                district
            }
            .run { districtRepository.saveAll(this) }
    }

    @GetMapping("cities")
    fun getAllCities(): MutableList<City> = cityRepository.findAll()

    @PostMapping("city")
    fun create(@RequestParam lng: Double, @RequestParam lat: Double, @RequestParam title: String): City {
        val city = City(title = title, lat = lat.toBigDecimal(), lng = lng.toBigDecimal())
        city.location = geometryFactory.createPoint(Coordinate(lng, lat))
        return cityRepository.save(city)
    }

    @GetMapping("/city/{lat}/{lng}/{distance}")
    fun findNearestCities(@PathVariable lat: Float, @PathVariable lng: Float, @PathVariable distance: Int): Iterable<City> {
        val point: Point = geometryFactory.createPoint(Coordinate(lng.toDouble(), lat.toDouble()))
        return cityRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/city/{id}/{distance}")
    fun findNearestCitiesByCityId(@PathVariable id: String, @PathVariable distance: Int): Iterable<City> {
        val city: City = cityRepository.findById(id).orElseThrow { RuntimeException("City with id $id not found") }
        val point: Point = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
        return cityRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("districts")
    fun getAllDistricts(): MutableList<District> = districtRepository.findAll()

    @GetMapping("/district/{lat}/{lng}/{distance}")
    fun findNearestDistricts(@PathVariable lat: Float, @PathVariable lng: Float, @PathVariable distance: Int): Iterable<District> {
        val point: Point = geometryFactory.createPoint(Coordinate(lng.toDouble(), lat.toDouble()))
        return districtRepository.findNearest(point, distance.toDouble())
    }

    @GetMapping("/district/{id}/{distance}")
    fun findNearestDistrictsByDistrictId(@PathVariable id: String, @PathVariable distance: Int): Iterable<District> {
        val district: District = districtRepository.findById(id).orElseThrow { RuntimeException("District with id $id not found") }
        val point: Point = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
        return districtRepository.findNearest(point, distance.toDouble())
    }
}
