import { type ReactNode, useState } from 'react'
import { BattleView } from './BattleView'
import { connect, type Connection, type DecorationState, type DetailState, type FinalScore } from './connection'
import { formatTime } from './format'
import { GameView } from './GameView'
import { HeroScene } from './HeroScene'
import { HomeBackdrop } from './HomeBackdrop'
import './App.css'

type Screen =
  | { name: 'home'; error?: string }
  | { name: 'lobby'; code: string; players: string[]; host: boolean }
  | { name: 'game'; decorations: DecorationState[]; details: DetailState[] }
  | { name: 'battle' }
  | { name: 'gameOver'; summary: string; players: FinalScore[] }

// "1 goblin", "4 goblins".
const count = (n: number, noun: string) => `${n} ${noun}${n === 1 ? '' : 's'}`

function App() {
  const [screen, setScreen] = useState<Screen>({ name: 'home' })
  const [connection, setConnection] = useState<Connection>()
  const [nickname, setNickname] = useState('')
  const [code, setCode] = useState('')
  const [connecting, setConnecting] = useState(false)

  async function open() {
    setConnecting(true)
    const opened = await connect((reason) => {
      setConnection(undefined)
      setConnecting(false)
      setScreen({ name: 'home', error: reason || 'Disconnected from the server' })
    })
    opened.on('roomCreated', ({ code, players }) => setScreen({ name: 'lobby', code, players, host: true }))
    opened.on('roomJoined', ({ code, players }) =>
      setScreen((current) => ({ name: 'lobby', code, players, host: current.name === 'lobby' && current.host })),
    )
    opened.on('gameStarted', ({ decorations, details }) => setScreen({ name: 'game', decorations, details }))
    opened.on('gameOver', ({ time, players }) =>
      setScreen({ name: 'gameOver', summary: `You survived ${formatTime(time)}.`, players }),
    )
    opened.on('battleStarted', () => setScreen({ name: 'battle' }))
    opened.on('battleOver', ({ turns, beaten, players }) => {
      const summary = `You beat ${count(beaten, 'goblin')} in ${count(turns, 'turn')}.`
      setScreen({ name: 'gameOver', summary, players })
    })
    setConnection(opened)
    setConnecting(false)
    return opened
  }

  function backToMenu() {
    connection?.close()
    setConnection(undefined)
    setScreen({ name: 'home' })
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
        <p className="hint">Move with WASD. Press the last digit of an enemy's answer to shoot it.</p>
      </>
    )
  }

  if (screen.name === 'battle' && connection) {
    return (
      <>
        <BattleView connection={connection} />
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
            <button type="button" onClick={() => connection?.send('startGame', { mode: 'realTime' })}>
              Start real-time
            </button>
            <button type="button" onClick={() => connection?.send('startGame', { mode: 'turnBased' })}>
              Start turn-based
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
        <input value={nickname} onChange={(event) => setNickname(event.target.value)} />
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
