package com.lovelumine.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    // 配置 sequenceTasks 队列、交换机和绑定
    @Bean
    fun sequenceTasksQueue(): Queue {
        return QueueBuilder.durable("sequenceTasks").build()
    }

    @Bean
    fun sequenceTasksExchange(): CustomExchange {
        val args = mapOf("x-delayed-type" to "direct")
        return CustomExchange("sequenceTasksExchange", "x-delayed-message", true, false, args)
    }

    @Bean
    fun sequenceTasksBinding(): Binding {
        return BindingBuilder.bind(sequenceTasksQueue()).to(sequenceTasksExchange()).with("sequenceTasks").noargs()
    }

    // 配置 RfamTasks 队列、交换机和绑定
    @Bean
    fun rfamTasksQueue(): Queue {
        return QueueBuilder.durable("rfamTasks").build()
    }

    @Bean
    fun rfamTasksExchange(): DirectExchange {
        return ExchangeBuilder.directExchange("rfamTasksExchange").durable(true).build()
    }

    @Bean
    fun rfamTasksBinding(): Binding {
        return BindingBuilder.bind(rfamTasksQueue()).to(rfamTasksExchange()).with("rfamTasks")
    }

    // 配置消息转换器，序列化和反序列化为 JSON 格式
    @Bean
    fun jackson2JsonMessageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = ObjectMapper().registerModule(kotlinModule())
        return Jackson2JsonMessageConverter(objectMapper)
    }

    // RabbitTemplate 配置
    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jackson2JsonMessageConverter()
        return rabbitTemplate
    }

    // RabbitListener 容器工厂配置，用于监听队列的消费
    @Bean
    fun rabbitListenerContainerFactory(connectionFactory: ConnectionFactory): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setConcurrentConsumers(3) // 设置并发消费者数量为 3
        factory.setMaxConcurrentConsumers(3) // 最大并发消费者数量为 3
        factory.setMessageConverter(jackson2JsonMessageConverter())
        return factory
    }
}
