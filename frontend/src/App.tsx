import { useState } from 'react'
import { connect, type Connection } from './connection'
import { GameView } from './GameView'
import './App.css'

type Screen =
  | { name: 'home'; error?: string }
  | { name: 'lobby'; code: string; players: string[]; host: boolean }
  | { name: 'game' }

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
    opened.on('gameStarted', () => setScreen({ name: 'game' }))
    setConnection(opened)
    setConnecting(false)
    return opened
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
        <GameView connection={connection} />
        <p className="hint">Move with WASD. Press an enemy's answer (0-9) to shoot it.</p>
      </>
    )
  }

  if (screen.name === 'lobby') {
    return (
      <main className="panel">
        <h1>Room {screen.code}</h1>
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
          <button type="button" onClick={() => connection?.send('startGame', {})}>
            Start game
          </button>
        ) : (
          <p>Waiting for the host to start…</p>
        )}
      </main>
    )
  }

  return (
    <main className="panel">
      <h1>Arithmancer</h1>
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
    </main>
  )
}

export default App
