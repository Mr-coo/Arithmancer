package com.arithmancer.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.arithmancer.entity.Position;

// Finds a way around the trees and stones for a character of the given radius. It heads straight for the goal when
// nothing is in the way, otherwise it searches a grid with A* and heads for the furthest point of that path it can
// walk to in a straight line.
public class PathFinder {

	private static final double CELL_SIZE = 16;
	// Past this many cells the goal is far off screen: head straight for it instead.
	private static final int MAX_EXPANDED = 5_000;
	// A character standing on an obstacle's edge can still walk away along it.
	private static final double EDGE_MARGIN = 0.5;
	private static final int[][] STEPS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 },
			{ -1, -1 } };

	// The decorations' solid circles, grown by the character's radius so its center has to stay out of them.
	private final List<Circle> obstacles;
	private final Set<Long> blocked = new HashSet<>();

	public PathFinder(List<Decoration> decorations, double characterRadius) {
		obstacles = decorations.stream()
				.map(decoration -> new Circle(decoration.solid().center(), decoration.solid().radius() + characterRadius))
				.toList();
		for (Circle obstacle : obstacles) {
			Position center = obstacle.center();
			double radius = obstacle.radius();
			for (int x = cellIndex(center.x() - radius); x <= cellIndex(center.x() + radius); x++) {
				for (int y = cellIndex(center.y() - radius); y <= cellIndex(center.y() + radius); y++) {
					if (center(cell(x, y)).distanceTo(center) < radius) {
						blocked.add(cell(x, y));
					}
				}
			}
		}
	}

	// The point to walk towards, in a straight line, to get from one position to the other.
	public Position waypoint(Position from, Position to) {
		if (isClear(from, to)) {
			return to;
		}
		List<Position> path = findPath(from, to);
		if (path.isEmpty()) {
			return to;
		}
		Position waypoint = path.getFirst();
		for (Position point : path) {
			if (!isClear(from, point)) {
				break;
			}
			waypoint = point;
		}
		return waypoint;
	}

	// Whether walking straight from one position to the other stays out of every obstacle.
	private boolean isClear(Position from, Position to) {
		double dx = to.x() - from.x();
		double dy = to.y() - from.y();
		double lengthSquared = dx * dx + dy * dy;
		for (Circle obstacle : obstacles) {
			Position center = obstacle.center();
			double t = lengthSquared == 0 ? 0
					: Math.clamp(((center.x() - from.x()) * dx + (center.y() - from.y()) * dy) / lengthSquared, 0, 1);
			Position closest = new Position(from.x() + dx * t, from.y() + dy * t);
			if (closest.distanceTo(center) < obstacle.radius() - EDGE_MARGIN) {
				return false;
			}
		}
		return true;
	}

	// The centers of the cells from the one after the start to the goal, which is replaced by the exact position.
	// Empty when no path was found.
	private List<Position> findPath(Position from, Position to) {
		long start = cell(cellIndex(from.x()), cellIndex(from.y()));
		long goal = cell(cellIndex(to.x()), cellIndex(to.y()));
		Map<Long, Double> costs = new HashMap<>(Map.of(start, 0.0));
		Map<Long, Long> previous = new HashMap<>();
		PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::estimate));
		open.add(new Node(start, 0, distance(start, goal)));
		for (int expanded = 0; !open.isEmpty() && expanded < MAX_EXPANDED; expanded++) {
			Node node = open.poll();
			if (node.cost() > costs.get(node.cell())) {
				continue;
			}
			if (node.cell() == goal) {
				return trace(previous, goal, to);
			}
			int x = cellX(node.cell());
			int y = cellY(node.cell());
			for (int[] step : STEPS) {
				long next = cell(x + step[0], y + step[1]);
				// Diagonal steps cannot cut the corner of a blocked cell.
				boolean free = isFree(next, goal) && (step[0] == 0 || step[1] == 0
						|| (isFree(cell(x + step[0], y), goal) && isFree(cell(x, y + step[1]), goal)));
				double cost = node.cost() + Math.hypot(step[0], step[1]);
				if (free && cost < costs.getOrDefault(next, Double.MAX_VALUE)) {
					costs.put(next, cost);
					previous.put(next, node.cell());
					open.add(new Node(next, cost, cost + distance(next, goal)));
				}
			}
		}
		return List.of();
	}

	// The goal is always free: a character can stand closer to an obstacle than a cell's center.
	private boolean isFree(long cell, long goal) {
		return cell == goal || !blocked.contains(cell);
	}

	private static List<Position> trace(Map<Long, Long> previous, long goal, Position to) {
		List<Position> path = new ArrayList<>(List.of(to));
		for (Long cell = previous.get(goal); cell != null && previous.containsKey(cell); cell = previous.get(cell)) {
			path.add(center(cell));
		}
		Collections.reverse(path);
		return path;
	}

	// The shortest distance in cells when moving in 8 directions, ignoring obstacles.
	private static double distance(long from, long to) {
		int dx = Math.abs(cellX(from) - cellX(to));
		int dy = Math.abs(cellY(from) - cellY(to));
		return Math.max(dx, dy) + (Math.sqrt(2) - 1) * Math.min(dx, dy);
	}

	private static int cellIndex(double coordinate) {
		return (int) Math.floor(coordinate / CELL_SIZE);
	}

	// A cell's column and row packed into one number.
	private static long cell(int x, int y) {
		return ((long) x << 32) | (y & 0xFFFFFFFFL);
	}

	private static int cellX(long cell) {
		return (int) (cell >> 32);
	}

	private static int cellY(long cell) {
		return (int) cell;
	}

	private static Position center(long cell) {
		return new Position((cellX(cell) + 0.5) * CELL_SIZE, (cellY(cell) + 0.5) * CELL_SIZE);
	}

	// estimate: the cost so far plus the distance left.
	private record Node(long cell, double cost, double estimate) {
	}

}
