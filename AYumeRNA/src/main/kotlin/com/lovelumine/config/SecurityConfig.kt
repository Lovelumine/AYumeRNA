package com.lovelumine.config

import com.lovelumine.auth.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val userService: UserService,
    private val redisTemplate: StringRedisTemplate
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**").permitAll()
                it.requestMatchers(
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/v3/api-docs/swagger-config",
                    "/swagger-ui.html",
                    "sample",
                    "sample/*",
                    "sample/process",
                    "/doc.html"
                ).permitAll()
                it.requestMatchers(
                    "/hello",
                    "/favicon.ico",
                    "/docs.html",
                    "/api-docs/",
                    "/sequence/process",
                    "/topic/progress/**",
                    "/ws/**",
                    "/sockjs/ws/**"
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                TokenAuthenticationFilter(userService, redisTemplate),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
