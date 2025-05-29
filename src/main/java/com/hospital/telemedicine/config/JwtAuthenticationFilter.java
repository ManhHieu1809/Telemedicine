package com.hospital.telemedicine.config;

import com.hospital.telemedicine.security.JwtTokenProvider;
import com.hospital.telemedicine.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Danh sách các đường dẫn được phép truy cập mà không cần xác thực
    private final List<String> allowedPaths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/password/forgot-password",
            "/api/password/verify-otp",
            "/api/password/reset-password",
            "/ws/**", // WebSocket endpoint
            "/favicon.ico"

    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();

            // Kiểm tra nếu request đi đến các đường dẫn được cho phép không cần xác thực
            if (shouldSkipAuthentication(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = tokenProvider.getJwtFromRequest(request);

            if (jwt != null && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
        } catch (Exception e) {
            logger.error("Không thể thiết lập xác thực người dùng: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String currentPath) {
        return allowedPaths.stream()
                .anyMatch(path -> pathMatcher.match(path, currentPath));
    }
}