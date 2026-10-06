package com.arithmancer.math;

import com.fasterxml.jackson.annotation.JsonProperty;

// What survival questions are about, each with its own generator in Question. The host picks one for the whole run;
// the first is the default.
public enum SurvivalTopic {

	@JsonProperty("arithmetic")
	ARITHMETIC,

	@JsonProperty("fractions")
	FRACTIONS,

	@JsonProperty("algebra")
	ALGEBRA,

	@JsonProperty("exponents")
	EXPONENTS,

	@JsonProperty("pythagoras")
	PYTHAGORAS

}
