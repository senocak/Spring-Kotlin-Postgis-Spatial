package com.github.senocak.sks.sql

import com.github.senocak.sks.CityDistrict
import com.github.senocak.sks.generateCityDistrict
import com.github.senocak.sks.getResourceText
import com.github.senocak.sks.logger
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.slf4j.Logger
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
    private val log: Logger by logger()
    private val geometryFactory = GeometryFactory()

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        log.info("Starting Postgis migration process...")
        districtRepository.deleteAll()
        cityRepository.deleteAll()
        "city-district.json"
            .getResourceText()
            .generateCityDistrict()
            .forEach { cd: CityDistrict ->
                run {
                    val city: City = City(title = cd.title, lat = cd.lat.toBigDecimal(), lng = cd.lng.toBigDecimal())
                        .apply {
                            this.id = cd.id.toInt()
                            this.northeastLat = cd.northeast_lat.toBigDecimal()
                            this.northeastLng = cd.northeast_lng.toBigDecimal()
                            this.southwestLat = cd.southwest_lat.toBigDecimal()
                            this.southwestLng = cd.southwest_lng.toBigDecimal()
                            this.location = geometryFactory.createPoint(Coordinate(cd.lng.toDouble(), cd.lat.toDouble()))
                        }
                        .apply {
                            cityRepository.save(this).also { log.info("Inserted $this city into Postgis") }
                        }
                    cd.districts.forEach { d: CityDistrict ->
                        District(city_id = city, title = d.title, lat = d.lat.toBigDecimal(), lng = d.lng.toBigDecimal())
                            .apply {
                                this.id = d.id.toInt()
                                this.northeastLat = d.northeast_lat.toBigDecimal()
                                this.northeastLng = d.northeast_lng.toBigDecimal()
                                this.southwestLat = d.southwest_lat.toBigDecimal()
                                this.southwestLng = d.southwest_lng.toBigDecimal()
                                this.location = geometryFactory.createPoint(Coordinate(d.lng.toDouble(), d.lat.toDouble()))
                            }
                            .apply {
                                districtRepository.save(this).also { log.info("Inserted $this district into Postgis") }
                            }
                    }
                }
            }
        log.info("Postgis migration completed successfully")
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
