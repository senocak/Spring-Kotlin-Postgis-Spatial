package com.github.senocak.sks

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.GeoOperations
import org.springframework.data.redis.core.RedisTemplate
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

@Configuration
class RedisConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.data.redis.HOST}") lateinit var host: String
    @Value("\${spring.data.redis.PORT}") var port: Int = 0
    @Value("\${spring.data.redis.PASSWORD}") lateinit var password: String
    @Value("\${spring.data.redis.TIMEOUT}") var timeout: Int = 0

    @Bean
    fun jedisPool(): JedisPool {
        log.debug("RedisConfig: host=$host, port=$port, password=$password, timeout=$timeout")
        return JedisPool(JedisPoolConfig(), host, port, timeout, password)
    }

    /**
     * Create JedisPool
     * @return JedisPool
     */
    val jedisPool: JedisPool?
        get() = Companion.jedisPool

    @Bean
    fun jedisConnectionFactory(): LettuceConnectionFactory {
        val redisStandaloneConfiguration = RedisStandaloneConfiguration()
        redisStandaloneConfiguration.hostName = host!!
        redisStandaloneConfiguration.setPassword(password)
        redisStandaloneConfiguration.port = port
        return LettuceConnectionFactory(redisStandaloneConfiguration)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>()
            .apply { this.connectionFactory = jedisConnectionFactory() }

    @Bean
    fun geoOperations(): GeoOperations<String, Any> = redisTemplate().opsForGeo()

    companion object {
        private var jedisPool: JedisPool? = null
    }
}
