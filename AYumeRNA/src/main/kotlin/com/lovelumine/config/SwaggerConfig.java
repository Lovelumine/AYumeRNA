package com.lovelumine.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 创建一个 SecurityRequirement 并将其添加到 OpenAPI 实例中
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("AYumeRNA API")
                        .version("1.0")
                        .description("“AY”代表“Aim Your”，结合“Yume”表示“梦想”，象征瞄准目标，生成具有实际功能的RNA序列。"))
                .components(new Components())
                .addSecurityItem(securityRequirement); // 添加全局的 SecurityRequirement
    }
}
