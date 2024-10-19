package com.lovelumine.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 原生 WebSocket 端点
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")

        // SockJS 端点
        registry.addEndpoint("/sockjs/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }
}

