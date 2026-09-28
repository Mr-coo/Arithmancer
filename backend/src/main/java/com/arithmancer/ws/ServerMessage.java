package com.arithmancer.ws;

import java.util.List;

public sealed interface ServerMessage {

	record RoomCreated(String code, List<String> players) implements ServerMessage {
	}

}
