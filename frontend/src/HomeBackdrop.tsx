import { useEffect, useState } from 'react'
import { DETAILS, GRASS_FRAME, GROUND_SHEET, ROCK_IMAGES, TREE_SHEET } from './assets'

const TILE = 64
const TILEMAP_COLUMNS = 9

// Trees, stones and small details, kept to the edges and the bottom so the title, the scene and the form stay clear.
// x and y are shares of the page, so they spread out on any screen; scale is how big each is drawn.
type Decoration = { image: string; x: number; y: number; scale: number; tree?: boolean }

const DECORATIONS: Decoration[] = [
  { image: TREE_SHEET, x: -3, y: 16, scale: 0.8, tree: true },
  { image: TREE_SHEET, x: 5, y: 60, scale: 0.9, tree: true },
  { image: TREE_SHEET, x: 86, y: 4, scale: 0.8, tree: true },
  { image: TREE_SHEET, x: 90, y: 52, scale: 0.9, tree: true },
  { image: TREE_SHEET, x: 22, y: 78, scale: 0.8, tree: true },
  { image: TREE_SHEET, x: 72, y: 80, scale: 0.8, tree: true },
  { image: ROCK_IMAGES[1], x: 3, y: 46, scale: 1.2 },
  { image: ROCK_IMAGES[2], x: 94, y: 40, scale: 1.2 },
  { image: ROCK_IMAGES[0], x: 54, y: 90, scale: 1.2 },
  { image: DETAILS.bush[0].url, x: 12, y: 34, scale: 1 },
  { image: DETAILS.bush[2].url, x: 88, y: 84, scale: 1 },
  { image: DETAILS.bush[1].url, x: 34, y: 92, scale: 1 },
  { image: DETAILS.mushroom[0].url, x: 10, y: 90, scale: 1 },
  { image: DETAILS.mushroom[2].url, x: 95, y: 22, scale: 1 },
  { image: DETAILS.mushroom[1].url, x: 63, y: 82, scale: 1 },
  { image: DETAILS.bone[0].url, x: 16, y: 70, scale: 1 },
  { image: DETAILS.bone[1].url, x: 84, y: 70, scale: 1 },
  { image: DETAILS.pebble[0].url, x: 46, y: 88, scale: 1 },
  { image: DETAILS.pumpkin[0].url, x: 2, y: 84, scale: 1 },
]

// The grass the game is played on is one tile of the tilemap: cut it out so the page can repeat it.
function cutGrassTile(): Promise<string> {
  return new Promise((resolve) => {
    const tilemap = new Image()
    tilemap.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = TILE
      canvas.height = TILE
      const x = (GRASS_FRAME % TILEMAP_COLUMNS) * TILE
      const y = Math.floor(GRASS_FRAME / TILEMAP_COLUMNS) * TILE
      canvas.getContext('2d')?.drawImage(tilemap, x, y, TILE, TILE, 0, 0, TILE, TILE)
      resolve(canvas.toDataURL())
    }
    tilemap.src = GROUND_SHEET
  })
}

// The home page's ground: the game's grass with trees, stones and details on it.
export function HomeBackdrop() {
  const [grass, setGrass] = useState<string>()

  useEffect(() => {
    cutGrassTile().then(setGrass)
  }, [])

  return (
    <div className="backdrop" aria-hidden="true" style={grass ? { backgroundImage: `url(${grass})` } : undefined}>
      {DECORATIONS.map((decoration, i) => {
        const place = { left: `${decoration.x}%`, top: `${decoration.y}%`, scale: String(decoration.scale) }
        return decoration.tree ? (
          // Trees sway out of step with each other.
          <div
            key={i}
            className="tree"
            style={{ ...place, backgroundImage: `url("${decoration.image}")`, animationDelay: `${-i * 0.17}s` }}
          />
        ) : (
          <img key={i} src={decoration.image} alt="" style={place} />
        )
      })}
    </div>
  )
}
