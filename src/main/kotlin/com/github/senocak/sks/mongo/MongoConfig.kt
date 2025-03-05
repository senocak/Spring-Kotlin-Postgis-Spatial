package com.github.senocak.sks.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import java.util.Optional
import org.bson.UuidRepresentation
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
import org.springframework.data.domain.AuditorAware
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackages = ["com.github.senocak.sks.mongo"])
class MongoConfig(
    private val mongoProperties: MongoProperties,
): AbstractMongoClientConfiguration() {
    override fun getDatabaseName(): String = mongoProperties.database
    override fun mongoClient(): MongoClient = mongoClientBean()

    @Bean
    fun mongoClientBean(): MongoClient =
        MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString("mongodb://${mongoProperties.username}:${String(mongoProperties.password)}@${mongoProperties.host}:${mongoProperties.port}/${mongoProperties.database}?authSource=admin"))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        )

    public override fun getMappingBasePackages(): MutableCollection<String> =  arrayListOf("com.github.senocak.sks.mongo")

    @Bean
    fun auditorAwareRef(): AuditorAware<String> = AuditorAware<String> { Optional.of("Mr. Senocak") }

    @Bean
    fun mongoTemplate(): MongoTemplate =
        MongoTemplate(mongoClient(), databaseName)

    @Bean
    fun exceptionTranslation(): PersistenceExceptionTranslationPostProcessor = PersistenceExceptionTranslationPostProcessor()
}
