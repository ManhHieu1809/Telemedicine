package com.hospital.telemedicine.config;

import com.hospital.telemedicine.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(" https://86b8-42-114-121-96.ngrok-free.app")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Lấy JWT token từ header
                    String token = accessor.getFirstNativeHeader("Authorization");

                    if (StringUtils.hasText(token)) {
                        try {
                            // Loại bỏ "Bearer " prefix
                            if (token.startsWith("Bearer ")) {
                                token = token.substring(7);
                            }

                            // Validate token
                            if (jwtTokenProvider.validateToken(token)) {
                                String username = jwtTokenProvider.getUsernameFromToken(token);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                accessor.setUser(authentication);

                                // Lưu userId vào session attributes để sử dụng sau
                                if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
                                    Long userId = ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getId();
                                    accessor.getSessionAttributes().put("userId", userId);
                                    accessor.getSessionAttributes().put("username", username);
                                }

                                System.out.println("WebSocket authenticated user: " + username);
                            } else {
                                System.err.println("Invalid JWT token in WebSocket connection");
                                throw new SecurityException("Invalid JWT token");
                            }
                        } catch (Exception e) {
                            System.err.println("Error authenticating WebSocket: " + e.getMessage());
                            throw new SecurityException("Authentication failed");
                        }
                    } else {
                        System.err.println("No JWT token found in WebSocket connection");
                        throw new SecurityException("No JWT token found");
                    }
                }

                return message;
            }
        });
    }
}