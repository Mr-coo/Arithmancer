import type Phaser from 'phaser'
import tilemap from '../asset/Tiny Swords (Free Pack)/Terrain/Tileset/Tilemap_color1.png'
import tree from '../asset/Tiny Swords (Free Pack)/Terrain/Resources/Wood/Trees/Tree1.png'
import rock2 from '../asset/Tiny Swords (Free Pack)/Terrain/Decorations/Rocks/Rock2.png'
import rock4 from '../asset/Tiny Swords (Free Pack)/Terrain/Decorations/Rocks/Rock4.png'
import arrow from '../asset/Tiny Swords (Free Pack)/Units/Blue Units/Archer/Arrow.png'
import blueIdle from '../asset/Tiny Swords (Free Pack)/Units/Blue Units/Archer/Archer_Idle.png'
import blueRun from '../asset/Tiny Swords (Free Pack)/Units/Blue Units/Archer/Archer_Run.png'
import blueShoot from '../asset/Tiny Swords (Free Pack)/Units/Blue Units/Archer/Archer_Shoot.png'
import redIdle from '../asset/Tiny Swords (Free Pack)/Units/Red Units/Archer/Archer_Idle.png'
import redRun from '../asset/Tiny Swords (Free Pack)/Units/Red Units/Archer/Archer_Run.png'
import redShoot from '../asset/Tiny Swords (Free Pack)/Units/Red Units/Archer/Archer_Shoot.png'
import yellowIdle from '../asset/Tiny Swords (Free Pack)/Units/Yellow Units/Archer/Archer_Idle.png'
import yellowRun from '../asset/Tiny Swords (Free Pack)/Units/Yellow Units/Archer/Archer_Run.png'
import yellowShoot from '../asset/Tiny Swords (Free Pack)/Units/Yellow Units/Archer/Archer_Shoot.png'
import purpleIdle from '../asset/Tiny Swords (Free Pack)/Units/Purple Units/Archer/Archer_Idle.png'
import purpleRun from '../asset/Tiny Swords (Free Pack)/Units/Purple Units/Archer/Archer_Run.png'
import purpleShoot from '../asset/Tiny Swords (Free Pack)/Units/Purple Units/Archer/Archer_Shoot.png'
import ghostIdle from '../asset/Tiny Swords (Free Pack)/Units/Black Units/Warrior/Warrior_Idle.png'
import ghostRun from '../asset/Tiny Swords (Free Pack)/Units/Black Units/Warrior/Warrior_Run.png'

// Players are archers, one color each (a room has up to 4 players).
export const PLAYER_COLORS = ['blue', 'red', 'yellow', 'purple'] as const

const ARCHERS = {
  blue: { idle: blueIdle, run: blueRun, shoot: blueShoot },
  red: { idle: redIdle, run: redRun, shoot: redShoot },
  yellow: { idle: yellowIdle, run: yellowRun, shoot: yellowShoot },
  purple: { idle: purpleIdle, run: purpleRun, shoot: purpleShoot },
}

// Enemies are ghosts: black warriors drawn see-through.
const GHOST = { idle: ghostIdle, run: ghostRun }

export const GROUND = 'ground'
// The center of the tilemap's grass patch, which repeats seamlessly.
export const GRASS_FRAME = 10
export const TREE = 'tree'
export const ROCKS = ['rock2', 'rock4']
export const ARROW = 'arrow'

export const archerKey = (color: string, action: 'idle' | 'run' | 'shoot') => `archer-${color}-${action}`
export const ghostKey = (action: 'idle' | 'run') => `ghost-${action}`

const UNIT_FRAME = { frameWidth: 192, frameHeight: 192 }

export function preloadAssets(scene: Phaser.Scene) {
  scene.load.spritesheet(GROUND, tilemap, { frameWidth: 64, frameHeight: 64 })
  scene.load.spritesheet(TREE, tree, { frameWidth: 192, frameHeight: 256 })
  scene.load.image(ROCKS[0], rock2)
  scene.load.image(ROCKS[1], rock4)
  scene.load.image(ARROW, arrow)
  for (const color of PLAYER_COLORS) {
    for (const [action, url] of Object.entries(ARCHERS[color])) {
      scene.load.spritesheet(archerKey(color, action as 'idle' | 'run' | 'shoot'), url, UNIT_FRAME)
    }
  }
  for (const [action, url] of Object.entries(GHOST)) {
    scene.load.spritesheet(ghostKey(action as 'idle' | 'run'), url, UNIT_FRAME)
  }
}

// Each animation uses the same key as its sprite sheet.
export function createAnimations(scene: Phaser.Scene) {
  const loop = (key: string, frameRate: number, repeat = -1) =>
    scene.anims.create({ key, frames: scene.anims.generateFrameNumbers(key), frameRate, repeat })
  for (const color of PLAYER_COLORS) {
    loop(archerKey(color, 'idle'), 8)
    loop(archerKey(color, 'run'), 10)
    loop(archerKey(color, 'shoot'), 24, 0)
  }
  loop(ghostKey('idle'), 8)
  loop(ghostKey('run'), 10)
  loop(TREE, 6)
}
