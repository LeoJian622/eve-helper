package xyz.foolcat.eve.evehelper.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Leojan
 * @date 2021-12-06 17:02
 */

@Configuration
public class SpringDocConfig {

    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("认证", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .bearerFormat("JWT")
                                .scheme("bearer"))
                )
                .info(new Info()
                        .title("EVE小帮手")
                        .version("1.0-SNAPSHOT")
                        .description("<div style='font-size:14px;color:red;'>市场、制造、研究、资产</div>")
                        .license(new License()
                                .name("APACHE 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

}
