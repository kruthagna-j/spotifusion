import { useEffect, useState, useCallback } from 'react'
import { Search as SearchIcon, WifiOff, Clock } from 'lucide-react'
import { searchMusic } from '@/lib/musicApi'
import { usePlayer } from '@/context/PlayerContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'

const HISTORY_KEY = 'spotifusion:search-history'
const MAX_HISTORY = 8

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []
  } catch {
    return []
  }
}

function pushHistory(query) {
  const q = query.trim()
  if (!q) return
  const existing = loadHistory().filter((h) => h.toLowerCase() !== q.toLowerCase())
  const next = [q, ...existing].slice(0, MAX_HISTORY)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
  return next
}

// simple debounce
function useDebouncedValue(value, delay) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}

export default function Search() {
  const [query, setQuery] = useState('')
  const debounced = useDebouncedValue(query, 400)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState(loadHistory)
  const player = usePlayer()
  const online = useOnlineStatus()

  // Search now goes through our own ytmusicapi-backed backend (api/main.py)
  // instead of the official (key-required, quota-limited) YouTube Data API —
  // see src/lib/musicApi.js. No API key needed here anymore.
  const runSearch = useCallback(async (q) => {
    if (!q.trim()) {
      setResults([])
      return
    }
    if (!navigator.onLine) {
      setError("You're offline — online search needs a connection. Your local files still work from the Library tab.")
      setResults([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      const tracks = await searchMusic(q)
      setResults(tracks)
      setHistory(pushHistory(q) || loadHistory())
    } catch (err) {
      setError(err.message || 'Unable to search right now. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    runSearch(debounced)
  }, [debounced, runSearch])

  function clearHistory() {
    localStorage.removeItem(HISTORY_KEY)
    setHistory([])
  }

  return (
    <div className="p-4 md:p-6">
      <div className="relative max-w-md mb-6">
        <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black" />
        <input
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="What do you want to play?"
          className="w-full bg-white text-black placeholder-gray-600 rounded-full pl-11 pr-4 py-3 text-sm font-medium outline-none"
        />
      </div>

      {!online && (
        <div className="flex items-center gap-2 text-sm text-yellow-500 bg-yellow-500/10 rounded-md px-3 py-2 mb-4 max-w-md">
          <WifiOff size={16} className="shrink-0" />
          You're offline — online search is unavailable right now.
        </div>
      )}

      {!query && history.length > 0 && (
        <div className="mb-6 max-w-md">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-semibold text-text-muted">Recent searches</p>
            <button onClick={clearHistory} className="text-xs text-text-subdued hover:text-text">
              Clear
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            {history.map((h) => (
              <button
                key={h}
                onClick={() => setQuery(h)}
                className="flex items-center gap-1.5 bg-surface-elevated hover:bg-surface-hover text-xs px-3 py-1.5 rounded-full"
              >
                <Clock size={12} className="text-text-subdued" />
                {h}
              </button>
            ))}
          </div>
        </div>
      )}

      {loading && <p className="text-text-muted text-sm">Searching…</p>}
      {error && <p className="text-red-400 text-sm">{error}</p>}

      {!loading && !error && query && results.length === 0 && (
        <p className="text-text-muted text-sm">No results found for "{query}".</p>
      )}

      {results.length > 0 && (
        <div className="flex flex-col">
          {results.map((track, i) => (
            <TrackRow key={track.id} track={track} index={i} contextTracks={results} />
          ))}
        </div>
      )}

      {!query && (
        <p className="text-text-subdued text-sm">
          Search YouTube Music for any song, artist, or album.
        </p>
      )}
    </div>
  )
}
