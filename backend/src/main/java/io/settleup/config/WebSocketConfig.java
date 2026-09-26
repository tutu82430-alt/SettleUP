package io.settleup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for /topic destinations
        config.enableSimpleBroker("/topic");
        // Prefix for messages sent FROM client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint with SockJS fallback and production CORS origins
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://*.vercel.app",
                        "https://settleup-app.vercel.app",
                        "https://settleup-backend.onrender.com",
                        "*"
                )
                .withSockJS();
    }
}
