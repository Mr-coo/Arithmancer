package com.arithmancer.math;

import com.fasterxml.jackson.annotation.JsonProperty;

// What turn-based problems are about, each with its own generator in Problem. The host picks one for the whole battle;
// the first is the default.
public enum BattleTopic {

	@JsonProperty("limits")
	LIMITS

}
