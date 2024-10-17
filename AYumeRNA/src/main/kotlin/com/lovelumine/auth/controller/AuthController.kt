package com.lovelumine.auth.controller

import com.lovelumine.auth.controller.dto.LoginRequest
import com.lovelumine.auth.controller.dto.LoginResponse
import com.lovelumine.auth.controller.dto.RegisterRequest
import com.lovelumine.auth.model.User
import com.lovelumine.auth.service.UserService
import com.lovelumine.common.ResponseUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    @Autowired val userService: UserService
) {

    @Operation(summary = "用户注册", description = "用户可以通过该接口进行注册")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "注册成功"),
        ApiResponse(responseCode = "400", description = "请求参数错误")
    ])
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val user = User(
                username = request.username,
                password = request.password,
                email = request.email
            )
            val registeredUser = userService.register(user)
            ResponseEntity.ok(ResponseUtil.formatResponse(200, registeredUser))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(400).body(ResponseUtil.formatResponse(400, ex.message ?: "注册失败"))
        } catch (ex: Exception) {
            ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, "服务器内部错误：${ex.message}"))
        }
    }

    @Operation(summary = "用户登录", description = "用户可以通过该接口进行登录")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "登录成功"),
        ApiResponse(responseCode = "401", description = "用户名或密码错误"),
        ApiResponse(responseCode = "500", description = "服务器内部错误")
    ])
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val token = userService.login(request.username, request.password)
            ResponseEntity.ok(ResponseUtil.formatResponse(200, LoginResponse(token)))
        } catch (ex: UsernameNotFoundException) {
            ResponseEntity.status(404).body(ResponseUtil.formatResponse(404, "用户不存在"))
        } catch (ex: BadCredentialsException) {
            ResponseEntity.status(401).body(ResponseUtil.formatResponse(401, "密码错误"))
        } catch (ex: Exception) {
            ResponseEntity.status(500).body(ResponseUtil.formatResponse(500, "服务器内部错误：${ex.message}"))
        }
    }
}
