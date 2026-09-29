import Phaser from 'phaser'
import { useEffect, useRef } from 'react'
import type { Connection, GameState, PlayerState, ShotState } from './connection'

// Physical key positions, so WASD also works on other keyboard layouts.
const MOVE_KEYS: Record<string, string> = { KeyW: 'w', KeyA: 'a', KeyS: 's', KeyD: 'd' }

const GRID_SIZE = 64
const PLAYER_RADIUS = 16
const HEALTH_BAR_WIDTH = 32
const HEALTH_BAR_HEIGHT = 4
const COOLDOWN_BAR_HEIGHT = 2
const ENEMY_RADIUS = 14
const PROJECTILE_RADIUS = 6
const PROJECTILE_MS = 250
// Players draw above enemies, so an enemy on top of a player does not hide their name or health.
const PLAYER_DEPTH = 1
// The server sends the state 20 times per second.
const TICKS_PER_SECOND = 20
// Share of the remaining distance covered each frame, to smooth those updates.
const SMOOTHING = 0.3

type Sprite = {
  body: Phaser.GameObjects.Arc
  label: Phaser.GameObjects.Text
  healthBack: Phaser.GameObjects.Rectangle
  healthFill: Phaser.GameObjects.Rectangle
  cooldownFill: Phaser.GameObjects.Rectangle
}

type Point = { x: number; y: number }

type EnemySprite = {
  body: Phaser.GameObjects.Arc
  question: Phaser.GameObjects.Text
  // Latest server position, and how fast it moved (units per second).
  target: Point
  velocity: Point
}

class GameScene extends Phaser.Scene {
  latest: GameState = { players: [], enemies: [], shots: [] }
  pendingShots: ShotState[] = []
  private sprites: Sprite[] = []
  private enemySprites = new Map<number, EnemySprite>()
  // Enemies with a projectile on the way, kept on screen until it arrives.
  private targeted = new Set<number>()
  private grid?: Phaser.GameObjects.TileSprite

  create() {
    const lines = this.make.graphics({}, false)
    lines.lineStyle(1, 0xffffff, 0.08).strokeRect(0, 0, GRID_SIZE, GRID_SIZE)
    lines.generateTexture('grid', GRID_SIZE, GRID_SIZE)
    lines.destroy()
    this.grid = this.add
      .tileSprite(0, 0, this.scale.width, this.scale.height, 'grid')
      .setOrigin(0)
      .setScrollFactor(0)
    this.scale.on('resize', (size: Phaser.Structs.Size) => this.grid?.setSize(size.width, size.height))
  }

  update(_time: number, delta: number) {
    this.latest.players.forEach((player, i) => {
      const sprite = this.sprites[i] ?? this.addSprite(player)
      sprite.body.x += (player.x - sprite.body.x) * SMOOTHING
      sprite.body.y += (player.y - sprite.body.y) * SMOOTHING
      const barX = sprite.body.x - HEALTH_BAR_WIDTH / 2
      const barY = sprite.body.y - PLAYER_RADIUS - 8
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
    })
    this.launchShots()
    this.updateEnemies(delta)
    // Keep the screen-sized grid lined up with the world as the camera moves.
    const camera = this.cameras.main
    this.grid?.setTilePosition(camera.scrollX, camera.scrollY)
  }

  // Each hit flies from the player to the enemy, fading and shrinking until it disappears.
  private launchShots() {
    for (const shot of this.pendingShots.splice(0)) {
      const from = this.sprites[shot.player]?.body
      const enemy = this.enemySprites.get(shot.enemy)
      if (!from || !enemy) {
        continue
      }
      this.targeted.add(shot.enemy)
      const start = { x: from.x, y: from.y }
      const projectile = this.add.circle(start.x, start.y, PROJECTILE_RADIUS, 0xfacc15).setDepth(PLAYER_DEPTH)
      this.tweens.addCounter({
        from: 0,
        to: 1,
        duration: PROJECTILE_MS,
        // Aim at where the enemy is now, so the projectile lands on it while it keeps moving.
        onUpdate: (tween) => {
          const progress = tween.getValue() ?? 1
          projectile
            .setPosition(
              start.x + (enemy.body.x - start.x) * progress,
              start.y + (enemy.body.y - start.y) * progress,
            )
            .setAlpha(1 - progress)
            .setScale(1 - 0.7 * progress)
        },
        onComplete: () => {
          projectile.destroy()
          this.targeted.delete(shot.enemy)
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
          body: this.add.circle(enemy.x, enemy.y, ENEMY_RADIUS, 0xef4444),
          question: this.add
            .text(enemy.x, enemy.y, '', { fontFamily: 'system-ui', fontSize: '16px', fontStyle: 'bold' })
            .setOrigin(0.5),
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
        // Remove enemies the server no longer sends, once any projectile at them has arrived.
        if (!this.targeted.has(id)) {
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
      sprite.question.setPosition(sprite.body.x, sprite.body.y - ENEMY_RADIUS - 12)
    }
  }

  private addSprite(player: PlayerState): Sprite {
    const sprite = {
      body: this.add.circle(player.x, player.y, PLAYER_RADIUS, player.you ? 0x7c5cff : 0x3ec9a7),
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

export function GameView({ connection }: { connection: Connection }) {
  const parent = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const scene = new GameScene('game')
    const game = new Phaser.Game({
      type: Phaser.AUTO,
      parent: parent.current!,
      backgroundColor: '#16171d',
      scale: { mode: Phaser.Scale.RESIZE },
      scene,
    })
    const stopState = connection.on('state', (state) => {
      scene.latest = state
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
  }, [connection])

  return <div ref={parent} className="game" />
}
