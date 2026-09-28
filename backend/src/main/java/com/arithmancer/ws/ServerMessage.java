package com.arithmancer.ws;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(@JsonSubTypes.Type(value = ServerMessage.RoomCreated.class, name = "roomCreated"))
public sealed interface ServerMessage {

	record RoomCreated(String code, List<String> players) implements ServerMessage {
	}

}
