import { type ReactNode, useState } from 'react'
import { BattleView } from './BattleView'
import {
  connect,
  type Connection,
  type DecorationState,
  type DetailState,
  type FinalScore,
  type Mode,
  type Topic,
} from './connection'
import { formatTime } from './format'
import { GameView } from './GameView'
import { HeroScene } from './HeroScene'
import { HomeBackdrop } from './HomeBackdrop'
import './App.css'

type Screen =
  | { name: 'home'; error?: string }
  | { name: 'lobby'; code: string; players: string[]; host: boolean }
  | { name: 'game'; decorations: DecorationState[]; details: DetailState[] }
  | { name: 'battle'; code: string }
  | { name: 'gameOver'; summary: string; players: FinalScore[] }

// The server turns longer nicknames away.
const MAX_NICKNAME_LENGTH = 16

type Choice<T> = { value: T; label: string }

// The host's choices: the mode, then one of its topics for the run's questions, in the menus' order.
const MODES: Choice<Mode>[] = [
  { value: 'realTime', label: 'Survival' },
  { value: 'turnBased', label: 'Turn-based' },
]
const TOPICS: Record<Mode, Choice<Topic>[]> = {
  realTime: [
    { value: 'arithmetic', label: 'Arithmetic' },
    { value: 'fractions', label: 'Integers & fractions' },
    { value: 'algebra', label: 'Algebra' },
    { value: 'exponents', label: 'Exponents & logarithms' },
    { value: 'pythagoras', label: 'Pythagoras' },
  ],
  turnBased: [
    { value: 'trigonometry', label: 'Trigonometry' },
    { value: 'limits', label: 'Limits' },
    { value: 'derivatives', label: 'Derivatives' },
  ],
}

// "1 goblin", "4 goblins".
const count = (n: number, noun: string) => `${n} ${noun}${n === 1 ? '' : 's'}`

