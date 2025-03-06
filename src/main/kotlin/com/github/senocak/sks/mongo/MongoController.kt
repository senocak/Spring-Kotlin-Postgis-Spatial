package com.github.senocak.sks.mongo

import com.github.senocak.sks.CityDistrict
import com.github.senocak.sks.generateCityDistrict
import com.github.senocak.sks.getResourceText
import com.github.senocak.sks.logger
import com.mongodb.client.model.Filters
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import org.bson.Document
import org.slf4j.Logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeospatialIndex
import org.springframework.data.mongodb.core.index.Index
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/mongo")
class MongoController(
    private val mongoTemplate: MongoTemplate,
    private val cityDocumentRepository: CityDocumentRepository,
    private val districtDocumentRepository: DistrictDocumentRepository,
) {
    private val log: Logger by logger()

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        log.info("Starting MongoDB migration process...")
        if (mongoTemplate.collectionExists("city"))
            mongoTemplate.dropCollection("city").also { log.info("Dropped existing city collection") }
        mongoTemplate.createCollection("city").also { log.info("Created city collection") }
        // Create indexes on city
        mongoTemplate.indexOps("city").ensureIndex(GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE))
        mongoTemplate.indexOps("city").ensureIndex(Index().on("title", org.springframework.data.domain.Sort.Direction.ASC))

        if (mongoTemplate.collectionExists("district"))
            mongoTemplate.dropCollection("district").also { log.info("Dropped existing district collection") }
        mongoTemplate.createCollection("district").also { log.info("Created district collection") }
        // Create indexes on district
        mongoTemplate.indexOps("district").ensureIndex(GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE))
        mongoTemplate.indexOps("district").ensureIndex(Index().on("title", org.springframework.data.domain.Sort.Direction.ASC))

        "city-district.json"
            .getResourceText()
            .generateCityDistrict()
            .forEach { cd: CityDistrict ->
                run {
                    CityDocument(title = cd.title, lat = cd.lat, lng = cd.lng)
                        .apply {
                            this.id = cd.id.toInt()
                            this.northeastLat = cd.northeast_lat.toBigDecimal()
                            this.northeastLng = cd.northeast_lng.toBigDecimal()
                            this.southwestLat = cd.southwest_lat.toBigDecimal()
                            this.southwestLng = cd.southwest_lng.toBigDecimal()
                            this.location = Point(Position(cd.lng.toDouble(), cd.lat.toDouble()))
                        }
                        .apply {
                            mongoTemplate.insert(this).also { log.info("Inserted $this city into MongoDB") }
                        }
                    cd.districts.forEach { d: CityDistrict ->
                        DistrictDocument(title = d.title, lat = d.lat, lng = d.lng, districtCityId = cd.id)
                            .apply {
                                this.id = d.id.toInt()
                                this.northeastLat = d.northeast_lat.toBigDecimal()
                                this.northeastLng = d.northeast_lng.toBigDecimal()
                                this.southwestLat = d.southwest_lat.toBigDecimal()
                                this.southwestLng = d.southwest_lng.toBigDecimal()
                                this.location = Point(Position(d.lng.toDouble(), d.lat.toDouble()))
                            }
                            .apply {
                                mongoTemplate.insert(this).also { log.info("Inserted $this district into MongoDB") }
                            }
                    }
                }
            }
        log.info("MongoDB migration completed successfully")
    }

    @GetMapping("/cities")
    fun getAllCities(): MutableList<CityDocument> = cityDocumentRepository.findAll()

    /*
    db.city.find({
        location: {
            $near: {
                $geometry: {
                    type: "Point",
                    coordinates: [35.3, 37.0]
                },
                $maxDistance: 50_000,
                $minDistance: 1.0
            }
        }
    })
    */
    @GetMapping("/city/{id}/{distance}")
    fun findNearestCitiesByCityId(@PathVariable id: Int, @PathVariable distance: Double): Any { // distance in meters
        val city: CityDocument = cityDocumentRepository.findById(id).orElseThrow { RuntimeException("City with id $id not found") }
        return cityDocumentRepository.findByLocationNear(org.springframework.data.geo.Point(city.lng!!.toDouble(), city.lat!!.toDouble()) , Distance(distance, Metrics.KILOMETERS))
    }

    @GetMapping("/city/{lat}/{lng}/{distance}")
    fun findNearestCities(@PathVariable lat: Double, @PathVariable lng: Double, @PathVariable distance: Double): Iterable<Document> =
        mongoTemplate.getCollection("city")
            .find(Filters.near("location", Point(Position(lng, lat)), distance, 0.0)).toList()

    @GetMapping("/district")
    fun getAllDistricts(): MutableList<DistrictDocument> = districtDocumentRepository.findAll()

    @GetMapping("/district/{id}/{distance}")
    fun findNearestDistrictsByDistrictId(@PathVariable id: Int, @PathVariable distance: Double): Iterable<DistrictDocument> {
        val district: DistrictDocument = districtDocumentRepository.findById(id).orElseThrow { RuntimeException("District with id $id not found") }
        return districtDocumentRepository.findByLocationNear(org.springframework.data.geo.Point(district.lng!!.toDouble(), district.lat!!.toDouble()) , Distance(distance, Metrics.KILOMETERS))
    }

    @GetMapping("/district/{lat}/{lng}/{distance}")
    fun findNearestDistricts(@PathVariable lat: Double, @PathVariable lng: Double, @PathVariable distance: Double): Iterable<Document> =
        mongoTemplate.getCollection("district")
            .find(Filters.near("location", Point(Position(lng, lat)), distance, 0.0)).toList()
}
