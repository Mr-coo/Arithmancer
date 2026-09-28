package com.arithmancer.entity;

public record Position(double x, double y) {

	public double distanceTo(Position other) {
		return Math.hypot(other.x - x, other.y - y);
	}

}
