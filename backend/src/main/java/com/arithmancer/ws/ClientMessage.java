package com.arithmancer.ws;

import com.arithmancer.math.Topic;

public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

	record JoinRoom(String code, String nickname) implements ClientMessage {
	}

	record Input(String key, KeyAction action) implements ClientMessage {
	}

	// mode: null starts a real-time game. topic: every question in the run is of it; null asks additions.
	record StartGame(Mode mode, Topic topic) implements ClientMessage {
	}

	record Answer(Integer value) implements ClientMessage {
	}

}
