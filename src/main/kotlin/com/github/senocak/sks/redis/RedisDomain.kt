package com.github.senocak.sks.redis

import org.springframework.data.geo.Distance

data class RedisLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    var averageDistance: Distance? = null
    var hash: String? = null
}
