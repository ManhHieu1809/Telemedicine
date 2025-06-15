package com.hospital.telemedicine.config;

import com.hospital.telemedicine.security.JwtTokenProvider;
import com.hospital.telemedicine.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    private final List<String> allowedPaths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/password/forgot-password",
            "/api/password/verify-otp",
            "/api/password/reset-password",
            "/admin/**",
            "/ws/**",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String path = request.getRequestURI();

            // Bỏ qua xác thực cho các đường dẫn trong allowedPaths
            if (shouldSkipAuthentication(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getTokenFromRequest(request);

            if (jwt == null) {
                // Trả về 401 nếu không có token
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Không có token xác thực");
                return;
            }

            if (tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Trả về 401 nếu token không hợp lệ
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
                return;
            }
        } catch (Exception e) {
            logger.error("Không thể thiết lập xác thực người dùng: {}", e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication failed: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String currentPath) {
        return allowedPaths.stream()
                .anyMatch(path -> pathMatcher.match(path, currentPath));
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}