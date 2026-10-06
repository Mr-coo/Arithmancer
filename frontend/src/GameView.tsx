import Phaser from 'phaser'
import { useEffect, useRef, useState } from 'react'
import {
  ARROW,
  addHealthBar,
  archerKey,
  BAR_SCALE,
  createAnimations,
  createHealthBar,
  DEAD,
  DETAIL_SCALE,
  DETAIL_SIZE,
  DETAILS,
  detailKey,
  DUST,
  ENEMY_BODIES,
  EXPLOSION,
  enemyKey,
  GRASS_FRAME,
  GROUND,
  HEALTH_BAR_CHANNEL,
  HEALTH_BAR_FILL,
  HEALTH_BAR_SIZE,
  type HealthBar,
  ITEM_ICONS,
  itemKey,
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
} from './assets'
import type {
  Connection,
  DecorationState,
  DetailState,
  EnemyType,
  GameState,
  PlayerState,
  ShotState,
} from './connection'
import { formatTime } from './format'

// Physical key positions, so WASD also works on other keyboard layouts. The arrow keys move the same way.
const MOVE_KEYS: Record<string, string> = {
  KeyW: 'w',
  KeyA: 'a',
  KeyS: 's',
  KeyD: 'd',
  ArrowUp: 'w',
  ArrowLeft: 'a',
  ArrowDown: 's',
  ArrowRight: 'd',
}

const PLAYER_RADIUS = 16
// A downed player's bar fills green as a teammate revives them.
const REVIVE_FILL = 0x4ade80
const COOLDOWN_BAR_HEIGHT = 2
const PROJECTILE_MS = 250
// The archer's feet in its 192x192 frame.
const ARCHER_FEET = { x: 95 / 192, y: 128 / 192 }
// From the feet to the top of the head, and to the chest where arrows leave and land.
const UNIT_HEIGHT = 40
const AIM_HEIGHT = 20
// Width and vertical center of each rock in ROCKS, so it can be scaled to its solid circle.
const ROCK_SIZES = [
  { width: 46, centerY: 33 / 64 },
  { width: 54, centerY: 33 / 64 },
  { width: 39, centerY: 30 / 64 },
]
const ARROW_SCALE = 0.6
// 8 frames at 24 per second.
const SHOOT_MS = 333
const EXPLOSION_SCALE = 0.7
// Items lie on the ground with a gold glow that brightens and dims, bobbing up and down, so they catch the eye.
const ITEM_SCALE = 0.65
const ITEM_BOB = 4
const ITEM_GLOW_COLOR = 0xfff1a8
// How far the glow reaches, and its strength as it dims and brightens.
const ITEM_GLOW = { scale: 1.6, dim: 2, bright: 7 }
// Draw order, bottom to top: characters behind a decoration or detail, decorations and details, enemies, then the
// other players, so an enemy on top of a player does not hide their name or health. Enemies' questions always stay
// above decorations, so they can be read.
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
// The server's logical view around each player: the camera zooms to fit it, so you see what you can hit.
const VIEW = { width: 960, height: 540 }

type Sprite = {
  body: Phaser.GameObjects.Sprite
  label: Phaser.GameObjects.Text
  healthBar: Phaser.GameObjects.Image
  healthFill: Phaser.GameObjects.Rectangle
  cooldownFill: Phaser.GameObjects.Rectangle
}

