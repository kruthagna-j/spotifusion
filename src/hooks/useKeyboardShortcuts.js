import { useEffect } from 'react'
import { usePlayer } from '@/context/PlayerContext'

// Spotify-style keyboard shortcuts. Ignored while typing in an input/textarea
// (e.g. the search box) so hitting space to type doesn't hijack playback.
export function useKeyboardShortcuts() {
  const player = usePlayer()

  useEffect(() => {
    function isTypingTarget(el) {
      if (!el) return false
      const tag = el.tagName
      return tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable
    }

    function onKeyDown(e) {
      if (isTypingTarget(document.activeElement)) return

      if (e.code === 'Space') {
        e.preventDefault() // stop page from scrolling
        if (player.currentTrack) player.togglePlay()
        return
      }
      if (e.code === 'ArrowRight' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault()
        player.playNext()
        return
      }
      if (e.code === 'ArrowLeft' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault()
        player.playPrevious()
        return
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [player])
}
