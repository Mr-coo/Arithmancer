package com.arithmancer.room;

import java.util.List;

public record Room(String code, List<Player> players) {
}