type EnemySprite = {
  type: EnemyType
  body: Phaser.GameObjects.Sprite
  question: Phaser.GameObjects.Text
  // Enemies that take more than one answer show how many are left.
  healthBar?: HealthBar
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
  latest: GameState = { time: 0, round: 0, roundStartsIn: 0, players: [], enemies: [], items: [], shots: [] }
  pendingShots: ShotState[] = []
  decorations: DecorationState[] = []
  details: DetailState[] = []
  // Where a character's feet are behind each detail.
  private detailCovers: Phaser.Geom.Rectangle[] = []
  private decorationSprites: DecorationSprite[] = []
  private sprites: Sprite[] = []
  // When each player's shoot animation ends, by player index.
  private shootingUntil: number[] = []
  private enemySprites = new Map<number, EnemySprite>()
  private itemSprites = new Map<number, { image: Phaser.GameObjects.Image; glow?: Phaser.Filters.Glow }>()
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
    createHealthBar(this)
    this.ground = this.add
      .tileSprite(0, 0, this.scale.width, this.scale.height, GROUND, GRASS_FRAME)
      .setOrigin(0)
      .setScrollFactor(0)
    this.fitCamera()
    this.scale.on('resize', (size: Phaser.Structs.Size) => {
      this.ground?.setSize(size.width, size.height)
      this.fitCamera()
    })
    // Characters walk over details, but one whose feet are on a detail, above its bottom edge, is behind it. The box
    // is widened by the character's radius.
    this.detailCovers = this.details.map((detail, i) => {
      const variants = DETAILS[detail.type]
      const variant = i % variants.length
      this.add
        .image(detail.x, detail.y, detailKey(detail.type, variant))
        .setScale(DETAIL_SCALE)
        .setDepth(DECORATION_DEPTH)
      const [left, top, right, bottom] = variants[variant].box.map((edge) => (edge - DETAIL_SIZE / 2) * DETAIL_SCALE)
      return new Phaser.Geom.Rectangle(
        detail.x + left - PLAYER_RADIUS,
        detail.y + top,
        right - left + 2 * PLAYER_RADIUS,
        bottom - top,
      )
    })
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
      const barY = sprite.body.y - UNIT_HEIGHT - HEALTH_BAR_SIZE.height * BAR_SCALE
      placeHealthBar({ frame: sprite.healthBar, fill: sprite.healthFill }, sprite.body.x, barY)
      const downed = player.health <= 0
      sprite.healthFill
        .setFillStyle(downed ? REVIVE_FILL : HEALTH_BAR_FILL)
        .setScale(Phaser.Math.Clamp(downed ? player.revive : player.health / player.maxHealth, 0, 1), 1)
      // Under the bar, shrinks as the shot cooldown runs out, hidden once the player can shoot.
      sprite.cooldownFill
        .setVisible(player.cooldown > 0)
        .setPosition(sprite.healthFill.x, barY + HEALTH_BAR_SIZE.height * BAR_SCALE)
        .setScale(Phaser.Math.Clamp(player.cooldown / player.maxCooldown, 0, 1), 1)
      sprite.label.setPosition(sprite.body.x, barY - 8)
      const covering = this.decorations.filter((decoration) => isBehind(sprite.body, decoration))
      const depth = covering.length || this.isBehindDetail(sprite.body) ? BEHIND_DEPTH : PLAYER_DEPTH
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
    this.updateItems()
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
        const { feet } = ENEMY_BODIES[enemy.type]
        sprite = {
          type: enemy.type,
          body: this.add.sprite(enemy.x, enemy.y, enemy.type).setOrigin(feet.x, feet.y).setScale(UNIT_SCALE),
          question: this.add.text(enemy.x, enemy.y, '', textStyle(16, 4)).setOrigin(0.5).setDepth(ENEMY_DEPTH),
          healthBar: enemy.maxHealth > 1 ? addHealthBar(this, ENEMY_DEPTH) : undefined,
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
      sprite.healthBar?.fill.setScale(Phaser.Math.Clamp(enemy.health / enemy.maxHealth, 0, 1), 1)
    }
    for (const [id, sprite] of this.enemySprites) {
      if (!ids.has(id)) {
        // Remove enemies the server no longer sends, once any arrow at them has arrived: in a puff of dust if an
        // arrow killed them, otherwise they reached a player and blew up.
        if (!this.targeted.has(id)) {
          const center = { x: sprite.body.x, y: sprite.body.y - ENEMY_BODIES[sprite.type].height / 2 }
          if (this.shotDown.delete(id)) {
            playEffect(this, DUST, center, 1, EFFECT_DEPTH)
          } else {
            playEffect(this, EXPLOSION, center, EXPLOSION_SCALE, EFFECT_DEPTH)
          }
          sprite.body.destroy()
          sprite.question.destroy()
          sprite.healthBar?.frame.destroy()
          sprite.healthBar?.fill.destroy()
          this.enemySprites.delete(id)
          continue
        }
        // Until then, keep it moving the way it was going.
        sprite.target.x += (sprite.velocity.x * delta) / 1000
        sprite.target.y += (sprite.velocity.y * delta) / 1000
      }
      sprite.body.x += (sprite.target.x - sprite.body.x) * SMOOTHING
      sprite.body.y += (sprite.target.y - sprite.body.y) * SMOOTHING
      const behind =
        this.decorations.some((decoration) => isBehind(sprite.body, decoration)) || this.isBehindDetail(sprite.body)
      sprite.body.setDepth(behind ? BEHIND_DEPTH : ENEMY_DEPTH)
      if (Math.abs(sprite.velocity.x) > MOVING) {
        sprite.body.setFlipX(sprite.velocity.x < 0)
      }
      const action = Math.hypot(sprite.velocity.x, sprite.velocity.y) > MOVING ? 'run' : 'idle'
      sprite.body.play(enemyKey(sprite.type, action), true)
      // The question sits above the head, and above the health bar if it has one.
      let top = sprite.body.y - ENEMY_BODIES[sprite.type].height
      if (sprite.healthBar) {
        top -= HEALTH_BAR_SIZE.height * BAR_SCALE
        placeHealthBar(sprite.healthBar, sprite.body.x, top)
      }
      sprite.question.setPosition(sprite.body.x, top - 12)
    }
  }

  // Items appear where the server drops them, and vanish in a puff once picked up or gone.
  private updateItems() {
    const ids = new Set<number>()
    for (const item of this.latest.items) {
      ids.add(item.id)
      if (!this.itemSprites.has(item.id)) {
        const image = this.add
          .image(item.x, item.y, itemKey(item.type))
          .setScale(ITEM_SCALE)
          .setDepth(DECORATION_DEPTH)
          .enableFilters()
        // Filters need WebGL; with the canvas renderer the item just has no glow.
        const glow = image.filters?.internal.addGlow(ITEM_GLOW_COLOR, ITEM_GLOW.bright, 0, ITEM_GLOW.scale)
        // Pad the image so the glow is not cut off at its edges.
        glow?.setPaddingOverride(null)
        this.tweens.add({
          targets: image,
          y: item.y - ITEM_BOB,
          duration: 600,
          yoyo: true,
          repeat: -1,
          ease: 'Sine.easeInOut',
        })
        if (glow) {
          this.tweens.add({
            targets: glow,
            outerStrength: ITEM_GLOW.dim,
            duration: 800,
            yoyo: true,
            repeat: -1,
            ease: 'Sine.easeInOut',
          })
        }
        this.itemSprites.set(item.id, { image, glow })
      }
    }
    for (const [id, { image, glow }] of this.itemSprites) {
      if (!ids.has(id)) {
        playEffect(this, DUST, image, 0.6, EFFECT_DEPTH)
        this.tweens.killTweensOf([image, ...(glow ? [glow] : [])])
        image.destroy()
        this.itemSprites.delete(id)
      }
    }
  }

  // Never zoomed out, so the screen-sized ground still covers the screen.
  private fitCamera() {
    const { width, height } = this.scale
    this.cameras.main.setZoom(Math.max(1, Math.min(width / VIEW.width, height / VIEW.height)))
  }

  private isBehindDetail(feet: Point) {
    return this.detailCovers.some((cover) => cover.contains(feet.x, feet.y))
  }

  private addSprite(player: PlayerState, index: number): Sprite {
    const healthBar = addHealthBar(this, PLAYER_DEPTH)
    const sprite = {
      body: this.add
        .sprite(player.x, player.y, archerKey(colorOf(index), 'idle'))
        .setOrigin(ARCHER_FEET.x, ARCHER_FEET.y)
        .setScale(UNIT_SCALE),
      label: this.add.text(player.x, player.y, player.nickname, textStyle(14, 3)).setOrigin(0.5),
      healthBar: healthBar.frame,
      healthFill: healthBar.fill,
      cooldownFill: this.add
        .rectangle(0, 0, HEALTH_BAR_CHANNEL.width * BAR_SCALE, COOLDOWN_BAR_HEIGHT, 0xfacc15)
        .setOrigin(0),
    }
    Object.values(sprite).forEach((part) => part.setDepth(PLAYER_DEPTH))
    this.sprites.push(sprite)
    if (player.you) {
      this.cameras.main.startFollow(sprite.body)
    }
    return sprite
  }
}

