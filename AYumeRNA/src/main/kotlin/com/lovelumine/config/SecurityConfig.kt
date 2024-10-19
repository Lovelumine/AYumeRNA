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
            .csrf { it.disable() }  // 正确的禁用 CSRF 保护方法
            .authorizeHttpRequests {
                it.requestMatchers("/auth/**").permitAll()  // 允许注册和登录接口
                it.requestMatchers("/swagger-ui/**", "/webjars/**","/api-docs/**","v3/api-docs/**","/v3/api-docs", "/v3/api-docs/swagger-config","/swagger-ui.html","/doc.html").permitAll()
                it.requestMatchers("/hello","/favicon.ico","docs.html","/api-docs/").permitAll()
                it.requestMatchers("/sequence/process").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                TokenAuthenticationFilter(userService, redisTemplate),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}
