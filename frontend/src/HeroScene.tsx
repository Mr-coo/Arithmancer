import { type CSSProperties, useEffect, useRef } from 'react'
import { ARCHERS, ENEMIES } from './assets'

// A character on the home page's stage: a 192x192 frame sheet played from one row, standing at (x, y) on the stage and
// z toward the viewer. Goblins are flipped to face the archers, with a question over their heads like in the game.
type Character = {
  sheet: string
  row?: number
  frames: number
  seconds: number
  x: number
  y: number
  z: number
  flip?: boolean
  question?: string
}

const CAST: Character[] = [
  { sheet: ARCHERS.red.idle, frames: 6, seconds: 0.8, x: 30, y: 40, z: -120 },
  { sheet: ARCHERS.blue.shoot, frames: 8, seconds: 1, x: 80, y: 190, z: 60 },
  {
    sheet: ENEMIES.torch.sheet,
    frames: ENEMIES.torch.idle,
    seconds: 0.9,
    x: 340,
    y: 30,
    z: -140,
    flip: true,
    question: 'log₂ 32',
  },
  { sheet: ENEMIES.goblin.sheet, row: 1, frames: 6, seconds: 0.6, x: 310, y: 180, z: 40, flip: true, question: '7 × 8' },
]

// Archers facing goblins on a stage tilted in 3D, which tilts a little further as the mouse moves.
export function HeroScene() {
  const scene = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const element = scene.current
    if (!element || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      return
    }
    // From -1 to 1 across the window, set straight on the element so moving the mouse does not re-render.
    const onMove = (event: PointerEvent) => {
      element.style.setProperty('--tilt-x', String((event.clientX / window.innerWidth) * 2 - 1))
      element.style.setProperty('--tilt-y', String((event.clientY / window.innerHeight) * 2 - 1))
    }
    window.addEventListener('pointermove', onMove)
    return () => window.removeEventListener('pointermove', onMove)
  }, [])

  return (
    <div ref={scene} className="scene" aria-hidden="true">
      <div className="stage">
        {CAST.map((character, i) => (
          <div
            key={i}
            className="character"
            style={{ '--x': `${character.x}px`, '--y': `${character.y}px`, '--z': `${character.z}px` } as CSSProperties}
          >
            <div
              className={character.flip ? 'sprite flip' : 'sprite'}
              style={
                {
                  '--sheet': `url("${character.sheet}")`,
                  '--row': character.row ?? 0,
                  '--frames': character.frames,
                  '--seconds': `${character.seconds}s`,
                } as CSSProperties
              }
            />
            {character.question && <p className="question">{character.question}</p>}
          </div>
        ))}
      </div>
    </div>
  )
}
