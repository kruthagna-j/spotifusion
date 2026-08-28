import { useEffect, useMemo, useRef, useState } from 'react'
import { Search as SearchIcon, WifiOff, Clock, X, SearchX, RotateCw, Trash2, Disc3, UserRound, ListMusic, Radio, Music2 } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { searchMusicPage } from '@/lib/search'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { getArtwork } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v3'
const CACHE_PREFIX = 'spotifusion:search-results:v6:'
const CACHE_TTL = 60 * 60 * 1000
const CATEGORIES = [
  ['all', 'All', SearchIcon],
  ['songs', 'Songs', Music2],
  ['albums', 'Albums', Disc3],
  ['artists', 'Artists', UserRound],
  ['playlists', 'Playlists', ListMusic],
  ['jukebox', 'Jukebox', Radio],
]

const clean = (value) => String(value || '').trim().replace(/\s+/g, ' ')
const cacheKey = (query, category) => `${CACHE_PREFIX}${category}:${clean(query).toLowerCase()}`

function cacheRead(query, category) {
  try {
    const value = JSON.parse(sessionStorage.getItem(cacheKey(query, category)) || 'null')
    return value && Date.now() - value.time < CACHE_TTL ? value : null
  } catch {
    return null
  }
}

function cacheWrite(query, category, data) {
  try {
    sessionStorage.setItem(cacheKey(query, category), JSON.stringify({ ...data, time: Date.now() }))
  } catch {
    // Storage is optional.
  }
}

function historyRead() {
  try {
    const value = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

function historyAdd(query) {
  const next = [query, ...historyRead().filter((value) => value.toLowerCase() !== query.toLowerCase())].slice(0, 100)
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(next))
  } catch {
    // Storage is optional.
  }
  return next
}

function EntityCard({ item }) {
  const navigate = useNavigate()
  const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const id = item.browseId || item.id
  const image = getArtwork(item, 'large')
  const path = type === 'artist'
    ? `/artist/${encodeURIComponent(id)}`
    : type === 'album'
      ? `/album/${encodeURIComponent(id)}`
      : `/youtube-playlist/${encodeURIComponent(id)}`

  return (
    <button
      type="button"
      onClick={() => navigate(path, { state: { name: item.title, artwork: image, browseId: id } })}
      className="sf-entity-card group text-left min-w-0"
    >
      <div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">
        {image ? (
          <img src={image} alt="" loading="lazy" className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full' : ''}`} />
        ) : (
          <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued" /></div>
        )}
      </div>
      <p className="font-bold text-sm truncate">{item.title || 'Untitled'}</p>
      <p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || type}</p>
    </button>
  )
}

