package com.early_express.inventory_service.global.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class UserHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String userId = request.getHeader("X-User-Id");

        // 이미 인증된 경우 건드리지 않음
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1) 헤더 기반 인증
        if (userId != null) {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 2) /signup 요청만 System 인증 주입
        else if (path.startsWith("/signup")) {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "System", null, Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        if (userId == null) {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "Anonymous", null, Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }


        filterChain.doFilter(request, response);
    }
}
