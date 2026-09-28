package com.arithmancer.ws;

public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

	record JoinRoom(String code, String nickname) implements ClientMessage {
	}

	record Input(String key) implements ClientMessage {
	}

}
