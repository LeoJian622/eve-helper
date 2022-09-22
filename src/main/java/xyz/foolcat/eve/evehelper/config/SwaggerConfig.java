package xyz.foolcat.eve.evehelper.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Leojan
 * @date 2021-12-06 17:02
 */

@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {

        //schema
        List<GrantType> grantTypes = new ArrayList<>();
        //密码模式
        String passwordTokenUrl = "http://localhost:18010/oauth/token";
        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant = new ResourceOwnerPasswordCredentialsGrant(passwordTokenUrl);
        grantTypes.add(resourceOwnerPasswordCredentialsGrant);
        OAuth oAuth = new OAuthBuilder().name("oauth2")
                .grantTypes(grantTypes).build();
        //context
        //scope方位
        List<AuthorizationScope> scopes = new ArrayList<>();
        scopes.add(new AuthorizationScope("read", "read  resources"));
        scopes.add(new AuthorizationScope("write", "write resources"));
        scopes.add(new AuthorizationScope("reads", "read all resources"));
        scopes.add(new AuthorizationScope("writes", "write all resources"));

        SecurityReference securityReference = new SecurityReference("oauth2", scopes.toArray(new AuthorizationScope[]{}));
        SecurityContext securityContext = new SecurityContext(Collections.singletonList(securityReference), PathSelectors.ant("/api/**"));
        //schemas
        List<SecurityScheme> securitySchemes = Collections.singletonList(oAuth);
        //securyContext
        List<SecurityContext> securityContexts = Collections.singletonList(securityContext);

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build()
                .securityContexts(securityContexts)
                .securitySchemes(securitySchemes);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("EVE小帮手")
                .description("<div style='font-size:14px;color:red;'>市场、制造、研究、资产</div>")
                .termsOfServiceUrl("http://foolcat.xyz")
                .contact(new Contact("清空栀浅", "ttp://foolcat.xyz", "hualuoy_yks@qq.com"))
                .license("Open Source")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .version("1.0-SNAPSHOT")
                .build();
    }

}
