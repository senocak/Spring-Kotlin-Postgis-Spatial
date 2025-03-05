package com.github.senocak.sks.redis

import com.github.senocak.sks.sql.City
import com.github.senocak.sks.sql.District
import com.github.senocak.sks.sql.PostgisController
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.geo.Circle
import org.springframework.data.geo.Distance
import org.springframework.data.geo.GeoResult
import org.springframework.data.geo.GeoResults
import org.springframework.data.geo.Metrics
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.RedisGeoCommands
import org.springframework.data.redis.core.GeoOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import redis.clients.jedis.GeoRadiusResponse
import redis.clients.jedis.GeoUnit
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

@RestController
@RequestMapping("/api/v1/redis")
class RedisController(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val geoOperations: GeoOperations<String, Any>,
    private val jedisPool: JedisPool,
    private val postgisController: PostgisController
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory()
    private val locationsCity: String = "redis_location_city"
    private val locationsDistrict: String = "redis_location_district"
    private val connection: RedisConnection? = redisTemplate.connectionFactory?.connection

    @EventListener(value = [ApplicationReadyEvent::class])
    fun init(event: ApplicationReadyEvent) {
        connection?.serverCommands()?.flushDb()
        connection?.serverCommands()?.flushAll()
        postgisController.cityRepository.findAll()
        .map { city: City ->
            if (city.location == null)
                city.location = geometryFactory.createPoint(Coordinate(city.lng.toDouble(), city.lat.toDouble()))
            //add(latitude = city.lat.toDouble(), longitude = city.lng.toDouble(), name = city.title)
            jedisPool.resource.use {
                it.geoadd(locationsCity, city.lng.toDouble(), city.lat.toDouble(), city.title);
            }
            city
        }
        postgisController.districtRepository.findAll()
            .map { district: District ->
                if (district.location == null)
                    district.location = geometryFactory.createPoint(Coordinate(district.lng.toDouble(), district.lat.toDouble()))
                add(latitude = district.lat.toDouble(), longitude = district.lng.toDouble(), name = district.title)
                jedisPool.resource.use {
                    it.geoadd(locationsDistrict, district.lng.toDouble(), district.lat.toDouble(), district.title);
                }
                district
            }
    }

    @GetMapping("/cities")
    fun getAllLocationsForCities(): MutableCollection<String> {
        /*
        TODO: following not working
        val redisKeys: MutableSet<ByteArray>? = connection?.keyCommands()?.keys("locationsCity*".toByteArray())
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
            cities = it.zrange(locationsCity, 0, -1)
        }
        return cities
    }

    @GetMapping("/districts")
    fun getAllLocationsForDistricts(): MutableSet<String> {
        val districts: MutableSet<String>
        jedisPool.resource.use { it: Jedis ->
            districts = it.zrange(locationsDistrict, 0, -1)
        }
        return districts
    }

    @GetMapping("/{type}/{lat}/{lng}/{distance}")
    fun findNearestByJedis(
        @PathVariable type: String,
        @PathVariable lat: Double,
        @PathVariable lng: Double,
        @PathVariable distance: Double
    ): MutableList<GeoRadiusResponse> {
        val georadius: MutableList<GeoRadiusResponse>
        jedisPool.resource.use { it: Jedis ->
            georadius = it.georadius(if (type == "districts") locationsDistrict else locationsCity,
                lng, lat, distance, GeoUnit.KM)
        }
        return georadius
    }

    @GetMapping("/{lat}/{lng}/{distance}")
    fun findNearestByGeoOperations(
        @PathVariable lat: Double,
        @PathVariable lng: Double,
        @PathVariable distance: Double
    ): List<RedisLocation> {
        val args: RedisGeoCommands.GeoRadiusCommandArgs = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
            .includeCoordinates().includeDistance().sortAscending().limit(10)

        val circle = Circle(org.springframework.data.geo.Point(lng, lat), Distance(distance, Metrics.KILOMETERS))
        val response: GeoResults<RedisGeoCommands.GeoLocation<Any>>? = geoOperations.radius(locationsDistrict, circle, args)

        val redisLocationRespons: MutableList<RedisLocation> = arrayListOf()
        response?.content?.forEach { data: GeoResult<RedisGeoCommands.GeoLocation<Any>> ->
            redisLocationRespons.add(element =
            RedisLocation(name = "${data.content.name}", latitude = data.content.point.y, longitude = data.content.point.x)
                .also { it.averageDistance =  data.distance}
                .also { it.hash =  geoOperations.hash(locationsDistrict, data.content.name)!!.stream().findFirst().get()}
            )
        }
        return redisLocationRespons
    }

    private fun add(latitude: Double, longitude: Double, name: String): Long? {
        val point: org.springframework.data.geo.Point = org.springframework.data.geo.Point(longitude, latitude)
        return geoOperations.add(locationsDistrict, point, name)
    }
}


