package com.lovelumine.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "登录请求参数")
data class LoginRequest(
    @Schema(description = "用户名", required = true)
    val username: String,

    @Schema(description = "密码", required = true)
    val password: String
)
