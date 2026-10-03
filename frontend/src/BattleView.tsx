import Phaser from 'phaser'
import { useEffect, useRef, useState } from 'react'
import {
  addHealthBar,
  BAR_SCALE,
  createAnimations,
  createHealthBar,
  DUST,
  ENEMY_BODIES,
  enemyKey,
  GRASS_FRAME,
  GROUND,
  HEALTH_BAR_SIZE,
  type HealthBar,
  PLAYER_COLORS,
  type Point,
  placeHealthBar,
  playEffect,
  preloadAssets,
  textStyle,
  UNIT_SCALE,
  WARRIOR_FEET,
  warriorKey,
} from './assets'
import type { BattleState, Connection, EnemyType, FoeState } from './connection'

// The battle is drawn on a fixed stage that the camera zooms to fit: the warriors in a column on the left, the goblin
// on the right. Characters stand above the middle, leaving the bottom to the problem.
const STAGE = { width: 640, height: 400 }
const WARRIOR_X = 200
const GOBLIN_X = 440
const MIDDLE_Y = 170
const ROW_SPACING = 56
const GOBLIN_SCALE = 0.8
// From the warrior's feet to the top of its head.
const WARRIOR_HEIGHT = 40
// How far in front of the goblin striking warriors stop, and how far apart they stand.
const REACH = 44
const STRIKE_SPACING = 12
// A strike is a run to the goblin, a swing whose blade lands LAND_MS in, and a run back.
const RUN_MS = 250
const LAND_MS = 170
const SWING_MS = 333
// A new goblin runs in from past the stage's right edge.
const ENTER_MS = 500
// Damage numbers float up and fade.
const POP_MS = 700
const POP_RISE = 28
const GOBLIN_DEPTH = 1
const WARRIOR_DEPTH = 2
const TEXT_DEPTH = 3

const OPTION_KEYS = ['1', '2', '3', '4']

type Character = {
  body: Phaser.GameObjects.Sprite
  healthBar: HealthBar
  // The health the bar shows. It follows the server, but waits until holdUntil, when a blow lands.
  shownHealth: number
  holdUntil: number
}

type Warrior = Character & { label: Phaser.GameObjects.Text; home: Point }

type Goblin = Character & { id: number; type: EnemyType; gone: boolean }

const colorOf = (player: number) => PLAYER_COLORS[player % PLAYER_COLORS.length]

class BattleScene extends Phaser.Scene {
  latest?: BattleState
  // States with strikes, played once the scene is running.
  pendingStrikes: BattleState[] = []
  private warriors: Warrior[] = []
  private goblin?: Goblin

  preload() {
    preloadAssets(this)
  }

  create() {
    createAnimations(this)
    createHealthBar(this)
    // Grass well past the stage, so it fills screens of any shape.
    this.add
      .tileSprite(-STAGE.width, -STAGE.height, 3 * STAGE.width, 3 * STAGE.height, GROUND, GRASS_FRAME)
      .setOrigin(0)
    this.fitCamera()
    this.scale.on('resize', () => this.fitCamera())
  }

  update() {
    const state = this.latest
    if (!state) {
      return
    }
    state.players.forEach((player, i) => {
      if (!this.warriors[i]) {
        this.warriors[i] = this.addWarrior(player.nickname, player.health, i, state.players.length)
      }
    })
    if (this.goblin?.id !== state.enemy.id) {
      if (this.goblin && !this.goblin.gone) {
        this.removeGoblin(this.goblin)
      }
      this.goblin = this.addGoblin(state.enemy, this.goblin !== undefined)
    }
    for (const strikes of this.pendingStrikes.splice(0)) {
      this.playStrikes(strikes)
    }
    state.players.forEach((player, i) => {
      const warrior = this.warriors[i]
      this.follow(warrior, player.health)
      const barY = warrior.body.y - WARRIOR_HEIGHT - HEALTH_BAR_SIZE.height * BAR_SCALE
      placeHealthBar(warrior.healthBar, warrior.body.x, barY)
      warrior.healthBar.fill.setScale(Phaser.Math.Clamp(warrior.shownHealth / player.maxHealth, 0, 1), 1)
      warrior.label.setPosition(warrior.body.x, barY - 8)
    })
    const goblin = this.goblin
    if (!goblin.gone) {
      this.follow(goblin, state.enemy.health)
      placeHealthBar(goblin.healthBar, goblin.body.x, this.headOf(goblin).y - HEALTH_BAR_SIZE.height * BAR_SCALE)
      goblin.healthBar.fill.setScale(Phaser.Math.Clamp(goblin.shownHealth / state.enemy.maxHealth, 0, 1), 1)
      if (goblin.shownHealth <= 0) {
        this.removeGoblin(goblin)
      }
    }
  }

