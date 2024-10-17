package com.lovelumine.config

import com.lovelumine.auth.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class TokenAuthenticationFilter(
    private val userService: UserService,
    private val redisTemplate: StringRedisTemplate  // 注入 redisTemplate
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = getTokenFromRequest(request)

        if (StringUtils.hasText(token)) {
            val userId = redisTemplate.opsForValue().get("token:$token") ?: return  // 从 Redis 获取 userId
            val user = userService.getUserById(userId.toLong())  // 获取用户信息
            if (user != null) {
                val authentication = UsernamePasswordAuthenticationToken(
                    user, null, emptyList()
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)  // 返回 Bearer 之后的 token 部分
        } else null
    }
}
