package com.lovelumine.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "登录响应结果")
data class LoginResponse(
    @Schema(description = "访问令牌", required = true)
    val token: String
)
