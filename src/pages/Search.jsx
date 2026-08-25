import { useEffect, useState, useCallback, useRef } from 'react'
import { Search as SearchIcon, WifiOff, Clock, X, SearchX, RotateCw } from 'lucide-react'
import { searchMusic } from '@/lib/musicApi'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { useLocation } from 'react-router-dom'

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
  const { user, signIn } = useAuth()
  const location = useLocation()
  const [query, setQuery] = useState(location.state?.query || '')
  const debounced = useDebouncedValue(query, 500)
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [history, setHistory] = useState(loadHistory)
  const searchAbortRef = useRef(null)
  const player = usePlayer()
  const online = useOnlineStatus()

  // Search now goes through our own ytmusicapi-backed backend (api/main.py)
  // instead of the official (key-required, quota-limited) YouTube Data API —
  // see src/lib/musicApi.js. No API key needed here anymore.
  const runSearch = useCallback(async (q) => {
    if (!user) return // gated below the sign-in wall too; belt-and-braces
    if (q.trim().length < 2) {
      setResults([])
      return
    }
    if (!navigator.onLine) {
      setError("You're offline — online search needs a connection. Your local files still work from the Library tab.")
      setResults([])
      return
    }
    searchAbortRef.current?.abort()
    const controller = new AbortController()
    searchAbortRef.current = controller

    setLoading(true)
    setError(null)
    try {
      const tracks = await searchMusic(q, { signal: controller.signal })
      if (controller.signal.aborted) return
      setResults(tracks)
      setHistory(pushHistory(q) || loadHistory())
    } catch (err) {
      if (err?.name === 'AbortError') return
      setError(err.message || 'Unable to search right now. Please try again.')
    } finally {
      if (searchAbortRef.current === controller) setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (user) runSearch(debounced)
  }, [debounced, runSearch, user])

  useEffect(() => () => searchAbortRef.current?.abort(), [])

  function clearHistory() {
    localStorage.removeItem(HISTORY_KEY)
    setHistory([])
  }

  if (!user) {
    return (
      <div className="p-4 md:p-6 flex flex-col items-center text-center pt-20">
        <SearchIcon size={40} className="text-text-subdued mb-4" aria-hidden="true" />
        <h1 className="text-xl font-bold mb-2">Sign in to search</h1>
        <p className="text-text-muted text-sm max-w-sm mb-6">
          Searching and streaming online songs requires a free account. Your own local files don't
          — check the Local Files tab if you just want to play files from this device.
        </p>
        <button
          onClick={signIn}
          className="bg-brand hover:bg-brand-hover text-black font-bold px-6 py-2.5 rounded-full transition-colors"
        >
          Sign in with Google
        </button>
      </div>
    )
  }

  return (
    <div className="p-4 md:p-6">
      <div className="relative max-w-md mb-6">
        <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black" aria-hidden="true" />
        <input
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="What do you want to play?"
          aria-label="Search for songs, artists, or albums"
          className="w-full bg-white text-black placeholder-gray-600 rounded-full pl-11 pr-10 py-3 text-sm font-medium outline-none"
        />
        {query && (
          <button
            onClick={() => setQuery('')}
            aria-label="Clear search"
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-600 hover:text-black"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {!online && (
        <div className="flex items-center gap-2 text-sm text-yellow-500 bg-yellow-500/10 rounded-md px-3 py-2 mb-4 max-w-md">
          <WifiOff size={16} className="shrink-0" aria-hidden="true" />
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
                <Clock size={12} className="text-text-subdued" aria-hidden="true" />
                {h}
              </button>
            ))}
          </div>
        </div>
      )}

      {loading && <SkeletonRowList count={6} />}

      {error && (
        <div className="flex items-center justify-between gap-3 bg-red-500/10 text-red-400 text-sm rounded-md px-4 py-3 mb-4 max-w-md">
          <span>{error}</span>
          <button
            onClick={() => runSearch(query)}
            aria-label="Retry search"
            className="flex items-center gap-1 text-xs font-semibold shrink-0 hover:text-red-300"
          >
            <RotateCw size={14} /> Retry
          </button>
        </div>
      )}

      {!loading && !error && query && results.length === 0 && (
        <div className="text-center py-16">
          <SearchX size={40} className="mx-auto mb-3 text-text-subdued" aria-hidden="true" />
          <p className="text-text-muted text-sm">No results found for "{query}".</p>
        </div>
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
