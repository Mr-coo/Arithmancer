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
						- `{"type":"startGame","content":{"mode":"realTime"}}` starts the game. Only the host (room creator) can \
						send it, and nobody can join after. `mode` is `realTime` (the default) or `turnBased`. \
						For a real-time game, the reply, to every player, comes with the trees and stones on the map. Players and enemies cannot walk into a \
						decoration's solid circle (`x`, `y`, `radius`); a character touching its cover circle is behind it. \
						`details` are bushes, mushrooms, pebbles, pumpkins and bones on the ground, which characters walk over: \
						`{"type":"gameStarted","content":{"code":"KQXB","players":["Marco","Ana"],"decorations":[{"type":"tree","x":320.0,"y":-140.0,"radius":14.0,"coverX":320.0,"coverY":-207.0,"coverRadius":50.0}],"details":[{"type":"mushroom","x":150.0,"y":80.0}]}}`
						- `{"type":"answer","content":{"value":4}}` answers a question during a game. Of the enemies in your \
						view with that answer, the nearest takes a hit. After any answer, right or wrong, your answers are \
						ignored for a cooldown (1 second). No reply: the hit shows up in the next state. \
						In a turn-based game, `value` is the option you pick for your problem.

						During a game, every player gets the state 20 times per second. `time` is the seconds since the run \
						started, `score` counts the enemies a player killed, `you` marks the receiving player, \
						and `shots` lists the hits from that tick (`player` is an index in `players`): \
						`{"type":"state","content":{"time":12.35,"players":[{"nickname":"Marco","x":0.0,"y":-10.0,"health":100,"maxHealth":100,"cooldown":0.25,"maxCooldown":0.5,"score":3,"revive":0.0,"haste":0.0,"speedBoost":0.0,"you":true}],"enemies":[{"id":0,"type":"goblin","x":420.0,"y":-310.0,"question":"7 - 3","health":1,"maxHealth":1}],"items":[{"id":0,"type":"heal","x":120.0,"y":40.0}],"shots":[{"player":0,"enemy":1}]}}`

						When every player is dead (health 0), the game ends and every player gets the results. \
						`time` is how long the team survived: \
						`{"type":"gameOver","content":{"time":83.2,"players":[{"nickname":"Marco","score":5,"you":true},{"nickname":"Ana","score":3,"you":false}]}}`

						A turn-based game starts with `{"type":"battleStarted","content":{"code":"KQXB","players":["Marco","Ana"]}}`. \
						Then every player gets the battle 20 times per second. On the players' turn (`phase` `players`), \
						everyone answers their own `problem` by picking one of its `options`. Each right answer adds to your \
						`charge`, one more than the last (1, 2, 3...); a wrong one locks your answers for `locked` seconds \
						(3). The problem's `id` changes with every new problem. On the enemy's turn (`phase` `enemy`), the \
						warriors with a charge strike the goblin for it: `strikes` lists them (indices in `players`) on that \
						tick. 2.8 seconds into the enemy's turn (of 6), a beaten goblin is replaced by the next one; \
						otherwise the goblin strikes every standing player, listed in `hits` on that tick. `score` is the \
						damage a player dealt: \
						`{"type":"battleState","content":{"turn":3,"phase":"players","secondsLeft":9.45,"players":[{"nickname":"Marco","health":100,"maxHealth":100,"charge":3,"score":12,"you":true}],"enemy":{"id":1,"type":"goblin","health":20,"maxHealth":30},"problem":{"id":14,"text":"7 + 5","options":[11,12,4,17]},"locked":0.0,"strikes":[],"hits":[]}}`

						When every player is downed (health 0), the battle ends and every player gets the results: the \
						`turns` the team lasted, the goblins `beaten`, and each player's damage dealt as `score`: \
						`{"type":"battleOver","content":{"turns":9,"beaten":4,"players":[{"nickname":"Marco","score":61,"you":true},{"nickname":"Ana","score":48,"you":false}]}}`

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
