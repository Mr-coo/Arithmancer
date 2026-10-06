# Guidelines

## Scope
- Only do the task that has been asked, as minimally as possible.

## Commits
- Format: `<type>: <short message>`. For example, `feat: add hello endpoint`.
- `<type>` must be one of `feat`, `fix`, `del`, `refactor`.
- The message is a single short line with no body.
- Do not add `Co-Authored-By` or any other trailer.
- Make one commit only has 1 feature
- Commit after each feature is done, without waiting to be asked.

## Testing
- To test a change, push it to `master`, wait for it to deploy, and test it on https://arithmancer.marcolinardi.site instead of running it locally.

## Game design
Arithmancer is an endless co-op browser game. Players survive enemies by solving math questions shown above the enemies' heads. All numbers here are starting values to tune during development. There are two modes, which the host picks in the lobby: real-time, described first, and turn-based.

### Core loop
- 2D top-down. Move with WASD or the arrow keys. Type answers with the digit keys, Backspace and Enter.
- Enemies come in rounds, chase the nearest player who is not downed, and deal contact damage.
- Each enemy shows a math question above its head. When a player submits a number, the enemy whose answer matches takes a hit. If several match, the enemy nearest that player is hit.
- A player can only hit enemies inside their own view.
- Questions are shared: all players see the same question on an enemy, and the first correct answer lands the hit.
- A wrong answer locks that player's input for about 1 second.
- There is no win condition. The run ends when every player is downed.

### Enemies
- Basic enemies die after 1 correct answer. Tougher types appear over time and need 2–3 answers. Each hit gives the enemy a new question.
- Each round is a herd, bigger and spawning faster than the last. A round starts after a 5-second break, and the next one once its herd is all gone.
- Enemy speed and the share of tougher types increase over time.

### Math
- Real-time answers are always whole numbers ≥ 0. Turn-based answers are exact and can be fractions, roots, π or expressions, as they are picked from options.
- Each mode has its own topics, and the host picks one of them in the lobby. Every question in the run is of that topic.
  - Real-time (survival): arithmetic (+ − × ÷ mixed, with brackets), integers and fractions, algebra (solve ax + b = c), exponents and logarithms, or Pythagoras.
  - Turn-based: trigonometry (exact values of special angles), limits, derivatives or integrals (of polynomials), matrices (determinants), statistics (mean, median, mode and range) or geometry (areas, perimeters, angles and volumes).
- Difficulty scales over time: questions go from easy to medium after 60 seconds and to hard after 150.

### Players
- 1–4 players per room.
- A player at 0 HP is downed. A teammate who stays next to them for a moment revives them.

### Map
- A large fixed-size world, several screens wide, with walls at the edges.
- Each player's camera follows them, and players can split up.
- Enemies spawn just outside the players' views.

### Turn-based mode
- A side-view battle: the players' warriors on the left, one goblin at a time on the right, with trees, rocks, bushes, mushrooms, pumpkins and bones scattered around them, the same for everyone in the room.
- The players' turn and the goblin's turn alternate.
- On the players' turn (15 seconds), everyone answers at once. Each player gets their own math problem with 4 options, picked with the keys 1–4 or a click. The options are the exact answer and 3 likely mistakes, topped up with numbers near the answer; the whole answer counts, not just its last digit.
- Each right answer adds to the player's charge, one more than the last: 1, then 2, then 3. A wrong answer locks that player's options for 3 seconds while the turn goes on. Every answer brings a new problem.
- When the players' turn ends, every warrior with a charge strikes the goblin for it. A beaten goblin is replaced by the next one; otherwise the goblin strikes every standing player for 10.
- Players have 100 health. A player at 0 is downed and cannot answer. The run ends when every player is downed, with the goblins beaten and turns lasted; each player's score is the damage they dealt.

### Rooms
- A player creates a room and gets a short room code. Others use the code to join the lobby.
- The host picks the mode, then one of its topics, and starts the run. Nobody can join mid-run.

### Scores
- There are no accounts. Players enter a nickname.
- When a run ends, the team's score is saved to a leaderboard: time survived, kills and player nicknames.

## Architecture
- The server is authoritative. For each room, the Spring Boot backend runs the simulation (movement, spawning, questions, answer checks, damage, revives) at about 20 ticks per second and broadcasts the state over WebSocket.
- Browsers only send input (movement and answers) and draw the state. React handles the menus, lobby and leaderboard, and Phaser renders the game view.
- The server decides whether an enemy is in a player's view using a fixed logical viewport centered on that player, so screen size doesn't affect who can hit what.
- The leaderboard is stored in PostgreSQL, run in docker-compose alongside the backend.