  // Each striking warrior runs up to the goblin and swings; once the blades land, the goblin's bar drops and the
  // damage floats up.
  private playStrikes(state: BattleState) {
    const goblin = this.goblin
    if (!goblin || goblin.gone || goblin.id !== state.enemy.id) {
      return
    }
    const damage = goblin.shownHealth - state.enemy.health
    goblin.holdUntil = this.time.now + RUN_MS + LAND_MS
    state.strikes.forEach((player, order) => this.strike(player, order, state.strikes.length))
    this.time.delayedCall(RUN_MS + LAND_MS, () => this.pop(this.headOf(goblin), `-${damage}`))
  }

  private strike(player: number, order: number, count: number) {
    const warrior = this.warriors[player]
    const color = colorOf(player)
    const target = { x: GOBLIN_X - REACH, y: MIDDLE_Y + (order - (count - 1) / 2) * STRIKE_SPACING }
    warrior.body.setFlipX(false).play(warriorKey(color, 'run'))
    this.tweens.add({
      targets: warrior.body,
      ...target,
      duration: RUN_MS,
      onComplete: () => {
        warrior.body.play(warriorKey(color, player % 2 === 0 ? 'attack1' : 'attack2'))
        this.time.delayedCall(SWING_MS, () => {
          warrior.body.setFlipX(true).play(warriorKey(color, 'run'))
          this.tweens.add({
            targets: warrior.body,
            ...warrior.home,
            duration: RUN_MS,
            onComplete: () => warrior.body.setFlipX(false).play(warriorKey(color, 'idle')),
          })
        })
      },
    })
  }

  // The bar follows the server's health, unless a blow has yet to land.
  private follow(character: Character, health: number) {
    if (this.time.now >= character.holdUntil) {
      character.shownHealth = health
    }
  }

  private pop(at: Point, text: string) {
    const label = this.add.text(at.x, at.y, text, textStyle(20, 4)).setOrigin(0.5).setDepth(TEXT_DEPTH)
    this.tweens.add({
      targets: label,
      y: at.y - POP_RISE,
      alpha: 0,
      duration: POP_MS,
      onComplete: () => label.destroy(),
    })
  }

  private headOf(goblin: Goblin): Point {
    return {
      x: goblin.body.x,
      y: goblin.body.y - (ENEMY_BODIES[goblin.type].height * GOBLIN_SCALE) / UNIT_SCALE,
    }
  }

  private addWarrior(nickname: string, health: number, index: number, count: number): Warrior {
    const home = { x: WARRIOR_X, y: MIDDLE_Y + (index - (count - 1) / 2) * ROW_SPACING }
    return {
      body: this.add
        .sprite(home.x, home.y, warriorKey(colorOf(index), 'idle'))
        .setOrigin(WARRIOR_FEET.x, WARRIOR_FEET.y)
        .setScale(UNIT_SCALE)
        .setDepth(WARRIOR_DEPTH)
        .play(warriorKey(colorOf(index), 'idle')),
      label: this.add.text(home.x, home.y, nickname, textStyle(14, 3)).setOrigin(0.5).setDepth(WARRIOR_DEPTH),
      healthBar: addHealthBar(this, WARRIOR_DEPTH),
      shownHealth: health,
      holdUntil: 0,
      home,
    }
  }

