import Phaser from 'phaser'
import { useEffect, useRef, useState } from 'react'
import {
  ARROW,
  archerKey,
  createAnimations,
  DEAD,
  DUST,
  EXPLOSION,
  GOBLIN,
  goblinKey,
  GRASS_FRAME,
  GROUND,
  PLAYER_COLORS,
  preloadAssets,
  ROCKS,
  TREE,
} from './assets'
import type { Connection, DecorationState, GameState, PlayerState, ShotState } from './connection'
import { formatTime } from './format'

// Physical key positions, so WASD also works on other keyboard layouts.
const MOVE_KEYS: Record<string, string> = { KeyW: 'w', KeyA: 'a', KeyS: 's', KeyD: 'd' }

const PLAYER_RADIUS = 16
const HEALTH_BAR_WIDTH = 32
const HEALTH_BAR_HEIGHT = 4
const COOLDOWN_BAR_HEIGHT = 2
const PROJECTILE_MS = 250
// Sprite sizes and anchors, measured from the Tiny Swords sheets. Characters stand with their feet on their position.
const UNIT_SCALE = 0.5
const ARCHER_FEET = { x: 95 / 192, y: 128 / 192 }
const GOBLIN_FEET = { x: 95 / 192, y: 127 / 192 }
// From the feet to the top of the head, and to the chest where arrows leave and land.
const UNIT_HEIGHT = 40
const GOBLIN_HEIGHT = 30
const AIM_HEIGHT = 20
const TREE_SCALE = 0.74
// The middle of the trunk, which sits on the tree's solid circle.
const TREE_BASE = { x: 98 / 192, y: 224 / 256 }
// Width and vertical center of each rock in ROCKS, so it can be scaled to its solid circle.
const ROCK_SIZES = [
  { width: 46, centerY: 33 / 64 },
  { width: 54, centerY: 33 / 64 },
  { width: 39, centerY: 30 / 64 },
]
const ARROW_SCALE = 0.6
// Enemies are ghosts that walk through trees and stones, so they are drawn slightly see-through.
const GHOST_ALPHA = 0.75
// 8 frames at 24 per second.
const SHOOT_MS = 333
// A dead player is a skull, standing where its shadow rests in the 128x128 frame.
const SKULL_FEET = { x: 67 / 128, y: 94 / 128 }
const SKULL_SCALE = 0.6
const EXPLOSION_SCALE = 0.7
// Draw order, bottom to top: players behind a decoration, decorations, enemies (ghosts float over decorations),
// then the other players, so an enemy on top of a player does not hide their name or health.
const BEHIND_DEPTH = 1
const DECORATION_DEPTH = 2
const ENEMY_DEPTH = 3
const PLAYER_DEPTH = 4
const EFFECT_DEPTH = 5
// A decoration you are behind turns see-through, so you can still see yourself.
const BEHIND_ALPHA = 0.5
// The server sends the state 20 times per second.
const TICKS_PER_SECOND = 20
// Share of the remaining distance covered each frame, to smooth those updates.
const SMOOTHING = 0.3
// Below this many pixels of movement per frame, a character plays its idle animation.
const MOVING = 0.5

type Sprite = {
  body: Phaser.GameObjects.Sprite
  label: Phaser.GameObjects.Text
  healthBack: Phaser.GameObjects.Rectangle
  healthFill: Phaser.GameObjects.Rectangle
  cooldownFill: Phaser.GameObjects.Rectangle
}

type Point = { x: number; y: number }

type EnemySprite = {
  body: Phaser.GameObjects.Sprite
  question: Phaser.GameObjects.Text
  // Latest server position, and how fast it moved (units per second).
  target: Point
  velocity: Point
}

type DecorationSprite = { decoration: DecorationState; object: Phaser.GameObjects.Image }

// A character touching a decoration's cover circle is behind it.
function isBehind(point: Point, decoration: DecorationState) {
  return (
    Math.hypot(point.x - decoration.coverX, point.y - decoration.coverY) < decoration.coverRadius + PLAYER_RADIUS
  )
}

const colorOf = (player: number) => PLAYER_COLORS[player % PLAYER_COLORS.length]

