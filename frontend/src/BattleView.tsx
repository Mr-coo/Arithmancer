import Phaser from 'phaser'
import { useEffect, useRef, useState } from 'react'
import {
  addHealthBar,
  BAR_SCALE,
  createAnimations,
  createHealthBar,
  DEAD,
  DETAIL_SCALE,
  DETAILS,
  detailKey,
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
  ROCKS,
  SKULL_FEET,
  SKULL_SCALE,
  TREE,
  TREE_BASE,
  TREE_SCALE,
  textStyle,
  UNIT_SCALE,
  WARRIOR_FEET,
  warriorKey,
} from './assets'
import type { BattleState, Connection, DetailState, EnemyType, FoeState } from './connection'

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
// A strike is a run to the goblin, a swing whose blade lands LAND_MS in, and a run back. It takes 2.6 seconds, before
// the goblin acts.
const RUN_MS = 1000
const LAND_MS = 300
const SWING_MS = 600
// A new goblin runs in from past the stage's right edge.
const ENTER_MS = 1500
// The goblin strikes back by running up to the warriors, GOBLIN_REACH in front of them, and attacking. Its blow lands
// IMPACT_MS into its attack: each warrior hit flashes, and raises its shield for the 6 frames of its guard animation.
// Then the goblin runs back. It takes 2.6 seconds.
const GOBLIN_REACH = 60
const GOBLIN_RUN_MS = 900
const ATTACK_MS = 800
const IMPACT_MS = 450
const FLASH_MS = 250
const GUARD_MS = 750
const HIT_TINT = 0xff8080
const HIT_COLOR = '#ffb3b3'
// Damage numbers float up and fade.
const POP_MS = 1000
const POP_RISE = 28
// Decorations are scattered over SCATTER, around the stage, but none touch the arena, where the characters stand and
// move: the warriors' column, the lane where they fight, and the lane the goblin comes in by from the right. Each kind
// takes up a box around where it stands, a tree mostly above it.
const SCATTER = { left: -320, top: -200, right: 960, bottom: 600 }
const ARENA = [
  { left: 150, top: 15, right: 250, bottom: 280 },
  { left: 150, top: 95, right: 520, bottom: 235 },
  { left: 380, top: 85, right: 780, bottom: 205 },
]
const TREE_BOX = { left: -60, top: -180, right: 60, bottom: 10 }
const SMALL_BOX = { left: -26, top: -26, right: 26, bottom: 26 }
const TREE_COUNT = 14
const ROCK_COUNT = 10
const DETAIL_COUNT = 90
const TREE_SPACING = 100
const DECORATION_SPACING = 40
const ROCK_SCALE = 0.8
// Types listed more than once are more common.
const DETAIL_TYPES: DetailState['type'][] = [
  'bush',
  'bush',
  'bush',
  'mushroom',
  'mushroom',
  'pebble',
  'pumpkin',
  'pumpkin',
  'bone',
]
// The ground, then decorations ordered by how low they stand, then the characters.
const GROUND_DEPTH = -1
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

type Box = { left: number; top: number; right: number; bottom: number }