function App() {
  const [screen, setScreen] = useState<Screen>({ name: 'home' })
  const [connection, setConnection] = useState<Connection>()
  const [nickname, setNickname] = useState('')
  const [code, setCode] = useState('')
  const [mode, setMode] = useState<Mode>('realTime')
  const [topic, setTopic] = useState<Topic>(TOPICS.realTime[0].value)
  // Set until the room answers or the socket closes, so a second click cannot open another socket.
  const [connecting, setConnecting] = useState(false)
  // Set once the host clicks start, so a second click cannot send another start.
  const [starting, setStarting] = useState(false)

  async function open() {
    setConnecting(true)
    setStarting(false)
    const opened = await connect((reason) => {
      setConnection(undefined)
      setConnecting(false)
      setScreen({ name: 'home', error: reason || 'Disconnected from the server' })
    })
    opened.on('roomCreated', ({ code, players }) => {
      setConnecting(false)
      setScreen({ name: 'lobby', code, players, host: true })
    })
    opened.on('roomJoined', ({ code, players, host }) => {
      setConnecting(false)
      setScreen({ name: 'lobby', code, players, host })
    })
    opened.on('gameStarted', ({ decorations, details }) => setScreen({ name: 'game', decorations, details }))
    opened.on('gameOver', ({ time, players }) =>
      setScreen({ name: 'gameOver', summary: `You survived ${formatTime(time)}.`, players }),
    )
    opened.on('battleStarted', ({ code }) => setScreen({ name: 'battle', code }))
    opened.on('battleOver', ({ turns, beaten, players }) => {
      const summary = `You beat ${count(beaten, 'goblin')} in ${count(turns, 'turn')}.`
      setScreen({ name: 'gameOver', summary, players })
    })
    setConnection(opened)
    return opened
  }

  function backToMenu() {
    connection?.close()
    setConnection(undefined)
    setScreen({ name: 'home' })
  }

  // Each mode has its own topics: a new mode starts from its first.
  function pickMode(picked: Mode) {
    setMode(picked)
    setTopic(TOPICS[picked][0].value)
  }

  function start() {
    setStarting(true)
    connection?.send('startGame', { mode, topic })
  }

  async function createRoom() {
    const opened = await open()
    opened.send('createRoom', { nickname: nickname.trim() })
  }

  async function joinRoom() {
    const opened = await open()
    opened.send('joinRoom', { code: code.trim().toUpperCase(), nickname: nickname.trim() })
  }

  if (screen.name === 'game' && connection) {
    return (
      <>
        <GameView connection={connection} decorations={screen.decorations} details={screen.details} />
        <p className="hint">Move with WASD or the arrow keys. Press the last digit of an enemy's answer to shoot it.</p>
      </>
    )
  }

  if (screen.name === 'battle' && connection) {
    return (
      <>
        <BattleView connection={connection} code={screen.code} />
        <p className="hint">Pick with 1–4 or a click. Right answers hit harder; wrong ones cost you 3 seconds.</p>
      </>
    )
  }

  if (screen.name === 'gameOver') {
    return (
      <main className="panel">
        <h1>Game over</h1>
        <p>{screen.summary}</p>
        <ul className="players results">
          {screen.players.map((player, i) => (
            <li key={i} className={player.you ? 'you' : undefined}>
              <span>{player.nickname}</span>
              <span>{player.score}</span>
            </li>
          ))}
        </ul>
        <button type="button" onClick={backToMenu}>
          Back to menu
        </button>
      </main>
    )
  }

  // The lobby takes the form's place on the home page's scroll; the rest of the page stays as it is.
  if (screen.name === 'lobby') {
    return (
      <Home>
        <h2>Room {screen.code}</h2>
        <p>Share this code so others can join.</p>
        <ul className="players">
          {screen.players.map((player, i) => (
            <li key={i}>
              {player}
              {i === 0 && <span className="tag">host</span>}
            </li>
          ))}
        </ul>
        {screen.host ? (
          <>
            <Menu label="Mode" value={mode} choices={MODES} disabled={starting} onChange={pickMode} />
            <Menu label="Questions" value={topic} choices={TOPICS[mode]} disabled={starting} onChange={setTopic} />
            <button type="button" disabled={starting} onClick={start}>
              Start
            </button>
          </>
        ) : (
          <p>Waiting for the host to start…</p>
        )}
      </Home>
    )
  }

  return (
    <Home>
      {screen.name === 'home' && screen.error && <p className="error">{screen.error}</p>}
      <label>
        Nickname
        <input value={nickname} maxLength={MAX_NICKNAME_LENGTH} onChange={(event) => setNickname(event.target.value)} />
      </label>
      <button type="button" disabled={!nickname.trim() || connecting} onClick={createRoom}>
        Create room
      </button>
      <div className="join">
        <input
          aria-label="Room code"
          placeholder="Room code"
          value={code}
          onChange={(event) => setCode(event.target.value)}
        />
        <button type="button" disabled={!nickname.trim() || !code.trim() || connecting} onClick={joinRoom}>
          Join room
        </button>
      </div>
    </Home>
  )
}

// A labelled menu, carved like the inputs.
function Menu<T extends string>(props: {
  label: string
  value: T
  choices: Choice<T>[]
  disabled: boolean
  onChange: (value: T) => void
}) {
  return (
    <label>
      {props.label}
      <span className="select">
        <select
          value={props.value}
          disabled={props.disabled}
          onChange={(event) => props.onChange(event.target.value as T)}
        >
          {props.choices.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </span>
    </label>
  )
}

// The home page: the game's name, then archers facing goblins beside a scroll holding the form or the lobby.
function Home({ children }: { children: ReactNode }) {
  return (
    <main className="home">
      <HomeBackdrop />
      <h1 className="title">Arithmancer</h1>
      <div className="home-body">
        <HeroScene />
        <section className="panel">{children}</section>
      </div>
    </main>
  )
}

export default App
