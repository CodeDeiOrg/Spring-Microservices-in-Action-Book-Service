package com.onlinelibrary.book.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import static com.onlinelibrary.book.utils.Constants.ROLES_CLAIM;

// Enables @PreAuthorize / @PostAuthorize on controller methods
@EnableMethodSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        // Disable Cross Site Request Forgery
        http.csrf(AbstractHttpConfigurer::disable);

        // Protect endpoints at /api/<type>/secure
        http.authorizeHttpRequests(configurer ->
                configurer
                        .requestMatchers(
                                "/api/books/secure/**",
                                "/api/reviews/secure/**",
                                "/api/messages/secure/**")
                        .authenticated().anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        // Add CORS filters
        http.cors(Customizer.withDefaults());

        // Add content negotiation strategy
        http.setSharedObject(ContentNegotiationStrategy.class,
                new HeaderContentNegotiationStrategy());

        return http.build();
    }

    /**
     * Teaches Spring Security how to extract roles from the Auth0 JWT.
     * <p>
     * By default, Spring reads the "scope" claim and adds a "SCOPE_" prefix.
     * Auth0 puts roles in a custom namespaced claim as a string array, with no prefix needed.
     * <p>
     * Before this bean: Spring sees no authorities → hasAuthority('admin') always false
     * After this bean:  Spring sees ["admin"] → hasAuthority('admin') works correctly
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Tell Spring which claim holds the roles
        authoritiesConverter.setAuthoritiesClaimName(ROLES_CLAIM);
        // No prefix — Auth0 gives "admin", we want GrantedAuthority("admin"), not "ROLE_admin"
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

}
