import { useCallback, useRef } from 'react'

/** Touch/mouse friendly circular seek wheel. The center is the artwork; the ring is the progress control. */
export default function CircularSeek({ progress = 0, duration = 0, artwork, onSeek }) {
  const ref = useRef(null)
  const radius = 46
  const circumference = 2 * Math.PI * radius
  const ratio = duration > 0 ? Math.min(1, Math.max(0, progress / duration)) : 0
  const dash = circumference * ratio

  const seekFromPoint = useCallback((clientX, clientY) => {
    const el = ref.current
    if (!el || !duration) return
    const rect = el.getBoundingClientRect()
    const cx = rect.left + rect.width / 2
    const cy = rect.top + rect.height / 2
    let angle = Math.atan2(clientY - cy, clientX - cx) * 180 / Math.PI + 90
    if (angle < 0) angle += 360
    onSeek((angle / 360) * duration)
  }, [duration, onSeek])

  const onPointerDown = (e) => {
    e.currentTarget.setPointerCapture?.(e.pointerId)
    seekFromPoint(e.clientX, e.clientY)
  }
  const onPointerMove = (e) => {
    if (e.buttons) seekFromPoint(e.clientX, e.clientY)
  }

  return (
    <div ref={ref} className="ref-vinyl-wrap ref-circular-seek" onPointerDown={onPointerDown} onPointerMove={onPointerMove} role="slider" aria-label="Song position" aria-valuemin={0} aria-valuemax={duration || 0} aria-valuenow={progress} tabIndex={0}
      onKeyDown={(e) => {
        if (!duration) return
        if (e.key === 'ArrowRight' || e.key === 'ArrowUp') { e.preventDefault(); onSeek(Math.min(duration, progress + Math.max(1, duration / 100))) }
        if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') { e.preventDefault(); onSeek(Math.max(0, progress - Math.max(1, duration / 100))) }
      }}>
      <svg className="ref-seek-svg" viewBox="0 0 100 100" aria-hidden="true">
        <circle cx="50" cy="50" r={radius} fill="none" stroke="#e0e5f1" strokeWidth="1.8" />
        <circle cx="50" cy="50" r={radius} fill="none" stroke="#2f5cff" strokeWidth="2.4" strokeLinecap="round" strokeDasharray={`${dash} ${circumference - dash}`} transform="rotate(-90 50 50)" />
      </svg>
      <div className="ref-vinyl">
        {artwork ? <img src={artwork} alt="" /> : <div className="ref-vinyl-placeholder" />}
      </div>
      <span className="ref-seek-knob" style={{ transform: `rotate(${ratio * 360}deg) translateY(calc(-1 * min(38vw, 162px)))` }} />
    </div>
  )
}
