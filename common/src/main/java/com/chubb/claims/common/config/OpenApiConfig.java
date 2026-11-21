package com.chubb.claims.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI with OAuth2 login against Keycloak, so endpoints can be called from the browser
 * as Claimant, Claims Staff or Manager users.
 */
@Configuration
public class OpenApiConfig {

    static final String SCHEME_NAME = "keycloak";

    @Value("${claims.oauth2.authorization-url:http://localhost:8180/realms/claims/protocol/openid-connect/auth}")
    private String authorizationUrl;

    @Value("${claims.oauth2.token-url:http://localhost:8180/realms/claims/protocol/openid-connect/token}")
    private String tokenUrl;

    @Bean
    public OpenAPI claimsOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Claims Platform API").version("v1")
                        .description("Claims intake, workflow, reserves and reporting"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                .authorizationUrl(authorizationUrl)
                                .tokenUrl(tokenUrl)
                                .scopes(new Scopes().addString("openid", "OpenID Connect scope"))))))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME, "openid"));
    }
}
