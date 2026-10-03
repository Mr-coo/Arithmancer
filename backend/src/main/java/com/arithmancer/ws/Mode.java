package com.arithmancer.ws;

import com.fasterxml.jackson.annotation.JsonProperty;

// What a room plays: the endless real-time game, or the turn-based battle.
public enum Mode {

	@JsonProperty("realTime")
	REAL_TIME,

	@JsonProperty("turnBased")
	TURN_BASED

}