class GameScene extends Phaser.Scene {
  latest: GameState = { time: 0, players: [], enemies: [], shots: [] }
  pendingShots: ShotState[] = []
  decorations: DecorationState[] = []
  private decorationSprites: DecorationSprite[] = []
  private sprites: Sprite[] = []
  // When each player's shoot animation ends, by player index.
  private shootingUntil: number[] = []
  private enemySprites = new Map<number, EnemySprite>()
  // Enemies with a projectile on the way, kept on screen until it arrives.
  private targeted = new Set<number>()
  // Enemies killed by an arrow. Any other enemy the server drops reached a player and blew up.
  private shotDown = new Set<number>()
  private ground?: Phaser.GameObjects.TileSprite

  preload() {
    preloadAssets(this)
  }

  create() {
    createAnimations(this)
    this.ground = this.add
      .tileSprite(0, 0, this.scale.width, this.scale.height, GROUND, GRASS_FRAME)
      .setOrigin(0)
      .setScrollFactor(0)
    this.scale.on('resize', (size: Phaser.Structs.Size) => this.ground?.setSize(size.width, size.height))
    // Trees stand with the middle of their trunk on the solid circle; rocks are scaled to fill it.
    this.decorationSprites = this.decorations.map((decoration, i) => {
      const object =
        decoration.type === 'tree'
          ? this.add
              .sprite(decoration.x, decoration.y, TREE)
              .setOrigin(TREE_BASE.x, TREE_BASE.y)
              .setScale(TREE_SCALE)
              .play({ key: TREE, startFrame: i % 8 })
          : this.add
              .image(decoration.x, decoration.y, ROCKS[i % ROCKS.length])
              .setOrigin(0.5, ROCK_SIZES[i % ROCKS.length].centerY)
              .setScale((2 * decoration.radius) / ROCK_SIZES[i % ROCKS.length].width)
      object.setDepth(DECORATION_DEPTH)
      return { decoration, object }
    })
  }

  update(time: number, delta: number) {
    let coveringYou = new Set<DecorationState>()
    this.latest.players.forEach((player, i) => {
      const sprite = this.sprites[i] ?? this.addSprite(player, i)
      const dx = player.x - sprite.body.x
      const dy = player.y - sprite.body.y
      sprite.body.x += dx * SMOOTHING
      sprite.body.y += dy * SMOOTHING
      const isSkull = sprite.body.anims.currentAnim?.key === DEAD
      if (player.health <= 0) {
        if (!isSkull) {
          sprite.body.setOrigin(SKULL_FEET.x, SKULL_FEET.y).setScale(SKULL_SCALE).setFlipX(false).play(DEAD)
        }
      } else {
        if (isSkull) {
          sprite.body.setOrigin(ARCHER_FEET.x, ARCHER_FEET.y).setScale(UNIT_SCALE)
        }
        if (Math.abs(dx) > MOVING) {
          sprite.body.setFlipX(dx < 0)
        }
        if (time >= (this.shootingUntil[i] ?? 0)) {
          sprite.body.play(archerKey(colorOf(i), Math.hypot(dx, dy) > MOVING ? 'run' : 'idle'), true)
        }
      }
      const barX = sprite.body.x - HEALTH_BAR_WIDTH / 2
      const barY = sprite.body.y - UNIT_HEIGHT - 6
      sprite.healthBack.setPosition(barX, barY)
      sprite.healthFill
        .setPosition(barX, barY)
        .setScale(Phaser.Math.Clamp(player.health / player.maxHealth, 0, 1), 1)
      // Shrinks as the shot cooldown runs out, hidden once the player can shoot.
      sprite.cooldownFill
        .setVisible(player.cooldown > 0)
        .setPosition(barX, barY + HEALTH_BAR_HEIGHT / 2 + COOLDOWN_BAR_HEIGHT)
        .setScale(Phaser.Math.Clamp(player.cooldown / player.maxCooldown, 0, 1), 1)
      sprite.label.setPosition(sprite.body.x, barY - 14)
      const covering = this.decorations.filter((decoration) => isBehind(sprite.body, decoration))
      const depth = covering.length ? BEHIND_DEPTH : PLAYER_DEPTH
      if (sprite.body.depth !== depth) {
        Object.values(sprite).forEach((part) => part.setDepth(depth))
      }
      if (player.you) {
        coveringYou = new Set(covering)
      }
    })
    for (const { decoration, object } of this.decorationSprites) {
      object.setAlpha(coveringYou.has(decoration) ? BEHIND_ALPHA : 1)
    }
    this.launchShots()
    this.updateEnemies(delta)
    // Keep the screen-sized ground lined up with the world as the camera moves.
    const camera = this.cameras.main
    this.ground?.setTilePosition(camera.scrollX, camera.scrollY)
  }

