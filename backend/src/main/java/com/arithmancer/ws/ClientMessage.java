package com.arithmancer.ws;

public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

}
