package com.github.senocak.sks

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import java.io.File

fun main(args: Array<String>) {
    runApplication<SpringKotlinSpatialApplication>(*args)
}

@SpringBootApplication
@ConfigurationPropertiesScan
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

fun String.getResourceText(): String = File(ClassLoader.getSystemResource(this).file).readText()
fun String.generateCityDistrict(): List<CityDistrict> = jacksonObjectMapper().readValue(this, object : TypeReference<List<CityDistrict>>() {})
fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger((if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass).name)
}