package com.arithmancer.ws;

import tools.jackson.databind.JsonNode;

// Every WebSocket message, both ways: {"type": "<eventName>", "content": {...}}
public record Event(String type, JsonNode content) {
}
