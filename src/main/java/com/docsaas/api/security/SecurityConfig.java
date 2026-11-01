package com.docsaas.api.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Value("${docsaas.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	http
        .cors(cors -> {})
        .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers("/", "/error", "/api/v1/health").permitAll()
            	    .requestMatchers("/api/auth/**").permitAll()

            	    // Public, guest-first tools. Job ownership is enforced in JobService.
            	    .requestMatchers("/api/v1/tools/**").permitAll()
            	    .requestMatchers(HttpMethod.GET, "/api/v1/usage").permitAll()
            	    .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*", "/api/v1/jobs/*/download").permitAll()
            	    .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/chat").permitAll()

            	    .requestMatchers("/api/admin/**").hasRole("ADMIN")
            	    .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")

            	    .anyRequest().authenticated()
            	)
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue"))
                    .accessDeniedHandler((req, res, e) ->
                            writeError(res, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action")))

            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse res, int status, String code, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(
                "{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);

        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.addExposedHeader("Content-Disposition");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
