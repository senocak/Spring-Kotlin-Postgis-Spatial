package com.github.senocak.sks

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

fun main(args: Array<String>) {
    runApplication<SpringKotlinSpatialApplication>(*args)
}

@SpringBootApplication
class SpringKotlinSpatialApplication

data class CityDistrict(
    val id: String,
    val title: String,
    val lat: String,
    val lng: String,
    val northeast_lat: String,
    val northeast_lng: String,
    val southwest_lat: String,
    val southwest_lng: String,
    val location: String,
) {
    var districts: MutableList<CityDistrict> = mutableListOf()
}