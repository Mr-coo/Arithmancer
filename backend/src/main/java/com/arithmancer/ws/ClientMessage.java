package com.arithmancer.ws;

public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

	record JoinRoom(String code, String nickname) implements ClientMessage {
	}

	record Input(String key, KeyAction action) implements ClientMessage {
	}

	// mode: null starts a real-time game. topic: one of the mode's topics, which every question in the run is of; null
	// is the mode's first.
	record StartGame(Mode mode, String topic) implements ClientMessage {
	}

	record Answer(Integer value) implements ClientMessage {
	}

}
