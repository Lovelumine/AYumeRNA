package com.lovelumine.auth.service

import com.lovelumine.auth.model.User
import com.lovelumine.auth.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
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

    override fun register(user: User): User {
        if (userRepository.findByUsername(user.username) != null) {
            throw IllegalArgumentException("用户名已存在")
        }
        user.password = passwordEncoder.encode(user.password)
        return userRepository.save(user)
    }

    override fun login(username: String, password: String): String {
        val user = userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("用户不存在")

        if (!passwordEncoder.matches(password, user.password)) {
            throw IllegalArgumentException("密码错误")
        }

        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set("token:$token", user.id.toString(), 1, TimeUnit.HOURS)
        return token
    }

    override fun getUserByToken(token: String): User? {
        val userId = redisTemplate.opsForValue().get("token:$token") ?: return null
        return userRepository.findById(userId.toLong()).orElse(null)
    }

    override fun getUserById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }
}
