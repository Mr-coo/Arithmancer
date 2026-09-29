import type Phaser from 'phaser'
import tilemap from '../asset/Tiny Swords (Free Pack)/Terrain/Tileset/Tilemap_color1.png'
import tree from '../asset/Tiny Swords (Free Pack)/Terrain/Resources/Wood/Trees/Tree1.png'
import rock2 from '../asset/Tiny Swords (Free Pack)/Terrain/Decorations/Rocks/Rock2.png'
import rock4 from '../asset/Tiny Swords (Free Pack)/Terrain/Decorations/Rocks/Rock4.png'
import boulder from '../Tiny Swords/Tiny Swords (Update 010)/Deco/06.png'
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
import tntGoblin from '../Tiny Swords/Tiny Swords (Update 010)/Factions/Goblins/Troops/TNT/Red/TNT_Red.png'
import explosion from '../Tiny Swords/Tiny Swords (Update 010)/Effects/Explosion/Explosions.png'
import dead from '../Tiny Swords/Tiny Swords (Update 010)/Factions/Knights/Troops/Dead/Dead.png'
import dust from '../asset/Tiny Swords (Free Pack)/Particle FX/Dust_02.png'

// Players are archers, one color each (a room has up to 4 players).
export const PLAYER_COLORS = ['blue', 'red', 'yellow', 'purple'] as const

const ARCHERS = {
  blue: { idle: blueIdle, run: blueRun, shoot: blueShoot },
  red: { idle: redIdle, run: redRun, shoot: redShoot },
  yellow: { idle: yellowIdle, run: yellowRun, shoot: yellowShoot },
  purple: { idle: purpleIdle, run: purpleRun, shoot: purpleShoot },
}

// Enemies are TNT goblins. Their sheet has one animation per row, 7 frames wide: idle (6 frames), run (6),
// then throw (7, unused).
export const GOBLIN = 'goblin'
const GOBLIN_FRAMES = { idle: { start: 0, end: 5 }, run: { start: 7, end: 12 } }

export const GROUND = 'ground'
// The center of the tilemap's grass patch, which repeats seamlessly.
export const GRASS_FRAME = 10
export const TREE = 'tree'
export const ROCKS = ['rock2', 'rock4', 'boulder']
export const ARROW = 'arrow'
// Effects, each played once: a goblin blowing up, a dust puff, and a dead player's skull.
export const EXPLOSION = 'explosion'
export const DUST = 'dust'
export const DEAD = 'dead'

export const archerKey = (color: string, action: 'idle' | 'run' | 'shoot') => `archer-${color}-${action}`
export const goblinKey = (action: 'idle' | 'run') => `goblin-${action}`

const UNIT_FRAME = { frameWidth: 192, frameHeight: 192 }

export function preloadAssets(scene: Phaser.Scene) {
  scene.load.spritesheet(GROUND, tilemap, { frameWidth: 64, frameHeight: 64 })
  scene.load.spritesheet(TREE, tree, { frameWidth: 192, frameHeight: 256 })
  scene.load.image(ROCKS[0], rock2)
  scene.load.image(ROCKS[1], rock4)
  scene.load.image(ROCKS[2], boulder)
  scene.load.image(ARROW, arrow)
  for (const color of PLAYER_COLORS) {
    for (const [action, url] of Object.entries(ARCHERS[color])) {
      scene.load.spritesheet(archerKey(color, action as 'idle' | 'run' | 'shoot'), url, UNIT_FRAME)
    }
  }
  scene.load.spritesheet(GOBLIN, tntGoblin, UNIT_FRAME)
  scene.load.spritesheet(EXPLOSION, explosion, UNIT_FRAME)
  scene.load.spritesheet(DUST, dust, { frameWidth: 64, frameHeight: 64 })
  scene.load.spritesheet(DEAD, dead, { frameWidth: 128, frameHeight: 128 })
}

// Archer and tree animations use the same key as their sprite sheet; the goblin's are rows of one sheet.
export function createAnimations(scene: Phaser.Scene) {
  const loop = (key: string, frameRate: number, repeat = -1) =>
    scene.anims.create({ key, frames: scene.anims.generateFrameNumbers(key), frameRate, repeat })
  for (const color of PLAYER_COLORS) {
    loop(archerKey(color, 'idle'), 8)
    loop(archerKey(color, 'run'), 10)
    loop(archerKey(color, 'shoot'), 24, 0)
  }
  for (const [action, frames] of Object.entries(GOBLIN_FRAMES)) {
    scene.anims.create({
      key: goblinKey(action as 'idle' | 'run'),
      frames: scene.anims.generateFrameNumbers(GOBLIN, frames),
      frameRate: 10,
      repeat: -1,
    })
  }
  loop(TREE, 6)
  loop(EXPLOSION, 18, 0)
  loop(DUST, 20, 0)
  // The skull pops up and rests; the sheet's later frames, where it sinks away, are left out.
  scene.anims.create({ key: DEAD, frames: scene.anims.generateFrameNumbers(DEAD, { start: 0, end: 6 }), frameRate: 10 })
}
