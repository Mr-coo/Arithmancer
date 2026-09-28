package com.arithmancer.ws;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration
public class WebSocketDocs {

	@Bean
	public OpenApiCustomizer webSocketPath() {
		return openApi -> openApi.path("/ws", new PathItem().get(new Operation()
				.tags(List.of("WebSocket"))
				.summary("Game WebSocket")
				.description("""
						Connect with a WebSocket client, e.g. `new WebSocket("ws://localhost:8080/ws")`. \
						"Try it out" cannot open a WebSocket.

						Send a text message of exactly one character and the server echoes it back. \
						Any other message closes the connection with status 1007 (Expected one character).""")
				.responses(new ApiResponses()
						.addApiResponse("101", new ApiResponse().description("Switching Protocols")))));
	}

}
