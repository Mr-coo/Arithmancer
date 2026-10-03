package com.arithmancer.ws;

public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

	record JoinRoom(String code, String nickname) implements ClientMessage {
	}

	record Input(String key, KeyAction action) implements ClientMessage {
	}

	// mode: null starts a real-time game.
	record StartGame(Mode mode) implements ClientMessage {
	}

	record Answer(Integer value) implements ClientMessage {
	}

}
