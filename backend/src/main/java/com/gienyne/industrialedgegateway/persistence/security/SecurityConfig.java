package com.gienyne.industrialedgegateway.persistence.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;


@Configuration
public class SecurityConfig{


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(){
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository) throws Exception{

        http.csrf(csrf ->
        csrf.csrfTokenRepository(csrfTokenRepository)
        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
        ).authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
        .requestMatchers("/error").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
        .anyRequest().authenticated()
        ).exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        ).formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

}