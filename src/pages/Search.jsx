import { useEffect, useState, useCallback } from 'react'
import { Search as SearchIcon } from 'lucide-react'
import { searchTracks } from '@/lib/youtube'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'

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
  const player = usePlayer()

  const runSearch = useCallback(async (q) => {
    if (!q.trim()) {
      setResults([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      const tracks = await searchTracks(q)
      setResults(tracks)
    } catch (err) {
      setError('Could not reach the search backend. Is the Cloud Function running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    runSearch(debounced)
  }, [debounced, runSearch])

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

      {loading && <p className="text-text-muted text-sm">Searching…</p>}
      {error && <p className="text-red-400 text-sm">{error}</p>}

      {!loading && !error && query && results.length === 0 && (
        <p className="text-text-muted text-sm">No trusted results found for "{query}".</p>
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
          Search pulls from official artist channels and verified-audio uploads on YouTube first.
        </p>
      )}
    </div>
  )
}
