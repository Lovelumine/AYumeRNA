package com.lovelumine.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.ErrorHandler

@Configuration
class RabbitMQConfig {

    private val logger = LoggerFactory.getLogger(RabbitMQConfig::class.java)

    // 配置 sequenceTasks 队列、交换机和绑定
    @Bean
    fun sequenceTasksQueue(): Queue {
        return QueueBuilder.durable("sequenceTasks").build()
    }

    @Bean
    fun sequenceTasksExchange(): CustomExchange {
        val args = mapOf<String, Any>("x-delayed-type" to "direct")
        return CustomExchange("sequenceTasksExchange", "x-delayed-message", true, false, args)
    }

    @Bean
    fun sequenceTasksBinding(): Binding {
        return BindingBuilder.bind(sequenceTasksQueue())
            .to(sequenceTasksExchange())
            .with("sequenceTasks")
            .noargs()
    }

    // 配置 rfamTasks 队列、交换机和绑定
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
        return BindingBuilder.bind(rfamTasksQueue())
            .to(rfamTasksExchange())
            .with("rfamTasks")
    }

    // 配置 cmbuildTasks 队列、交换机和绑定
    @Bean
    fun cmbuildTasksQueue(): Queue {
        return QueueBuilder.durable("cmbuildTasks").build()
    }

    @Bean
    fun cmbuildTasksExchange(): DirectExchange {
        return ExchangeBuilder.directExchange("cmbuildTasksExchange").durable(true).build()
    }

    @Bean
    fun cmbuildTasksBinding(): Binding {
        return BindingBuilder.bind(cmbuildTasksQueue())
            .to(cmbuildTasksExchange())
            .with("cmbuildTasks")
    }

    // 配置 onehotTasks 队列、交换机和绑定
    @Bean
    fun onehotTasksQueue(): Queue {
        return QueueBuilder.durable("onehotTasks").build()
    }

    @Bean
    fun onehotTasksExchange(): DirectExchange {
        return ExchangeBuilder.directExchange("onehotTasksExchange").durable(true).build()
    }

    @Bean
    fun onehotTasksBinding(): Binding {
        return BindingBuilder.bind(onehotTasksQueue())
            .to(onehotTasksExchange())
            .with("onehotTasks")
    }

    // 配置 splitOnehotTasks 队列、交换机和绑定
    @Bean
    fun splitOnehotTasksQueue(): Queue {
        return QueueBuilder.durable("splitOnehotTasks").build()
    }

    @Bean
    fun splitOnehotTasksExchange(): DirectExchange {
        return ExchangeBuilder.directExchange("splitOnehotTasksExchange").durable(true).build()
    }

    @Bean
    fun splitOnehotTasksBinding(): Binding {
        return BindingBuilder.bind(splitOnehotTasksQueue())
            .to(splitOnehotTasksExchange())
            .with("splitOnehotTasks")
    }

    // 配置消息转换器，序列化和反序列化为 JSON 格式
    @Bean
    fun jackson2JsonMessageConverter(): Jackson2JsonMessageConverter {
        val objectMapper = ObjectMapper().registerKotlinModule()
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
        factory.setErrorHandler(ErrorHandler { throwable ->
            logger.error("RabbitMQ 消息处理异常：${throwable.message}", throwable)
        })
        return factory
    }
}
