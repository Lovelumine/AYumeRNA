package com.lovelumine.auth.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.time.LocalDateTime

@Schema(description = "用户实体")
@Entity
@Table(name = "users")
data class User(
    @Schema(description = "用户ID")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Schema(description = "用户名")
    @Column(nullable = false, unique = true)
    var username: String,

    @Schema(description = "密码")
    @Column(nullable = false)
    var password: String,

    @Schema(description = "邮箱")
    @Column(nullable = false)
    var email: String,

    @Schema(description = "创建时间")
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

{
    // 无参构造函数
    constructor() : this(0, "", "", "", LocalDateTime.now())
}
