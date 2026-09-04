import { useEffect, useMemo, useState } from 'react'

/** Lightweight visualizer that works for both local and YouTube playback.
 * It intentionally uses CSS transforms instead of WebAudio so cross-origin
 * YouTube playback remains compatible and mobile performance stays smooth.
 */
export default function Visualizer({ active = false, compact = false }) {
  const [tick, setTick] = useState(0)

  useEffect(() => {
    if (!active) return
    const id = window.setInterval(() => setTick(v => v + 1), 140)
    return () => window.clearInterval(id)
  }, [active])

  const bars = useMemo(() => Array.from({ length: compact ? 18 : 28 }, (_, i) => i), [compact])

  return (
    <div className={`sf-visualizer ${compact ? 'is-compact' : ''} ${active ? 'is-active' : ''}`} aria-label={active ? 'Music visualizer active' : 'Music visualizer'} role="img">
      {bars.map(i => {
        const wave = Math.sin((i + tick) * 0.72) * 0.5 + 0.5
        const shape = ((i * 17) % 11) / 10
        const height = active ? 18 + Math.round((wave * 0.65 + shape * 0.35) * 52) : 18
        return <span key={i} style={{ height: `${height}%` }} />
      })}
    </div>
  )
}
