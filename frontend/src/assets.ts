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
import mushroom1 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/01.png'
import mushroom2 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/02.png'
import mushroom3 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/03.png'
import pebble1 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/04.png'
import pebble2 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/05.png'
import bush1 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/07.png'
import bush2 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/08.png'
import bush3 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/09.png'
import bush4 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/10.png'
import bush5 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/11.png'
import pumpkin1 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/12.png'
import pumpkin2 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/13.png'
import bone1 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/14.png'
import bone2 from '../Tiny Swords/Tiny Swords (Update 010)/Deco/15.png'
import smallBar from '../asset/Tiny Swords (Free Pack)/UI Elements/UI Elements/Bars/SmallBar_Base.png'
import type { DetailState } from './connection'

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
// The variants of each detail on the ground, with the box their sprite fills in its 64x64 image, leaving out the
// shadow: left, top, right, bottom.
export const DETAIL_SIZE = 64
export const DETAILS: Record<DetailState['type'], { url: string; box: number[] }[]> = {
  bush: [
    { url: bush1, box: [16, 21, 47, 40] },
    { url: bush2, box: [12, 17, 53, 45] },
    { url: bush3, box: [4, 11, 61, 50] },
    { url: bush4, box: [23, 23, 44, 43] },
    { url: bush5, box: [20, 15, 52, 47] },
  ],
  mushroom: [
    { url: mushroom1, box: [25, 24, 42, 40] },
    { url: mushroom2, box: [21, 20, 47, 44] },
    { url: mushroom3, box: [14, 12, 52, 45] },
  ],
  pebble: [
    { url: pebble1, box: [25, 19, 43, 34] },
    { url: pebble2, box: [19, 18, 46, 38] },
  ],
  pumpkin: [
    { url: pumpkin1, box: [12, 12, 51, 48] },
    { url: pumpkin2, box: [8, 10, 61, 51] },
  ],
  bone: [
    { url: bone1, box: [15, 16, 51, 45] },
    { url: bone2, box: [25, 20, 43, 43] },
  ],
}
export const ARROW = 'arrow'
// Effects, each played once: a goblin blowing up, a dust puff, and a dead player's skull.
export const EXPLOSION = 'explosion'
export const DUST = 'dust'
export const DEAD = 'dead'
// Health bars are the Free Pack's small bar. Its sheet spaces the pieces 64px apart, so they are put together into
// one texture: the 15x19 ends at (49, 22) and (256, 22), with the 64x19 middle at (128, 22) stretched between them.
// The fill goes in the channel inside.
export const HEALTH_BAR = 'healthBar'
const BAR_SHEET = 'barSheet'
export const HEALTH_BAR_SIZE = { width: 80, height: 19 }
export const HEALTH_BAR_CHANNEL = { x: 10, y: 8, width: 60, height: 6 }
export const HEALTH_BAR_FILL = 0xff3e3e

export const archerKey = (color: string, action: 'idle' | 'run' | 'shoot') => `archer-${color}-${action}`
export const goblinKey = (action: 'idle' | 'run') => `goblin-${action}`
export const detailKey = (type: string, variant: number) => `${type}-${variant}`

const UNIT_FRAME = { frameWidth: 192, frameHeight: 192 }

export function preloadAssets(scene: Phaser.Scene) {
  scene.load.spritesheet(GROUND, tilemap, { frameWidth: 64, frameHeight: 64 })
  scene.load.spritesheet(TREE, tree, { frameWidth: 192, frameHeight: 256 })
  scene.load.image(ROCKS[0], rock2)
  scene.load.image(ROCKS[1], rock4)
  scene.load.image(ROCKS[2], boulder)
  for (const [type, variants] of Object.entries(DETAILS)) {
    variants.forEach(({ url }, variant) => scene.load.image(detailKey(type, variant), url))
  }
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
  scene.load.image(BAR_SHEET, smallBar)
}

export function createHealthBar(scene: Phaser.Scene) {
  const sheet = scene.textures.get(BAR_SHEET).getSourceImage() as HTMLImageElement
  const { width, height } = HEALTH_BAR_SIZE
  const end = 15
  const bar = scene.textures.createCanvas(HEALTH_BAR, width, height)!
  const context = bar.getContext()
  context.drawImage(sheet, 49, 22, end, height, 0, 0, end, height)
  context.drawImage(sheet, 128, 22, 64, height, end, 0, width - 2 * end, height)
  context.drawImage(sheet, 256, 22, end, height, width - end, 0, end, height)
  bar.refresh()
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
