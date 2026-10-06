export type PlayerState = {
  nickname: string
  x: number
  y: number
  health: number
  maxHealth: number
  // Seconds until the player can shoot again, out of maxCooldown.
  cooldown: number
  maxCooldown: number
  // Enemies this player killed.
  score: number
  // While downed, how far a teammate has got reviving them, from 0 to 1.
  revive: number
  // Seconds left of the item boosts: faster shots and faster moves.
  haste: number
  speedBoost: number
  you: boolean
}

export type EnemyType = 'goblin' | 'torch'

// health: answers still needed to kill it, out of maxHealth.
export type EnemyState = {
  id: number
  type: EnemyType
  x: number
  y: number
  question: string
  health: number
  maxHealth: number
}

// A hit from the last tick: player is an index in players, enemy is the id of the enemy hit.
export type ShotState = { player: number; enemy: number }

// time: seconds since the run started.
// Dropped by a killed enemy: heal restores health, haste makes shots cool down faster, speed makes moves faster.
export type ItemType = 'heal' | 'haste' | 'speed'

export type ItemState = { id: number; type: ItemType; x: number; y: number }

// round: the current round, whose herd starts coming in roundStartsIn seconds, 0 once it has.
export type GameState = {
  time: number
  round: number
  roundStartsIn: number
  players: PlayerState[]
  enemies: EnemyState[]
  items: ItemState[]
  shots: ShotState[]
}

export type FinalScore = { nickname: string; score: number; you: boolean }

// time: seconds the team survived.
export type GameOver = { time: number; players: FinalScore[] }

// A tree or stone: players cannot walk into the solid circle (x, y, radius), and a character touching the cover
// circle is behind it.
export type DecorationState = {
  type: 'tree' | 'stone'
  x: number
  y: number
  radius: number
  coverX: number
  coverY: number
  coverRadius: number
}

// Drawn on the ground at (x, y). Characters walk over it.
export type DetailState = { type: 'bush' | 'mushroom' | 'pebble' | 'pumpkin' | 'bone'; x: number; y: number }

// What a room plays: the endless real-time game, or the turn-based battle.
export type Mode = 'realTime' | 'turnBased'

// What every question in a run is about, picked by the host.
export type Topic = 'addition' | 'subtraction' | 'multiplication' | 'division' | 'powers' | 'logarithms' | 'limits'

// charge: the damage this player's right answers add up to this turn. score: damage dealt over the battle.
export type FighterState = {
  nickname: string
  health: number
  maxHealth: number
  charge: number
  score: number
  you: boolean
}

export type FoeState = { id: number; type: EnemyType; health: number; maxHealth: number }

// id changes with every new problem.
export type ProblemState = { id: number; text: string; options: number[] }

// problem and locked (seconds until a wrong answer stops locking your options) are yours. strikes: indices in players
// of the warriors striking the goblin on this tick; hits: of the players the goblin strikes.
export type BattleState = {
  turn: number
  phase: 'players' | 'enemy'
  secondsLeft: number
  players: FighterState[]
  enemy: FoeState
  problem: ProblemState
  locked: number
  strikes: number[]
  hits: number[]
}

// turns: the turns the team lasted. beaten: goblins beaten. Each player's score is the damage they dealt.
export type BattleOver = { turns: number; beaten: number; players: FinalScore[] }

type RoomContent = { code: string; players: string[] }

type ServerMessages = {
  roomCreated: RoomContent
  // host: whether you are the host, the first of players.
  roomJoined: RoomContent & { host: boolean }
  gameStarted: RoomContent & { decorations: DecorationState[]; details: DetailState[] }
  state: GameState
  gameOver: GameOver
  battleStarted: RoomContent
  battleState: BattleState
  battleOver: BattleOver
}

type ClientMessages = {
  createRoom: { nickname: string }
  joinRoom: { code: string; nickname: string }
  startGame: { mode: Mode; topic: Topic }
  input: { key: string; action: 'down' | 'up' }
  answer: { value: number }
}

type Handler = (content: never) => void

export type Connection = {
  send: <T extends keyof ClientMessages>(type: T, content: ClientMessages[T]) => void
  on: <T extends keyof ServerMessages>(type: T, handler: (content: ServerMessages[T]) => void) => () => void
  // Closes the socket without calling onClose.
  close: () => void
}

// Opens the game WebSocket. Resolves once it is open; onClose gets the server's close reason, or says the server could
// not be reached when the socket never opened.
export function connect(onClose: (reason: string) => void): Promise<Connection> {
  return new Promise((resolve) => {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const socket = new WebSocket(`${protocol}://${location.host}/ws`)
    const handlers = new Map<string, Set<Handler>>()
    let opened = false
    let closedByUs = false

    socket.onmessage = (event) => {
      const { type, content } = JSON.parse(event.data) as { type: string; content: never }
      handlers.get(type)?.forEach((handler) => handler(content))
    }
    socket.onclose = (event) => {
      if (!closedByUs) {
        onClose(opened ? event.reason : 'Could not reach the server')
      }
    }
    socket.onopen = () => {
      opened = true
      resolve({
        close: () => {
          closedByUs = true
          socket.close()
        },
        send: (type, content) => socket.send(JSON.stringify({ type, content })),
        on: (type, handler) => {
          const set = handlers.get(type) ?? new Set()
          handlers.set(type, set.add(handler as Handler))
          return () => set.delete(handler as Handler)
        },
      })
    }
  })
}
