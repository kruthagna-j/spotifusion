import { X, GripVertical, Trash2, ArrowUp, ArrowDown } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'

export default function QueuePanel({ onClose, embedded = false }) {
  const { queue, queueIndex, removeFromQueue, reorderQueue, clearQueue, playTrack } = usePlayer()

  const upNext = queue.slice(queueIndex + 1)

  return (
    <div className={embedded ? 'w-full max-h-[58vh] flex flex-col bg-transparent overflow-hidden' : 'absolute bottom-full right-0 mb-2 w-80 max-h-[70vh] flex flex-col bg-surface-elevated rounded-lg shadow-2xl border border-border z-40 overflow-hidden'}>
      <div className="flex items-center justify-between px-4 py-3 border-b border-border shrink-0">
        <p className="font-bold text-sm" id="queue-panel-heading">Queue</p>
        <div className="flex items-center gap-3">
          {upNext.length > 0 && (
            <button onClick={clearQueue} className="text-xs text-text-muted hover:text-text">
              Clear
            </button>
          )}
          <button onClick={onClose} aria-label="Close queue" className="text-text-muted hover:text-text">
            <X size={16} />
          </button>
        </div>
      </div>

      <div className="overflow-y-auto">
        {queueIndex >= 0 && queue[queueIndex] && (
          <div className="px-4 py-2">
            <p className="text-[11px] font-bold text-brand uppercase tracking-wide mb-1">Now playing</p>
            <div className="flex items-center gap-3">
              <img
                src={queue[queueIndex].thumbnail}
                alt=""
                className="w-10 h-10 rounded object-cover shrink-0"
              />
              <div className="min-w-0">
                <p className="text-sm truncate">{queue[queueIndex].title}</p>
                <p className="text-xs text-text-subdued truncate">{queue[queueIndex].artist}</p>
              </div>
            </div>
          </div>
        )}

        {upNext.length > 0 && (
          <p className="text-[11px] font-bold text-text-muted uppercase tracking-wide px-4 pt-3 pb-1">
            Next up
          </p>
        )}

        {upNext.length === 0 ? (
          <p className="text-xs text-text-subdued px-4 py-6 text-center">
            Nothing queued next — add songs with "Play Next" from any track's menu.
          </p>
        ) : (
          upNext.map((track, i) => {
            const realIndex = queueIndex + 1 + i
            return (
              <div key={`${track.id}-${realIndex}`} className="group flex items-center gap-2 px-4 py-2 hover:bg-surface-hover">
                <GripVertical size={14} className="text-text-subdued shrink-0" />
                <button
                  onClick={() => playTrack(track, queue)}
                  className="flex items-center gap-3 min-w-0 flex-1 text-left"
                >
                  <img src={track.thumbnail} alt="" className="w-9 h-9 rounded object-cover shrink-0" />
                  <div className="min-w-0">
                    <p className="text-sm truncate">{track.title}</p>
                    <p className="text-xs text-text-subdued truncate">{track.artist}</p>
                  </div>
                </button>
                <div className="flex items-center opacity-0 group-hover:opacity-100 shrink-0">
                  {i > 0 && (
                    <button
                      onClick={() => reorderQueue(realIndex, realIndex - 1)}
                      className="p-1 text-text-muted hover:text-text"
                      aria-label={`Move ${track.title} up in queue`}
                      title="Move up"
                    >
                      <ArrowUp size={14} />
                    </button>
                  )}
                  {i < upNext.length - 1 && (
                    <button
                      onClick={() => reorderQueue(realIndex, realIndex + 1)}
                      className="p-1 text-text-muted hover:text-text"
                      aria-label={`Move ${track.title} down in queue`}
                      title="Move down"
                    >
                      <ArrowDown size={14} />
                    </button>
                  )}
                  <button
                    onClick={() => removeFromQueue(realIndex)}
                    className="p-1 text-text-muted hover:text-red-400"
                    aria-label={`Remove ${track.title} from queue`}
                    title="Remove"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
