package com.github.senocak.sks

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import java.io.Serializable
import java.math.BigDecimal
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.annotations.UuidGenerator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/cities")
@SpringBootApplication
class SpringKotlinSpatialApplication(
    val cityRepository: CityRepository,
    val districtRepository: DistrictRepository
){
    private val geometryFactory = GeometryFactory()

    //@EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        cityRepository.findAll()
        .map { city ->
            city.location = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
            city
        }
        .run { cityRepository.saveAll(this) }
        districtRepository.findAll()
            .map { district ->
                district.location = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
                district
            }
            .run { districtRepository.saveAll(this) }
    }

    @PostMapping
    fun create(@RequestParam lng: Double, @RequestParam lat: Double, @RequestParam title: String): City {
        val city = City(title = title, lat = lat.toBigDecimal(), lng = lng.toBigDecimal())
        city.location = geometryFactory.createPoint(Coordinate(lng, lat))
        return cityRepository.save(city)
    }

    @GetMapping
    fun findAll(): MutableList<City> = cityRepository.findAll()

    @GetMapping("/nearest")
    fun findNearestCities(
        @RequestParam lat: Float,
        @RequestParam lng: Float,
        @RequestParam distance: Int
    ): Iterable<City?>? {
        val point: Point = geometryFactory.createPoint(Coordinate(lng.toDouble(), lat.toDouble()))
        return cityRepository.findNearestCities(point, distance.toDouble())
    }

    @GetMapping("/nearest/{id}/{distance}")
    fun findNearestCitiesByCityId(
        @PathVariable id: String,
        @PathVariable distance: Int
    ): Iterable<City?>? {
        val city: City = cityRepository.findById(id).orElseThrow { RuntimeException("City with id $id not found") }
        val point: Point = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
        return cityRepository.findNearestCities(point, distance.toDouble())
    }
}

fun main(args: Array<String>) {
    runApplication<SpringKotlinSpatialApplication>(*args)
}

@MappedSuperclass
open class BaseDomain(
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(name = "id", updatable = false, nullable = false)
    var id: String? = null
): Serializable

@Entity
@Table(name = "city")
data class City(
    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "lat", precision = 10, scale = 8, nullable = false) var lat: BigDecimal,
    @Column(name = "lng", precision = 10, scale = 8, nullable = false) var lng: BigDecimal,
): BaseDomain() {
    @Column(name = "northeast_lat", precision = 10, scale = 8, nullable = false) var northeastLat: BigDecimal? = null
    @Column(name = "northeast_lng", precision = 10, scale = 8, nullable = false) var northeastLng: BigDecimal? = null
    @Column(name = "southwest_lat", precision = 10, scale = 8, nullable = false) var southwestLat: BigDecimal? = null
    @Column(name = "southwest_lng", precision = 10, scale = 8, nullable = false) var southwestLng: BigDecimal? = null
    @Column(columnDefinition = "geography(Point, 4326)") var location: Point? = null
}

@Entity
@Table(name = "district")
data class District (
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(
        name = "district_city_id",
        referencedColumnName = "id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_district_city_id")
    )
    val city_id: City,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "lat", precision = 10, scale = 8, nullable = false) var lat: BigDecimal,
    @Column(name = "lng", precision = 10, scale = 8, nullable = false) var lng: BigDecimal,
) : BaseDomain() {
    @Column(name = "northeast_lat", precision = 10, scale = 8, nullable = false) var northeastLat: BigDecimal? = null
    @Column(name = "northeast_lng", precision = 10, scale = 8, nullable = false) var northeastLng: BigDecimal? = null
    @Column(name = "southwest_lat", precision = 10, scale = 8, nullable = false) var southwestLat: BigDecimal? = null
    @Column(name = "southwest_lng", precision = 10, scale = 8, nullable = false) var southwestLng: BigDecimal? = null
    @Column(columnDefinition = "geography(Point, 4326)") var location: Point? = null
}

interface CityRepository: JpaRepository<City, String>, JpaSpecificationExecutor<City> {
    @Query("SELECT c FROM City c WHERE function('ST_DWithin', c.location, :point, :distance) = true")
    fun findNearestCities(point: Point?, distance: Double): Iterable<City?>?
}
interface DistrictRepository: JpaRepository<District, String>, JpaSpecificationExecutor<District>{
    @Query("SELECT d FROM District d WHERE function('ST_DWithin', d.location, :point, :distance) = true")
    fun findNearestCities(point: Point?, distance: Double): Iterable<District?>?
}