export default function SearchFixed() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const online = useOnlineStatus()
  const params = useMemo(() => new URLSearchParams(location.search), [location.search])
  const urlQuery = clean(params.get('q'))
  const requestedCategory = params.get('category')
  const urlCategory = CATEGORIES.some((entry) => entry[0] === requestedCategory) ? requestedCategory : 'all'

  const initialCache = cacheRead(urlQuery, urlCategory)
  const [query, setQuery] = useState(urlQuery)
  const [submitted, setSubmitted] = useState(urlQuery)
  const [category, setCategory] = useState(urlCategory)
  const [results, setResults] = useState(initialCache?.results || [])
  const [page, setPage] = useState(initialCache?.batch || 0)
  const [hasMore, setHasMore] = useState(Boolean(initialCache?.hasMore))
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [history, setHistory] = useState(historyRead)

  const abortRef = useRef(null)
  const requestId = useRef(0)
  const moreRef = useRef(false)
  const sentinelRef = useRef(null)
  const latest = useRef({ results: [], page: 0, hasMore: false })

  useEffect(() => {
    latest.current = { results, page, hasMore }
  }, [results, page, hasMore])

  const groups = useMemo(() => {
    const seen = new Set()
    const unique = results.filter((item) => {
      const key = `${item?.resultType || 'song'}:${item?.id || ''}`
      if (!item?.id || seen.has(key)) return false
      seen.add(key)
      return true
    })
    return {
      all: unique,
      songs: unique.filter((item) => !item.resultType || item.resultType === 'song'),
      albums: unique.filter((item) => item.resultType === 'album'),
      artists: unique.filter((item) => item.resultType === 'artist'),
      playlists: unique.filter((item) => item.resultType === 'playlist'),
      jukebox: unique.filter((item) => item.resultType === 'jukebox' || item.resultType === 'mix'),
    }
  }, [results])

  async function load(queryValue, categoryValue, batch = 1) {
    const cleanQuery = clean(queryValue)
    if (!user || cleanQuery.length < 2 || !navigator.onLine) return

    if (batch === 1) {
      abortRef.current?.abort()
      const controller = new AbortController()
      const request = ++requestId.current
      abortRef.current = controller
      setLoading(true)
      setError('')

      try {
        const data = await searchMusicPage(cleanQuery, {
          category: categoryValue,
          batch: 1,
          signal: controller.signal,
        })

        if (controller.signal.aborted || request !== requestId.current) return

        const nextResults = Array.isArray(data?.results) ? data.results : []
        const more = Boolean(data?.hasMore)
        setResults(nextResults)
        setPage(1)
        setHasMore(more)
        cacheWrite(cleanQuery, categoryValue, { results: nextResults, batch: 1, hasMore: more })
      } catch (err) {
        if (err?.name !== 'AbortError' && request === requestId.current) {
          setResults([])
          setPage(0)
          setHasMore(false)
          setError(err?.message || 'Unable to search right now.')
        }
      }

      if (abortRef.current === controller) setLoading(false)
      return
    }

    if (moreRef.current || !latest.current.hasMore) return
    moreRef.current = true
    setLoadingMore(true)

    try {
      const data = await searchMusicPage(cleanQuery, { category: categoryValue, batch })
      const incoming = Array.isArray(data?.results) ? data.results : []
      const seen = new Set(latest.current.results.map((item) => `${item?.resultType || 'song'}:${item?.id || ''}`))
      const fresh = incoming.filter((item) => {
        const key = `${item?.resultType || 'song'}:${item?.id || ''}`
        if (!item?.id || seen.has(key)) return false
        seen.add(key)
        return true
      })

      if (!fresh.length) {
        setHasMore(false)
        return
      }

      const merged = [...latest.current.results, ...fresh]
      const more = Boolean(data?.hasMore)
      setResults(merged)
      setPage(batch)
      setHasMore(more)
      cacheWrite(cleanQuery, categoryValue, { results: merged, batch, hasMore: more })
    } catch (err) {
      setError(err?.message || 'Unable to load more results.')
    } finally {
      moreRef.current = false
      setLoadingMore(false)
    }
  }

  useEffect(() => {
    const cached = cacheRead(urlQuery, urlCategory)
    setQuery(urlQuery)
    setSubmitted(urlQuery)
    setCategory(urlCategory)
    setResults(cached?.results || [])
    setPage(cached?.batch || 0)
    setHasMore(Boolean(cached?.hasMore))
    setError('')
    if (!urlQuery) abortRef.current?.abort()
  }, [urlQuery, urlCategory])

  useEffect(() => {
    if (user && submitted.length >= 2 && page === 0) load(submitted, category, 1)
  }, [user, submitted, category, page])

  useEffect(() => () => abortRef.current?.abort(), [])

  useEffect(() => {
    const node = sentinelRef.current
    if (!node || !submitted || !hasMore) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !loading && !loadingMore) {
          load(submitted, category, latest.current.page + 1)
        }
      },
      { root: document.querySelector('main'), rootMargin: '900px' },
    )

    observer.observe(node)
    return () => observer.disconnect()
  }, [submitted, category, hasMore, loading, loadingMore])

  function submit(value = query, categoryValue = category) {
    const nextQuery = clean(value)
    if (nextQuery.length < 2) {
      setError('Enter at least 2 characters to search.')
      return
    }
    setHistory(historyAdd(nextQuery))
    setQuery(nextQuery)
    setSubmitted(nextQuery)
    setResults([])
    setPage(0)
    setHasMore(false)
    navigate(`/search?q=${encodeURIComponent(nextQuery)}&category=${categoryValue}`)
  }

  function chooseCategory(nextCategory) {
    if (nextCategory === category) return
    setResults([])
    setPage(0)
    setHasMore(false)
    setError('')
    navigate(submitted ? `/search?q=${encodeURIComponent(submitted)}&category=${nextCategory}` : `/search?category=${nextCategory}`)
  }

  function clear() {
    abortRef.current?.abort()
    ++requestId.current
    setQuery('')
    setSubmitted('')
    setResults([])
    setPage(0)
    setHasMore(false)
    navigate('/search', { replace: true })
  }

  if (!user) {
    return (
      <div className="p-6 text-center pt-20">
        <SearchIcon size={42} className="mx-auto mb-4 text-text-subdued" />
        <h1 className="text-xl font-bold">Sign in to search</h1>
        <button onClick={() => navigate('/login')} className="sf-primary-button mt-6">Sign in</button>
      </div>
    )
  }

  const visible = groups[category] || []
  const label = CATEGORIES.find((entry) => entry[0] === category)?.[1] || 'All'
  const suggestions = query ? history.filter((value) => value.toLowerCase().includes(query.toLowerCase())).slice(0, 6) : []

  return (
    <div className="p-4 md:p-6 max-w-[1500px] mx-auto pb-36 min-w-0">
      <form onSubmit={(event) => { event.preventDefault(); submit() }} className="relative max-w-3xl mb-4">
        <SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black" />
        <input value={query} onChange={(event) => { setQuery(event.target.value); setError('') }} placeholder="What do you want to play?" className="w-full bg-white text-black rounded-full pl-11 pr-28 py-3.5 outline-none" />
        {query && <button type="button" onClick={clear} className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 p-2"><X size={16} /></button>}
        <button type="submit" disabled={query.trim().length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button>
      </form>

      <div className="search-category-tabs">
        {CATEGORIES.map(([id, labelText, Icon]) => (
          <button key={id} type="button" onClick={() => chooseCategory(id)} className={`search-category-tab ${category === id ? 'is-active' : ''}`}>
            <Icon size={16} />{labelText}
          </button>
        ))}
      </div>

      {!online && <div className="flex gap-2 items-center text-sm text-yellow-500 bg-yellow-500/10 rounded-xl p-3 mb-4"><WifiOff size={16} />You're offline.</div>}

      {suggestions.length > 0 && !loading && <div className="flex flex-wrap gap-2 mb-5">{suggestions.map((value) => <button key={value} type="button" onClick={() => submit(value)} className="sf-history-chip"><SearchIcon size={12} />{value}</button>)}</div>}

      {history.length > 0 && !query && (
        <div className="mb-6">
          <div className="flex justify-between mb-2"><span className="text-sm font-semibold">Recent searches</span><button type="button" onClick={() => { localStorage.removeItem(HISTORY_KEY); setHistory([]) }} className="sf-ghost-button"><Trash2 size={14} />Clear</button></div>
          <div className="flex flex-wrap gap-2">{history.slice(0, 10).map((value) => <button type="button" key={value} onClick={() => submit(value)} className="sf-history-chip"><Clock size={12} />{value}</button>)}</div>
        </div>
      )}

      {loading && <SkeletonRowList count={8} />}
      {error && !loading && <div className="flex justify-between items-center gap-3 bg-red-500/10 text-red-300 rounded-xl p-3 mb-4"><span className="text-sm">{error}</span><button type="button" onClick={() => load(submitted, category, 1)} className="sf-ghost-button"><RotateCw size={14} />Retry</button></div>}
      {!loading && !error && submitted && page > 0 && !visible.length && <div className="text-center py-16 text-text-muted"><SearchX size={40} className="mx-auto mb-3" /><p>No {label.toLowerCase()} results for “{submitted}”.</p></div>}

      {visible.length > 0 && (
        <section>
          <div className="flex items-end justify-between mb-4"><div><p className="text-xs text-brand uppercase tracking-widest font-bold">Search results</p><h2 className="text-2xl font-black">{label}</h2></div><span className="text-xs text-text-subdued">{visible.length} loaded</span></div>

          {category === 'songs' && <div className="space-y-1">{groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs} />)}</div>}

          {category === 'all' && (
            <div className="space-y-8">
              {groups.artists.length > 0 && <div><h3 className="text-lg font-bold mb-3">Artists</h3><div className="search-entity-grid">{groups.artists.map((item) => <EntityCard key={item.id} item={item} />)}</div></div>}
              {groups.albums.length > 0 && <div><h3 className="text-lg font-bold mb-3">Albums</h3><div className="search-entity-grid">{groups.albums.map((item) => <EntityCard key={item.id} item={item} />)}</div></div>}
              {groups.songs.length > 0 && <div><h3 className="text-lg font-bold mb-3">Songs</h3><div className="space-y-1">{groups.songs.map((track, index) => <TrackRow key={track.id} track={track} index={index} contextTracks={groups.songs} />)}</div></div>}
              {groups.playlists.length > 0 && <div><h3 className="text-lg font-bold mb-3">Playlists</h3><div className="search-entity-grid">{groups.playlists.map((item) => <EntityCard key={item.id} item={item} />)}</div></div>}
            </div>
          )}

          {(category === 'albums' || category === 'artists' || category === 'playlists' || category === 'jukebox') && <div className="search-entity-grid">{visible.map((item) => <EntityCard key={item.id} item={item} />)}</div>}
          <div ref={sentinelRef} className="h-8" />
          {loadingMore && <div className="text-center text-sm text-text-subdued py-5">Loading more…</div>}
        </section>
      )}
    </div>
  )
}
