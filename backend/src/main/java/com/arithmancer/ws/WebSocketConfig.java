package com.arithmancer.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final GameGateway gameGateway;

	public WebSocketConfig(GameGateway gameGateway) {
		this.gameGateway = gameGateway;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(gameGateway, "/ws").setAllowedOrigins("http://localhost:5173");
	}

}
