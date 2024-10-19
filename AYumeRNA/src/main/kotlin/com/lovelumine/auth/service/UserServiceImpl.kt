package com.lovelumine.auth.service

import com.lovelumine.auth.model.User
import com.lovelumine.auth.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class UserServiceImpl(
    @Autowired val userRepository: UserRepository,
    @Autowired val redisTemplate: StringRedisTemplate
) : UserService {

    private val passwordEncoder = BCryptPasswordEncoder()

    // 用户注册逻辑
    override fun register(user: User): User {
        if (userRepository.findByUsername(user.username) != null) {
            throw IllegalArgumentException("用户名已存在")
        }
        user.password = passwordEncoder.encode(user.password)
        return userRepository.save(user)
    }

    // 用户登录逻辑，生成 token 并存储在 Redis 中
    override fun login(username: String, password: String): String {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("用户不存在")

        if (!passwordEncoder.matches(password, user.password)) {
            throw BadCredentialsException("密码错误")
        }

        // 生成 token 并存储在 Redis 中，初始过期时间为 1 小时
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set("token:$token", user.id.toString(), 1, TimeUnit.HOURS)
        return token
    }

    // 根据 token 获取用户并滑动过期
    override fun getUserByToken(token: String): User? {
        val tokenKey = "token:$token"

        // 从 Redis 中获取用户 ID，如果 token 不存在则返回 null
        val userId = redisTemplate.opsForValue().get(tokenKey) ?: return null

        // 每次获取用户成功后，延长 token 的过期时间（滑动过期），比如延长 1 小时
        redisTemplate.expire(tokenKey, 1, TimeUnit.HOURS)

        // 根据用户 ID 从数据库获取用户信息
        return userRepository.findById(userId.toLong()).orElse(null)
    }

    // 通过用户 ID 获取用户信息
    override fun getUserById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }
}
