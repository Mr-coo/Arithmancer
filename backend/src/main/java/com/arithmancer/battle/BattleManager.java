package com.arithmancer.battle;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arithmancer.room.Player;
import com.arithmancer.room.Room;
import com.arithmancer.ws.ServerMessage.BattleOver;
import com.arithmancer.ws.ServerMessage.BattleState;
import com.arithmancer.ws.ServerMessage.FighterState;
import com.arithmancer.ws.ServerMessage.FinalScore;
import com.arithmancer.ws.ServerMessage.FoeState;
import com.arithmancer.ws.ServerMessage.ProblemState;
import com.arithmancer.ws.SessionRegistry;

@Component
public class BattleManager {

	private static final Logger log = LoggerFactory.getLogger(BattleManager.class);

	private static final int TICKS_PER_SECOND = 20;

	private final List<Battle> battles = new CopyOnWriteArrayList<>();
	private final SessionRegistry sessionRegistry;

	public BattleManager(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	public Battle start(Room room) {
		Battle battle = new Battle(room.code(), room.players());
		battles.add(battle);
		log.info("Started battle {} with {} players", battle.getCode(), battle.getPlayers().size());
		return battle;
	}

	public Player findPlayer(String sessionId) {
		return battles.stream()
				.flatMap(battle -> battle.getPlayers().stream())
				.filter(player -> player.getSessionId().equals(sessionId))
				.findFirst()
				.orElse(null);
	}

	@Scheduled(fixedRate = 1000 / TICKS_PER_SECOND)
	void loop() {
		for (Battle battle : battles) {
			// A battle that fails is ended, so the others keep going.
			try {
				battle.tick(1.0 / TICKS_PER_SECOND);
				sendState(battle);
				if (battle.isOver()) {
					end(battle);
				}
			} catch (RuntimeException e) {
				log.error("Battle {} failed", battle.getCode(), e);
				end(battle);
			}
		}
	}

	// Each player's score is the damage they dealt.
	private void end(Battle battle) {
		battles.remove(battle);
		log.info("Battle {} over after {} turns, {} goblins beaten", battle.getCode(), battle.getTurn(),
				battle.getBeaten());
		for (Player recipient : battle.getPlayers()) {
			List<FinalScore> scores = battle.getPlayers().stream()
					.map(player -> new FinalScore(player.getNickname(), battle.getFighter(player).getDamageDealt(),
							player == recipient))
					.toList();
			sessionRegistry.send(recipient.getSessionId(), "battleOver",
					new BattleOver(battle.getTurn(), battle.getBeaten(), scores));
		}
	}

	private void sendState(Battle battle) {
		List<Player> players = battle.getPlayers();
		Foe foe = battle.getFoe();
		FoeState enemy = new FoeState(foe.getId(), foe.getType().name().toLowerCase(Locale.ROOT), foe.getHealth(),
				foe.getMaxHealth());
		List<Integer> strikes = battle.getStrikes().stream().map(players::indexOf).toList();
		List<Integer> hits = battle.getHits().stream().map(players::indexOf).toList();
		for (Player recipient : players) {
			List<FighterState> fighters = players.stream().map(player -> {
				Fighter fighter = battle.getFighter(player);
				return new FighterState(player.getNickname(), player.getHealth(), player.getMaxHealth(),
						fighter.getCharge(), fighter.getDamageDealt(), player == recipient);
			}).toList();
			Fighter you = battle.getFighter(recipient);
			ProblemState problem = new ProblemState(you.getProblemId(), you.getProblem().text(), you.getOptions());
			sessionRegistry.send(recipient.getSessionId(), "battleState",
					new BattleState(battle.getTurn(), battle.getPhase().name().toLowerCase(Locale.ROOT),
							battle.getSecondsLeft(), fighters, enemy, problem, you.getLockSeconds(), strikes, hits));
		}
	}

}
