package com.arithmancer.room;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

@Component
public class RoomRegistry {

	private static final String CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int CODE_LENGTH = 4;

	private final Map<String, Room> rooms = new ConcurrentHashMap<>();

	public Room create(Player host) {
		while (true) {
			Room room = new Room(randomCode(), new CopyOnWriteArrayList<>(List.of(host)));
			if (rooms.putIfAbsent(room.code(), room) == null) {
				return room;
			}
		}
	}

	public Room find(String code) {
		return code == null ? null : rooms.get(code);
	}

	public Room findBySession(String sessionId) {
		return rooms.values().stream()
				.filter(room -> room.players().stream().anyMatch(player -> player.getSessionId().equals(sessionId)))
				.findFirst()
				.orElse(null);
	}

	public boolean remove(Room room) {
		return rooms.remove(room.code(), room);
	}

	private String randomCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(CODE_LETTERS.charAt(ThreadLocalRandom.current().nextInt(CODE_LETTERS.length())));
		}
		return code.toString();
	}

}