  // The first goblin is already there; the next ones run in from the right.
  private addGoblin(enemy: FoeState, enter: boolean): Goblin {
    const { feet } = ENEMY_BODIES[enemy.type]
    const body = this.add
      .sprite(enter ? STAGE.width + feet.x * 192 : GOBLIN_X, MIDDLE_Y, enemy.type)
      .setOrigin(feet.x, feet.y)
      .setScale(GOBLIN_SCALE)
      .setFlipX(true)
      .setDepth(GOBLIN_DEPTH)
      .play(enemyKey(enemy.type, enter ? 'run' : 'idle'))
    if (enter) {
      this.tweens.add({
        targets: body,
        x: GOBLIN_X,
        duration: ENTER_MS,
        onComplete: () => body.play(enemyKey(enemy.type, 'idle')),
      })
    }
    return {
      id: enemy.id,
      type: enemy.type,
      body,
      healthBar: addHealthBar(this, GOBLIN_DEPTH),
      shownHealth: enemy.health,
      holdUntil: 0,
      gone: false,
    }
  }

  // A beaten goblin goes in a puff of dust.
  private removeGoblin(goblin: Goblin) {
    const center = { x: goblin.body.x, y: (goblin.body.y + this.headOf(goblin).y) / 2 }
    playEffect(this, DUST, center, 1.5, TEXT_DEPTH)
    goblin.body.destroy()
    goblin.healthBar.frame.destroy()
    goblin.healthBar.fill.destroy()
    goblin.gone = true
  }

  private fitCamera() {
    const { width, height } = this.scale
    this.cameras.main
      .setZoom(Math.min(width / STAGE.width, height / STAGE.height))
      .centerOn(STAGE.width / 2, STAGE.height / 2)
  }
}

export function BattleView({ connection }: { connection: Connection }) {
  const parent = useRef<HTMLDivElement>(null)
  const [state, setState] = useState<BattleState>()
  // The problem you last answered: the options wait for the next one, so a second press cannot land on it.
  const [answered, setAnswered] = useState<number>()

  useEffect(() => {
    const scene = new BattleScene('battle')
    const game = new Phaser.Game({
      type: Phaser.AUTO,
      parent: parent.current!,
      backgroundColor: '#16171d',
      scale: { mode: Phaser.Scale.RESIZE },
      scene,
    })
    const stopState = connection.on('battleState', (state) => {
      scene.latest = state
      if (state.strikes.length > 0) {
        scene.pendingStrikes.push(state)
      }
      setState(state)
    })
    return () => {
      stopState()
      game.destroy(true)
    }
  }, [connection])

  const you = state?.players.find((player) => player.you)
  const canAnswer =
    state?.phase === 'players' && state.locked <= 0 && state.problem.id !== answered && (you?.health ?? 0) > 0

  function pick(index: number) {
    if (!state || !canAnswer) {
      return
    }
    connection.send('answer', { value: state.problem.options[index] })
    setAnswered(state.problem.id)
  }

  // Keys 1 to 4 pick an option, through the latest pick so they see the latest state. Auto-repeat is ignored.
  const pickRef = useRef(pick)
  useEffect(() => {
    pickRef.current = pick
  })
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const index = OPTION_KEYS.indexOf(event.key)
      if (index >= 0 && !event.repeat) {
        pickRef.current(index)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const charge = state?.players.reduce((total, player) => total + player.charge, 0) ?? 0

  return (
    <>
      <div ref={parent} className="game" />
      {state && (
        <>
          <div className="hud">
            <p className="time">
              Turn {state.turn} · {state.phase === 'players' ? `${Math.ceil(state.secondsLeft)}s` : "Goblin's turn"}
            </p>
            <ul>
              {state.players.map((player, i) => (
                <li key={i} className={player.you ? 'you' : undefined}>
                  <span>{player.nickname}</span>
                  <span>{player.score}</span>
                </li>
              ))}
            </ul>
          </div>
          <section className="problem">
            {state.phase === 'players' ? (
              <>
                <p>
                  {state.locked > 0 ? `Wrong! Thinking… ${Math.ceil(state.locked)}s` : `Damage this turn: ${charge}`}
                </p>
                <p className="problem-text">{state.problem.text}</p>
                <div className="options">
                  {state.problem.options.map((option, i) => (
                    <button key={i} type="button" disabled={!canAnswer} onClick={() => pick(i)}>
                      <kbd>{OPTION_KEYS[i]}</kbd> {option}
                    </button>
                  ))}
                </div>
              </>
            ) : (
              <p className="problem-text">The goblin's turn…</p>
            )}
          </section>
        </>
      )}
    </>
  )
}
