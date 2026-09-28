import Phaser from 'phaser'
import { useEffect, useRef } from 'react'
import type { Connection, PlayerState } from './connection'

// Physical key positions, so WASD also works on other keyboard layouts.
const MOVE_KEYS: Record<string, string> = { KeyW: 'w', KeyA: 'a', KeyS: 's', KeyD: 'd' }

const GRID_SIZE = 64
const PLAYER_RADIUS = 16
// Share of the remaining distance covered each frame, to smooth the 20 updates per second.
const SMOOTHING = 0.3

type Sprite = { body: Phaser.GameObjects.Arc; label: Phaser.GameObjects.Text }

class GameScene extends Phaser.Scene {
  latest: PlayerState[] = []
  private sprites: Sprite[] = []
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

  update() {
    this.latest.forEach((player, i) => {
      const sprite = this.sprites[i] ?? this.addSprite(player)
      sprite.body.x += (player.x - sprite.body.x) * SMOOTHING
      sprite.body.y += (player.y - sprite.body.y) * SMOOTHING
      sprite.label.setPosition(sprite.body.x, sprite.body.y - PLAYER_RADIUS - 12)
    })
    // Keep the screen-sized grid lined up with the world as the camera moves.
    const camera = this.cameras.main
    this.grid?.setTilePosition(camera.scrollX, camera.scrollY)
  }

  private addSprite(player: PlayerState): Sprite {
    const sprite = {
      body: this.add.circle(player.x, player.y, PLAYER_RADIUS, player.you ? 0x7c5cff : 0x3ec9a7),
      label: this.add
        .text(player.x, player.y, player.nickname, { fontFamily: 'system-ui', fontSize: '14px' })
        .setOrigin(0.5),
    }
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
    const stopState = connection.on('state', ({ players }) => {
      scene.latest = players
    })

    const held = new Set<string>()
    const onKeyDown = (event: KeyboardEvent) => {
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
