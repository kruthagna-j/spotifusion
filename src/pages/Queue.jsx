import { usePlayer } from '@/context/PlayerContext'
import QueuePanel from '@/components/QueuePanel'
import { ListMusic } from 'lucide-react'

// Full-page queue view (used on mobile where the panel overlay is too small)
export default function Queue() {
  const { queue, currentTrack } = usePlayer()

  return (
    <div className="p-4 md:p-6 max-w-2xl">
      <div className="flex items-center gap-3 mb-6">
        <ListMusic size={28} className="text-brand" />
        <h1 className="text-2xl font-bold">Queue</h1>
      </div>
      {!currentTrack && !queue.length ? (
        <div className="text-center py-16">
          <ListMusic size={40} className="mx-auto mb-3 text-text-subdued" aria-hidden="true" />
          <p className="text-text-muted text-sm">Nothing is queued yet.</p>
          <p className="text-xs text-text-subdued mt-2">
            Play a song or use "Play Next" / "Add to Queue" from any track.
          </p>
        </div>
      ) : (
        // Reuse QueuePanel in embedded mode (no absolute positioning / popup styles)
        <QueuePanel embedded onClose={() => {}} />
      )}
    </div>
  )
}
