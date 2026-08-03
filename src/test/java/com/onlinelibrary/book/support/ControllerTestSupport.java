package com.onlinelibrary.book.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Shared security setup for all @WebMvcTest controller tests.
 * Spring Boot 4.x @WebMvcTest includes OAuth2ResourceServerAutoConfiguration
 * in the slice but not SecurityAutoConfiguration, so HttpSecurity is never
 * created and the context crashes. This class works around that by:
 *  1. The test class excludes OAuth2ResourceServerAutoConfiguration
 *     (via @WebMvcTest excludeAutoConfiguration)
 *  2. This config bootstraps Spring Security via @EnableWebSecurity
 *     and defines a filter chain that mirrors production security rules
 *  3. JwtDecoder is mocked — required by the .jwt() DSL but never called
 *     because jwt() from SecurityMockMvcRequestPostProcessors bypasses decoding
 * Usage:
 *   @WebMvcTest(value = MyController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class)
 *   @Import(ControllerTestSupport.class)
 *   class MyControllerTest {
 *       @MockitoBean JwtDecoder jwtDecoder;
 *       ...
 *   }
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class ControllerTestSupport {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/books/secure/**").authenticated()
                        .requestMatchers("/api/reviews/secure/**").authenticated()
                        .requestMatchers("/api/messages/secure/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}