package com.arithmancer.map;

import com.arithmancer.entity.Position;

// A tree or stone. Players cannot walk into its solid circle (enemies are ghosts and pass through),
// and a character touching its cover circle is behind it.
public record Decoration(String type, Circle solid, Circle cover) {

	// Starting sizes, to tune during development. The cover sits above the base, like a tree's canopy.
	public static Decoration tree(Position base) {
		return new Decoration("tree", new Circle(base, 14), new Circle(new Position(base.x(), base.y() - 36), 44));
	}

	public static Decoration stone(Position base) {
		return new Decoration("stone", new Circle(base, 24), new Circle(new Position(base.x(), base.y() - 14), 30));
	}

}
