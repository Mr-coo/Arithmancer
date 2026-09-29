export type PlayerState = {
  nickname: string
  x: number
  y: number
  health: number
  maxHealth: number
  // Seconds until the player can shoot again, out of maxCooldown.
  cooldown: number
  maxCooldown: number
  you: boolean
}

export type EnemyState = { id: number; x: number; y: number; question: string }

// A hit from the last tick: player is an index in players, enemy is the id of the enemy hit.
export type ShotState = { player: number; enemy: number }

export type GameState = { players: PlayerState[]; enemies: EnemyState[]; shots: ShotState[] }

type RoomContent = { code: string; players: string[] }

type ServerMessages = {
  roomCreated: RoomContent
  roomJoined: RoomContent
  gameStarted: RoomContent
  state: GameState
}

type ClientMessages = {
  createRoom: { nickname: string }
  joinRoom: { code: string; nickname: string }
  startGame: Record<string, never>
  input: { key: string; action: 'down' | 'up' }
  answer: { value: number }
}

type Handler = (content: never) => void

export type Connection = {
  send: <T extends keyof ClientMessages>(type: T, content: ClientMessages[T]) => void
  on: <T extends keyof ServerMessages>(type: T, handler: (content: ServerMessages[T]) => void) => () => void
}

// Opens the game WebSocket. Resolves once it is open; onClose gets the server's close reason.
export function connect(onClose: (reason: string) => void): Promise<Connection> {
  return new Promise((resolve) => {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const socket = new WebSocket(`${protocol}://${location.host}/ws`)
    const handlers = new Map<string, Set<Handler>>()

    socket.onmessage = (event) => {
      const { type, content } = JSON.parse(event.data) as { type: string; content: never }
      handlers.get(type)?.forEach((handler) => handler(content))
    }
    socket.onclose = (event) => onClose(event.reason)
    socket.onopen = () =>
      resolve({
        send: (type, content) => socket.send(JSON.stringify({ type, content })),
        on: (type, handler) => {
          const set = handlers.get(type) ?? new Set()
          handlers.set(type, set.add(handler as Handler))
          return () => set.delete(handler as Handler)
        },
      })
  })
}