  // Each hit is an arrow from the archer to the enemy, fading and shrinking until it disappears.
  private launchShots() {
    for (const shot of this.pendingShots.splice(0)) {
      this.shotDown.add(shot.enemy)
      const archer = this.sprites[shot.player]?.body
      const enemy = this.enemySprites.get(shot.enemy)
      if (!archer || !enemy) {
        continue
      }
      this.targeted.add(shot.enemy)
      this.shootingUntil[shot.player] = this.time.now + SHOOT_MS
      archer.setFlipX(enemy.body.x < archer.x).play(archerKey(colorOf(shot.player), 'shoot'))
      const start = { x: archer.x, y: archer.y - AIM_HEIGHT }
      const arrow = this.add.image(start.x, start.y, ARROW).setScale(ARROW_SCALE).setDepth(PLAYER_DEPTH)
      this.tweens.addCounter({
        from: 0,
        to: 1,
        duration: PROJECTILE_MS,
        // Aim at where the enemy is now, so the arrow lands on it while it keeps moving.
        onUpdate: (tween) => {
          const progress = tween.getValue() ?? 1
          const target = { x: enemy.body.x, y: enemy.body.y - AIM_HEIGHT }
          arrow
            .setPosition(start.x + (target.x - start.x) * progress, start.y + (target.y - start.y) * progress)
            .setRotation(Math.atan2(target.y - start.y, target.x - start.x))
            .setAlpha(1 - progress)
            .setScale(ARROW_SCALE * (1 - 0.5 * progress))
        },
        onComplete: () => {
          arrow.destroy()
          this.targeted.delete(shot.enemy)
          // An enemy that survived the hit can still blow up later.
          if (this.latest.enemies.some((e) => e.id === shot.enemy)) {
            this.shotDown.delete(shot.enemy)
          }
        },
      })
    }
  }

  private updateEnemies(delta: number) {
    const ids = new Set<number>()
    for (const enemy of this.latest.enemies) {
      ids.add(enemy.id)
      let sprite = this.enemySprites.get(enemy.id)
      if (!sprite) {
        sprite = {
          body: this.add
            .sprite(enemy.x, enemy.y, GOBLIN)
            .setOrigin(GOBLIN_FEET.x, GOBLIN_FEET.y)
            .setScale(UNIT_SCALE)
            .setAlpha(GHOST_ALPHA)
            .setDepth(ENEMY_DEPTH),
          question: this.add
            .text(enemy.x, enemy.y, '', { fontFamily: 'system-ui', fontSize: '16px', fontStyle: 'bold' })
            .setOrigin(0.5)
            .setDepth(ENEMY_DEPTH),
          target: { x: enemy.x, y: enemy.y },
          velocity: { x: 0, y: 0 },
        }
        this.enemySprites.set(enemy.id, sprite)
      }
      if (enemy.x !== sprite.target.x || enemy.y !== sprite.target.y) {
        sprite.velocity = {
          x: (enemy.x - sprite.target.x) * TICKS_PER_SECOND,
          y: (enemy.y - sprite.target.y) * TICKS_PER_SECOND,
        }
        sprite.target = { x: enemy.x, y: enemy.y }
      }
      sprite.question.setText(enemy.question)
    }
    for (const [id, sprite] of this.enemySprites) {
      if (!ids.has(id)) {
        // Remove enemies the server no longer sends, once any arrow at them has arrived: in a puff of dust if an
        // arrow killed them, otherwise they reached a player and blew up.
        if (!this.targeted.has(id)) {
          const center = { x: sprite.body.x, y: sprite.body.y - GOBLIN_HEIGHT / 2 }
          if (this.shotDown.delete(id)) {
            this.playEffect(DUST, center, 1)
          } else {
            this.playEffect(EXPLOSION, center, EXPLOSION_SCALE)
          }
          sprite.body.destroy()
          sprite.question.destroy()
          this.enemySprites.delete(id)
          continue
        }
        // Until then, keep it moving the way it was going.
        sprite.target.x += (sprite.velocity.x * delta) / 1000
        sprite.target.y += (sprite.velocity.y * delta) / 1000
      }
      sprite.body.x += (sprite.target.x - sprite.body.x) * SMOOTHING
      sprite.body.y += (sprite.target.y - sprite.body.y) * SMOOTHING
      if (Math.abs(sprite.velocity.x) > MOVING) {
        sprite.body.setFlipX(sprite.velocity.x < 0)
      }
      sprite.body.play(goblinKey(Math.hypot(sprite.velocity.x, sprite.velocity.y) > MOVING ? 'run' : 'idle'), true)
      sprite.question.setPosition(sprite.body.x, sprite.body.y - GOBLIN_HEIGHT - 12)
    }
  }

