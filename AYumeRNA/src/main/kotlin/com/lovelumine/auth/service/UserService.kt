package com.lovelumine.auth.service

import com.lovelumine.auth.model.User

interface UserService {
    fun register(user: User): User
    fun login(username: String, password: String): String
    fun getUserByToken(token: String): User?
    fun getUserById(id: Long): User?  // 添加根据 ID 获取用户的方法
}