// Random numbers from 0 to 1 that are the same for the same seed: an FNV-1a hash of it, fed to mulberry32.
function seededRandom(seed: string) {
  let state = [...seed].reduce((hash, char) => Math.imul(hash ^ char.charCodeAt(0), 16777619), 2166136261)
  return () => {
    state = (state + 0x6d2b79f5) | 0
    let t = Math.imul(state ^ (state >>> 15), 1 | state)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

class BattleScene extends Phaser.Scene {
  // The room's code, which lays out the decorations.
  code = ''
  latest?: BattleState
  // States with strikes or hits, played once the scene is running.
  pending: BattleState[] = []
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
      .tileSprite(-2 * STAGE.width, -2 * STAGE.height, 5 * STAGE.width, 5 * STAGE.height, GROUND, GRASS_FRAME)
      .setOrigin(0)
      .setDepth(GROUND_DEPTH)
    this.scatterDecorations()
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
    for (const event of this.pending.splice(0)) {
      if (event.strikes.length > 0) {
        this.playStrikes(event)
      }
      if (event.hits.length > 0) {
        this.playHits(event)
      }
    }
    state.players.forEach((player, i) => {
      const warrior = this.warriors[i]
      this.follow(warrior, player.health)
      // A downed warrior is a skull.
      const downed = warrior.shownHealth <= 0
      const isSkull = warrior.body.anims.currentAnim?.key === DEAD
      if (downed && !isSkull) {
        warrior.body.setOrigin(SKULL_FEET.x, SKULL_FEET.y).setScale(SKULL_SCALE).play(DEAD)
      } else if (!downed && isSkull) {
        warrior.body.setOrigin(WARRIOR_FEET.x, WARRIOR_FEET.y).setScale(UNIT_SCALE).play(warriorKey(colorOf(i), 'idle'))
      }
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

  // The goblin runs up to the warriors and attacks. As its blow lands, each warrior hit raises its shield, flashes, and
  // its bar drops. Then the goblin runs back to its spot.
  private playHits(state: BattleState) {
    const goblin = this.goblin
    if (!goblin || goblin.gone) {
      return
    }
    const run = enemyKey(goblin.type, 'run')
    goblin.body.setFlipX(true).play(run)
    this.tweens.add({
      targets: goblin.body,
      x: WARRIOR_X + GOBLIN_REACH,
      duration: GOBLIN_RUN_MS,
      onComplete: () => {
        goblin.body.play(enemyKey(goblin.type, 'attack'))
        this.time.delayedCall(ATTACK_MS, () => {
          goblin.body.setFlipX(false).play(run)
          this.tweens.add({
            targets: goblin.body,
            x: GOBLIN_X,
            duration: GOBLIN_RUN_MS,
            onComplete: () => goblin.body.setFlipX(true).play(enemyKey(goblin.type, 'idle')),
          })
        })
      },
    })
    const landsIn = GOBLIN_RUN_MS + IMPACT_MS
    for (const player of state.hits) {
      const warrior = this.warriors[player]
      const damage = warrior.shownHealth - state.players[player].health
      warrior.holdUntil = this.time.now + landsIn
      this.time.delayedCall(landsIn, () => {
        warrior.body.play(warriorKey(colorOf(player), 'guard')).setTint(HIT_TINT)
        this.time.delayedCall(FLASH_MS, () => warrior.body.clearTint())
        this.time.delayedCall(GUARD_MS, () => {
          if (warrior.shownHealth > 0) {
            warrior.body.play(warriorKey(colorOf(player), 'idle'))
          }
        })
        this.pop({ x: warrior.body.x, y: warrior.body.y - WARRIOR_HEIGHT }, `-${damage}`, HIT_COLOR)
      })
    }
  }

  // The bar follows the server's health, unless a blow has yet to land.
  private follow(character: Character, health: number) {
    if (this.time.now >= character.holdUntil) {
      character.shownHealth = health
    }
  }

  private pop(at: Point, text: string, color = '#ffffff') {
    const label = this.add
      .text(at.x, at.y, text, { ...textStyle(20, 4), color })
      .setOrigin(0.5)
      .setDepth(TEXT_DEPTH)
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

  // Trees, rocks, bushes, mushrooms, pebbles, pumpkins and bones around the arena, laid out from the room code so
  // everyone in the room sees the same ones.
  private scatterDecorations() {
    const random = seededRandom(this.code)
    const pick = <T,>(items: readonly T[]) => items[Math.floor(random() * items.length)]
    const placed: { at: Point; spacing: number }[] = []
    const scatter = (count: number, box: Box, spacing: number, add: (at: Point) => Phaser.GameObjects.Image) => {
      for (let attempt = 0, added = 0; attempt < 50 * count && added < count; attempt++) {
        const at = {
          x: SCATTER.left + random() * (SCATTER.right - SCATTER.left),
          y: SCATTER.top + random() * (SCATTER.bottom - SCATTER.top),
        }
        const inArena = ARENA.some(
          (area) =>
            at.x + box.right > area.left &&
            at.x + box.left < area.right &&
            at.y + box.bottom > area.top &&
            at.y + box.top < area.bottom,
        )
        const crowded = placed.some(
          (other) => Phaser.Math.Distance.BetweenPoints(other.at, at) < Math.max(other.spacing, spacing),
        )
        if (!inArena && !crowded) {
          placed.push({ at, spacing })
          // Under the characters; lower ones in front of higher ones.
          add(at).setDepth(at.y / 10000)
          added++
        }
      }
    }
    scatter(TREE_COUNT, TREE_BOX, TREE_SPACING, (at) =>
      this.add
        .sprite(at.x, at.y, TREE)
        .setOrigin(TREE_BASE.x, TREE_BASE.y)
        .setScale(TREE_SCALE)
        .play({ key: TREE, startFrame: Math.floor(random() * 8) }),
    )
    scatter(ROCK_COUNT, SMALL_BOX, DECORATION_SPACING, (at) =>
      this.add.image(at.x, at.y, pick(ROCKS)).setScale(ROCK_SCALE),
    )
    scatter(DETAIL_COUNT, SMALL_BOX, DECORATION_SPACING, (at) => {
      const type = pick(DETAIL_TYPES)
      const variant = Math.floor(random() * DETAILS[type].length)
      return this.add.image(at.x, at.y, detailKey(type, variant)).setScale(DETAIL_SCALE)
    })
  }

  private fitCamera() {
    const { width, height } = this.scale
    this.cameras.main
      .setZoom(Math.min(width / STAGE.width, height / STAGE.height))
      .centerOn(STAGE.width / 2, STAGE.height / 2)
  }
}

export function BattleView({ connection, code }: { connection: Connection; code: string }) {
  const parent = useRef<HTMLDivElement>(null)
  const [state, setState] = useState<BattleState>()
  // The problem you last answered: the options wait for the next one, so a second press cannot land on it.
  const [answered, setAnswered] = useState<number>()

  useEffect(() => {
    const scene = new BattleScene('battle')
    scene.code = code
    const game = new Phaser.Game({
      type: Phaser.AUTO,
      parent: parent.current!,
      backgroundColor: '#16171d',
      scale: { mode: Phaser.Scale.RESIZE },
      scene,
    })
    const stopState = connection.on('battleState', (state) => {
      scene.latest = state
      // Phaser stops while the tab is hidden, so strikes and hits then are skipped rather than all played on coming
      // back; the bars jump to the latest health.
      if (!document.hidden && (state.strikes.length > 0 || state.hits.length > 0)) {
        scene.pending.push(state)
      }
      setState(state)
    })
    return () => {
      stopState()
      game.destroy(true)
    }
  }, [connection, code])

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
            <p className="turn">
              Turn {state.turn}
              {state.phase === 'players' && ` · ${Math.ceil(state.secondsLeft)}s`}
            </p>
            {(you?.health ?? 0) <= 0 ? (
              <p className="problem-text">You're down.</p>
            ) : state.phase === 'players' ? (
              <>
                <p className={state.locked > 0 ? 'status wrong' : 'status'}>
                  {state.locked > 0 ? `Wrong! Thinking… ${Math.ceil(state.locked)}s` : `Damage this turn: ${charge}`}
                </p>
                <p className="problem-text">{state.problem.text}</p>
                <div className="options">
                  {state.problem.options.map((option, i) => (
                    <button key={i} type="button" disabled={!canAnswer} onClick={() => pick(i)}>
                      <kbd>{OPTION_KEYS[i]}</kbd>
                      {option}
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
