package com.github.senocak.sks.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class PostgisConfig(
    private val datasource: DataSourceConfigs,
    private val hikari: HikariProperties
){
    @Bean
    @Primary
    fun dataSource(): DataSource =
        when {
            datasource.url.contains(other = "jdbc:postgresql") -> DriverManagerDataSource()
                .also { db: DriverManagerDataSource ->
                    db.url = datasource.url
                    db.username = datasource.username
                    db.password = datasource.password
                }
            else -> throw RuntimeException("Not configured")
        }

    @Bean
    fun hikariDataSource(dataSource: DataSource): HikariDataSource =
        HikariDataSource(HikariConfig()
            .also { it: HikariConfig ->
                it.dataSource = dataSource
                it.poolName = hikari.poolName ?: "SpringKotlinSpatialHikariCP"
                it.minimumIdle = hikari.minimumIdle
                it.maximumPoolSize = hikari.maximumPoolSize
                it.maxLifetime = hikari.maxLifetime
                it.idleTimeout = hikari.idleTimeout
                it.connectionTimeout = hikari.connectionTimeout
                it.transactionIsolation = hikari.transactionIsolation ?: "TRANSACTION_READ_COMMITTED"
            }
        )
}

@ConfigurationProperties(prefix = "spring.datasource")
class DataSourceConfigs: DataSourceProperties()

@ConfigurationProperties(prefix = "spring.datasource.hikari")
class HikariProperties: HikariConfig()