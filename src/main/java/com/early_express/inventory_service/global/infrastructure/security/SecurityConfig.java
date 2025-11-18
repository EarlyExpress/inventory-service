package com.early_express.inventory_service.global.infrastructure.security;//package com.early_express.product_service.global.infrastructure.security;
//
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final LoginFilter loginFilter;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//        http.csrf(CsrfConfigurer::disable)
//                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
//                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                .authorizeHttpRequests(authorize -> {
//                    authorize.anyRequest().permitAll();
//                })
//                .exceptionHandling(c -> {
//                    c.authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
//                    c.accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED));
//                });
//
//        return http.build();
//    }
//}