  private playEffect(key: string, at: Point, scale: number) {
    const effect = this.add.sprite(at.x, at.y, key).setScale(scale).setDepth(EFFECT_DEPTH).play(key)
    effect.once(Phaser.Animations.Events.ANIMATION_COMPLETE, () => effect.destroy())
  }

  private addSprite(player: PlayerState, index: number): Sprite {
    const sprite = {
      body: this.add
        .sprite(player.x, player.y, archerKey(colorOf(index), 'idle'))
        .setOrigin(ARCHER_FEET.x, ARCHER_FEET.y)
        .setScale(UNIT_SCALE),
      label: this.add
        .text(player.x, player.y, player.nickname, { fontFamily: 'system-ui', fontSize: '14px' })
        .setOrigin(0.5),
      healthBack: this.add.rectangle(0, 0, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT, 0x7f1d1d).setOrigin(0, 0.5),
      healthFill: this.add.rectangle(0, 0, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT, 0x4ade80).setOrigin(0, 0.5),
      cooldownFill: this.add.rectangle(0, 0, HEALTH_BAR_WIDTH, COOLDOWN_BAR_HEIGHT, 0xfacc15).setOrigin(0, 0.5),
    }
    Object.values(sprite).forEach((part) => part.setDepth(PLAYER_DEPTH))
    this.sprites.push(sprite)
    if (player.you) {
      this.cameras.main.startFollow(sprite.body)
    }
    return sprite
  }
}

export function GameView({ connection, decorations }: { connection: Connection; decorations: DecorationState[] }) {
  const parent = useRef<HTMLDivElement>(null)
  const [hud, setHud] = useState<GameState>()

  useEffect(() => {
    const scene = new GameScene('game')
    scene.decorations = decorations
    const game = new Phaser.Game({
      type: Phaser.AUTO,
      parent: parent.current!,
      backgroundColor: '#16171d',
      scale: { mode: Phaser.Scale.RESIZE },
      scene,
    })
    const stopState = connection.on('state', (state) => {
      scene.latest = state
      setHud(state)
      scene.pendingShots.push(...state.shots)
    })

    const held = new Set<string>()
    const onKeyDown = (event: KeyboardEvent) => {
      // Answers are one digit, so pressing it shoots. Ignore auto-repeat while the key is held.
      if (/^[0-9]$/.test(event.key)) {
        if (!event.repeat) {
          connection.send('answer', { value: Number(event.key) })
        }
        return
      }
      const key = MOVE_KEYS[event.code]
      if (key && !held.has(key)) {
        held.add(key)
        connection.send('input', { key, action: 'down' })
      }
    }
    const onKeyUp = (event: KeyboardEvent) => {
      const key = MOVE_KEYS[event.code]
      if (key && held.delete(key)) {
        connection.send('input', { key, action: 'up' })
      }
    }
    // Keyup never arrives if the window loses focus while a key is held, so release everything.
    const onBlur = () => {
      held.forEach((key) => connection.send('input', { key, action: 'up' }))
      held.clear()
    }
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)
    window.addEventListener('blur', onBlur)

    return () => {
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('keyup', onKeyUp)
      window.removeEventListener('blur', onBlur)
      stopState()
      game.destroy(true)
    }
  }, [connection, decorations])

  return (
    <>
      <div ref={parent} className="game" />
      {hud && (
        <div className="hud">
          <p className="time">{formatTime(hud.time)}</p>
          <ul>
            {hud.players.map((player, i) => (
              <li key={i} className={player.you ? 'you' : undefined}>
                <span>{player.nickname}</span>
                <span>{player.score}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  )
}
