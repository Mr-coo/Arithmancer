package com.arithmancer.map;

import com.arithmancer.entity.Position;

// A mushroom, bush, pebble, pumpkin or bone on the ground. It is only drawn: characters walk over it.
public record Detail(String type, Position position) {
}
