package com.lovelumine.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "注册请求参数")
data class RegisterRequest(
    @Schema(description = "用户名", required = true)
    val username: String,

    @Schema(description = "密码", required = true)
    val password: String,

    @Schema(description = "邮箱", required = true)
    val email: String
)
