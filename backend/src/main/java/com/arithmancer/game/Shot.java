package com.arithmancer.game;

import com.arithmancer.entity.Enemy;
import com.arithmancer.room.Player;

// A correct answer from a player hitting an enemy, shown by clients as a projectile.
public record Shot(Player player, Enemy enemy) {
}
