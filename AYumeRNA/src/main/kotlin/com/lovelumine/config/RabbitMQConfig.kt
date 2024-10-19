package com.lovelumine.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    @Bean
    fun sequenceTasksQueue(): Queue {
        return Queue("sequenceTasks")
    }

    @Bean
    fun jackson2JsonMessageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = ObjectMapper().registerModule(kotlinModule())
        return Jackson2JsonMessageConverter(objectMapper)
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jackson2JsonMessageConverter()
        return rabbitTemplate
    }
}
