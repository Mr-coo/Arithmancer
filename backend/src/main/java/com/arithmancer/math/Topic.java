package com.arithmancer.math;

import com.fasterxml.jackson.annotation.JsonProperty;

// The kinds of questions, each with its own generator in Question. The host picks one for the whole run.
public enum Topic {

	@JsonProperty("addition")
	ADDITION,

	@JsonProperty("subtraction")
	SUBTRACTION,

	@JsonProperty("multiplication")
	MULTIPLICATION,

	@JsonProperty("division")
	DIVISION,

	@JsonProperty("powers")
	POWERS,

	@JsonProperty("logarithms")
	LOGARITHMS,

	@JsonProperty("limits")
	LIMITS

}
