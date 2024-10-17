package com.lovelumine.auth.controller

import com.lovelumine.auth.controller.dto.LoginRequest
import com.lovelumine.auth.controller.dto.LoginResponse
import com.lovelumine.auth.controller.dto.RegisterRequest
import com.lovelumine.auth.model.User
import com.lovelumine.auth.service.UserService
import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@Tag(name = "认证管理", description = "处理用户注册和登录的接口")
@RestController
@RequestMapping("/auth")
class AuthController(
    @Autowired val userService: UserService
) {

    @Operation(
        summary = "用户注册",
        description = "用户可以通过该接口进行注册",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "注册成功",
                content = [Content(schema = Schema(implementation = User::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "请求参数错误"
            )
        ]
    )
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): User {
        val user = User(
            username = request.username,
            password = request.password,
            email = request.email
        )
        return userService.register(user)
    }

    @Operation(
        summary = "用户登录",
        description = "用户可以通过该接口进行登录",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "登录成功",
                content = [Content(schema = Schema(implementation = LoginResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "用户名或密码错误"
            )
        ]
    )
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        val token = userService.login(request.username, request.password)
        return LoginResponse(token)
    }
}
