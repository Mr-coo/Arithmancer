package com.arithmancer.ws;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(@JsonSubTypes.Type(value = ClientMessage.CreateRoom.class, name = "createRoom"))
public sealed interface ClientMessage {

	record CreateRoom(String nickname) implements ClientMessage {
	}

}
