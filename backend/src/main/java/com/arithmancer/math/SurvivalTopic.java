package com.arithmancer.math;

import com.fasterxml.jackson.annotation.JsonProperty;

// What survival questions are about, each with its own generator in Question. The host picks one for the whole run;
// the first is the default.
public enum SurvivalTopic {

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
	LOGARITHMS

}