export function GameView({
  connection,
  decorations,
  details,
}: {
  connection: Connection
  decorations: DecorationState[]
  details: DetailState[]
}) {
  const parent = useRef<HTMLDivElement>(null)
  const [hud, setHud] = useState<GameState>()

  useEffect(() => {
    const scene = new GameScene('game')
    scene.decorations = decorations
    scene.details = details
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

    // The physical keys held. A direction is down while any of its keys is, such as W and the up arrow together.
    const held = new Set<string>()
    const isHeld = (key: string) => [...held].some((code) => MOVE_KEYS[code] === key)
    const onKeyDown = (event: KeyboardEvent) => {
      // Answers are one digit, so pressing it shoots. Ignore auto-repeat while the key is held.
      if (/^[0-9]$/.test(event.key)) {
        if (!event.repeat) {
          connection.send('answer', { value: Number(event.key) })
        }
        return
      }
      const key = MOVE_KEYS[event.code]
      if (key) {
        // The arrow keys would otherwise scroll the page.
        event.preventDefault()
        if (!isHeld(key)) {
          connection.send('input', { key, action: 'down' })
        }
        held.add(event.code)
      }
    }
    const onKeyUp = (event: KeyboardEvent) => {
      const key = MOVE_KEYS[event.code]
      if (key && held.delete(event.code) && !isHeld(key)) {
        connection.send('input', { key, action: 'up' })
      }
    }
    // Keyup never arrives if the window loses focus while a key is held, so release everything.
    const onBlur = () => {
      const keys = new Set([...held].map((code) => MOVE_KEYS[code]))
      keys.forEach((key) => connection.send('input', { key, action: 'up' }))
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
  }, [connection, decorations, details])

  return (
    <>
      <div ref={parent} className="game" />
      {hud && (
        <div className="hud">
          <p className="time">
            {hud.roundStartsIn > 0
              ? `Round ${hud.round} in ${Math.ceil(hud.roundStartsIn)}s`
              : `Round ${hud.round} · ${formatTime(hud.time)}`}
          </p>
          <ul>
            {hud.players.map((player, i) => (
              <li key={i} className={player.you ? 'you' : undefined}>
                <span>{player.nickname}</span>
                <span>{player.score}</span>
              </li>
            ))}
            <Boosts player={hud.players.find((player) => player.you)} />
          </ul>
        </div>
      )}
    </>
  )
}

// Your item boosts and the seconds left of each.
function Boosts({ player }: { player?: PlayerState }) {
  const boosts = [
    { type: 'haste' as const, label: 'Faster shots', seconds: player?.haste ?? 0 },
    { type: 'speed' as const, label: 'Faster moves', seconds: player?.speedBoost ?? 0 },
  ].filter((boost) => boost.seconds > 0)
  if (boosts.length === 0) {
    return null
  }
  return (
    <li className="boosts">
      {boosts.map((boost) => (
        <span key={boost.type} title={boost.label}>
          <img src={ITEM_ICONS[boost.type]} alt={boost.label} />
          {Math.ceil(boost.seconds)}s
        </span>
      ))}
    </li>
  )
}
