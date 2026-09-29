package com.arithmancer.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@Configuration
public class WebSocketDocs {

	private static final String PRESETS = "presets: [";

	// Swagger UI can only send HTTP, so "Try it out" on /ws opens a real WebSocket instead of fetching.
	private static final String WS_INTERCEPTOR = """
			requestInterceptor: (request) => {
				const url = new URL(request.url, document.location.href);
				if (url.pathname !== '/ws') return request;
				const message = url.searchParams.get('message') ?? '';
				url.protocol = url.protocol.replace('http', 'ws');
				url.search = '';
				request.userFetch = () => new Promise((resolve, reject) => {
					const socket = new WebSocket(url);
					let opened = false;
					const respond = (body) => resolve({
						ok: true, url: url.href, status: 101, statusText: 'Switching Protocols',
						headers: new Headers({ 'content-type': 'application/json' }),
						text: async () => JSON.stringify(body, null, 2)
					});
					socket.onopen = () => { opened = true; socket.send(message); };
					socket.onmessage = (event) => { respond({ sent: message, received: event.data }); socket.close(); };
					socket.onclose = (event) => opened
						? respond({ sent: message, closeCode: event.code, closeReason: event.reason })
						: reject(new Error('Could not open the WebSocket'));
				});
				return request;
			},
			""";

	@Bean
	public OpenApiCustomizer webSocketPath() {
		return openApi -> openApi.path("/ws", new PathItem().get(new Operation()
				.tags(List.of("WebSocket"))
				.summary("Game WebSocket")
				.description("""
						Connect with a WebSocket client, e.g. `new WebSocket("ws://localhost:8080/ws")`. \
						"Try it out" opens a real WebSocket, sends `message` and shows the reply.

						Every message, both ways, is `{"type": "<eventName>", "content": {...}}`:
						- `{"type":"createRoom","content":{"nickname":"Marco"}}` creates a room with you in it. \
						Reply: `{"type":"roomCreated","content":{"code":"KQXB","players":["Marco"]}}`
						- `{"type":"joinRoom","content":{"code":"KQXB","nickname":"Ana"}}` joins a room (max 4 players). \
						Reply, to everyone in the room: `{"type":"roomJoined","content":{"code":"KQXB","players":["Marco","Ana"]}}`
						- `{"type":"input","content":{"key":"a","action":"down"}}` sends a key press (`down`) or release (`up`) \
						of exactly one letter. \
						Reply: the same message echoed back.
						- `{"type":"startGame","content":{}}` starts the game. Only the host (room creator) can send it, \
						and nobody can join after. \
						Reply, to every player: `{"type":"gameStarted","content":{"code":"KQXB","players":["Marco","Ana"]}}`
						- `{"type":"answer","content":{"value":4}}` answers a question during a game. Of the enemies in your \
						view with that answer, the nearest takes a hit. After a hit, your answers are ignored for a \
						cooldown (1 second). No reply: the hit shows up in the next state.

						During a game, every player gets the state 20 times per second. `you` marks the receiving player, \
						and `shots` lists the hits from that tick (`player` is an index in `players`): \
						`{"type":"state","content":{"players":[{"nickname":"Marco","x":0.0,"y":-10.0,"health":100,"maxHealth":100,"cooldown":0.5,"maxCooldown":1.0,"you":true}],"enemies":[{"id":0,"x":420.0,"y":-310.0,"question":"7 - 3"}],"shots":[{"player":0,"enemy":1}]}}`

						Anything else closes the connection with status 1007 (bad data).""")
				.addParametersItem(new QueryParameter()
						.name("message")
						.required(true)
						.description("Swagger UI only: sent as a text message once the socket is open.")
						.example("{\"type\":\"createRoom\",\"content\":{\"nickname\":\"Marco\"}}")
						.schema(new StringSchema()))
				.responses(new ApiResponses()
						.addApiResponse("101", new ApiResponse().description("Switching Protocols")))));
	}

	@Bean
	public SwaggerIndexTransformer webSocketSwaggerIndexTransformer(SwaggerUiConfigProperties swaggerUiConfig,
			SwaggerUiOAuthProperties swaggerUiOAuthProperties, SwaggerWelcomeCommon swaggerWelcomeCommon,
			ObjectMapperProvider objectMapperProvider) {
		return new SwaggerIndexPageTransformer(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon,
				objectMapperProvider) {
			@Override
			protected String defaultTransformations(SwaggerUiConfigParameters swaggerUiConfigParameters,
					InputStream inputStream) throws IOException {
				return super.defaultTransformations(swaggerUiConfigParameters, inputStream)
						.replace(PRESETS, WS_INTERCEPTOR + PRESETS);
			}
		};
	}

}
