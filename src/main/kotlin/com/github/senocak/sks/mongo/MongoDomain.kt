package com.github.senocak.sks.mongo

import com.mongodb.client.model.geojson.Point
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.repository.MongoRepository
import java.io.Serializable
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

open class BaseDocument(
    @Id var id: UUID? = null,
    @Field @CreatedDate var createdAt: Date = Date(),
    @Field @LastModifiedDate var updatedAt: Date = Date()
): Serializable

@Document(collection = "city")
data class CityDocument(
    @Field var title: String? = null,
    @Field var lat: String? = null,
    @Field var lng: String? = null
): BaseDocument() {
    @Field(name = "northeast_lat") var northeastLat: BigDecimal? = null
    @Field(name = "northeast_lng") var northeastLng: BigDecimal? = null
    @Field(name = "southwest_lat") var southwestLat: BigDecimal? = null
    @Field(name = "southwest_lng") var southwestLng: BigDecimal? = null
    @Field @GeoSpatialIndexed var location: Point? = null
}

@Document(collection = "district")
data class DistrictDocument(
    @Field var title: String? = null,
    @Field var lat: String? = null,
    @Field var lng: String? = null,
    @Field(name = "district_city_id") var districtCityId: String? = null
): BaseDocument() {
    @Field(name = "northeast_lat") var northeastLat: BigDecimal? = null
    @Field(name = "northeast_lng") var northeastLng: BigDecimal? = null
    @Field(name = "southwest_lat") var southwestLat: BigDecimal? = null
    @Field(name = "southwest_lng") var southwestLng: BigDecimal? = null
    @Field @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)  var location: Point? = null
}

interface CityDocumentRepository: MongoRepository<CityDocument, UUID> {
    fun findByLocationNear(location: org.springframework.data.geo.Point, distance: Distance): List<CityDocument>
}

interface DistrictDocumentRepository: MongoRepository<DistrictDocument, UUID> {
    fun findByLocationNear(location: org.springframework.data.geo.Point, distance: Distance): List<DistrictDocument>
}
