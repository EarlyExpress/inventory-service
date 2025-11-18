package com.early_express.inventory_service.global.infrastructure.security;

import com.early_express.inventory_service.global.infrastructure.security.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * API 게이트웨이 헤더를 통해 전달된 사용자 정보를 바탕으로 로그인 처리
 * */
@Component
public class LoginFilter extends GenericFilterBean {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_ROLES = "X-User-Roles";
    private static final String HEADER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_NAME = "X-User-Name";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        doLogin((HttpServletRequest) request);
        // 다운 케스팅을 통해 헤더로 접근해야된다.

        chain.doFilter(request, response);
    }
    
    // 로그인처리
    private void doLogin(HttpServletRequest request) {
        String id = request.getHeader(HEADER_USER_ID); //필수항목
        String username = request.getHeader(HEADER_USERNAME); //필수항목
        String name = request.getHeader(HEADER_USER_NAME);
        String email = request.getHeader(HEADER_EMAIL);
        String roles = request.getHeader(HEADER_ROLES);

        // id 유무 확인
        if(!StringUtils.hasText(id) || !StringUtils.hasText(username)) return;

        // 사용자명 확인
        name = name == null ? null : URLDecoder.decode(name, StandardCharsets.UTF_8);

        UserDetails userDetails = UserDetailsImpl.builder()
                .id(UUID.fromString(id))
                .username(username)
                .name(name)
                .email(email)
                .roles(roles)
                .build();

        // 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 인증 객체를 컨텍스트 홀더에 인증 객체를 넣어 필터 체인을 통과하도록 설계한다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
