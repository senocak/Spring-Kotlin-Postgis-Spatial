package com.github.senocak.sks.sql

import com.fasterxml.jackson.annotation.JsonIgnore
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
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

@MappedSuperclass
open class BaseDomain(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    var id: Int? = null
): Serializable

@Entity
@Table(name = "city")
data class City(
    @Column(name = "title", nullable = false) var title: String,
    @Column(name = "lat", precision = 10, scale = 8, nullable = false) var lat: BigDecimal,
    @Column(name = "lng", precision = 10, scale = 8, nullable = false) var lng: BigDecimal,
): BaseDomain() {
    @Column(name = "northeast_lat", precision = 10, scale = 8, nullable = false) var northeastLat: BigDecimal? = null
    @Column(name = "northeast_lng", precision = 10, scale = 8, nullable = false) var northeastLng: BigDecimal? = null
    @Column(name = "southwest_lat", precision = 10, scale = 8, nullable = false) var southwestLat: BigDecimal? = null
    @Column(name = "southwest_lng", precision = 10, scale = 8, nullable = false) var southwestLng: BigDecimal? = null
    @JsonIgnore
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

    @Column(name = "title", nullable = false) var title: String,
    @Column(name = "lat", precision = 10, scale = 8, nullable = false) var lat: BigDecimal,
    @Column(name = "lng", precision = 10, scale = 8, nullable = false) var lng: BigDecimal,
) : BaseDomain() {
    @Column(name = "northeast_lat", precision = 10, scale = 8, nullable = false) var northeastLat: BigDecimal? = null
    @Column(name = "northeast_lng", precision = 10, scale = 8, nullable = false) var northeastLng: BigDecimal? = null
    @Column(name = "southwest_lat", precision = 10, scale = 8, nullable = false) var southwestLat: BigDecimal? = null
    @Column(name = "southwest_lng", precision = 10, scale = 8, nullable = false) var southwestLng: BigDecimal? = null
    @JsonIgnore
    @Column(columnDefinition = "geography(Point, 4326)") var location: Point? = null
}

interface CityRepository: JpaRepository<City, String>, JpaSpecificationExecutor<City> {
    @Query("SELECT c FROM City c WHERE function('ST_DWithin', c.location, :point, :distance) = true")
    fun findNearest(point: Point, distance: Double): Iterable<City>
}
interface DistrictRepository: JpaRepository<District, String>, JpaSpecificationExecutor<District> {
    @Query("SELECT d FROM District d WHERE function('ST_DWithin', d.location, :point, :distance) = true")
    fun findNearest(point: Point, distance: Double): Iterable<District>
}
