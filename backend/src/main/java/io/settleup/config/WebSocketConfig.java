package io.settleup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${ALLOWED_ORIGIN:${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://*.vercel.app}}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for /topic destinations
        config.enableSimpleBroker("/topic");
        // Prefix for messages sent FROM client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> originsList = new ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        originsList.add("http://localhost:3000");
        originsList.add("http://localhost:5173");
        originsList.add("https://*.vercel.app");
        originsList.add("*");

        // STOMP endpoint with SockJS fallback and production CORS origins
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(originsList.toArray(new String[0]))
                .withSockJS();
    }
}
