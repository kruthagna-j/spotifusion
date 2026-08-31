import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Search as SearchIcon, Music2, Disc3, UserRound, ListMusic, Radio, X, RotateCw, SearchX, LoaderCircle, Clock, Trash2, WifiOff } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { searchMusicPage } from '@/lib/search'
import { useAuth } from '@/context/AuthContext'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'
import { getArtwork } from '@/lib/artwork'

const HISTORY_KEY = 'spotifusion:search-history:v2'
const CACHE_PREFIX = 'spotifusion:search-results:v7:'
const CACHE_TTL = 30 * 60 * 1000
const MAX_HISTORY = 100
const CATEGORIES = [
  { id: 'all', label: 'All', icon: SearchIcon }, { id: 'songs', label: 'Songs', icon: Music2 },
  { id: 'albums', label: 'Albums', icon: Disc3 }, { id: 'artists', label: 'Artists', icon: UserRound },
  { id: 'playlists', label: 'Playlists', icon: ListMusic }, { id: 'jukebox', label: 'Jukebox', icon: Radio },
]
const clean = value => String(value || '').trim().replace(/\s+/g, ' ')
const cacheKey = (q, c) => `${CACHE_PREFIX}${c}:${clean(q).toLowerCase()}`
function readCache(q, c) { try { const x = JSON.parse(sessionStorage.getItem(cacheKey(q, c)) || 'null'); return x && Date.now() - x.time < CACHE_TTL && Array.isArray(x.results) && x.results.length ? x : null } catch { return null } }
function writeCache(q, c, x) { if (!q || !Array.isArray(x?.results) || !x.results.length) return; try { sessionStorage.setItem(cacheKey(q, c), JSON.stringify({ ...x, time: Date.now() })) } catch {} }
function readHistory() { try { const x = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]'); return Array.isArray(x) ? x.filter(x => typeof x === 'string').map(clean).filter(x => x.length >= 2).slice(0, MAX_HISTORY) : [] } catch { return [] } }
function addHistory(q) { const n = [q, ...readHistory().filter(x => x.toLowerCase() !== q.toLowerCase())].slice(0, MAX_HISTORY); try { localStorage.setItem(HISTORY_KEY, JSON.stringify(n)) } catch {}; return n }

function EntityCard({ item }) {
  const navigate = useNavigate(); const type = item.resultType
  const Icon = type === 'artist' ? UserRound : type === 'album' ? Disc3 : type === 'jukebox' ? Radio : ListMusic
  const label = type === 'artist' ? 'Artist' : type === 'album' ? 'Album' : type === 'jukebox' ? 'Jukebox' : 'Playlist'
  const id = item.browseId || item.id || item.playlistId || item.albumId || item.artistId; const image = getArtwork(item, 'large')
  const path = type === 'artist' ? `/artist/${encodeURIComponent(id)}` : type === 'album' ? `/album/${encodeURIComponent(id)}` : `/youtube-playlist/${encodeURIComponent(id)}`
  return <button type="button" onClick={() => navigate(path, { state: { name: item.title, artwork: image, browseId: id } })} className="sf-entity-card group text-left min-w-0"><div className="relative aspect-square overflow-hidden rounded-2xl bg-surface-highlight mb-3">{image ? <img src={image} alt="" loading="lazy" decoding="async" className={`w-full h-full object-cover ${type === 'artist' ? 'rounded-full' : ''}`} onError={e => { e.currentTarget.style.display = 'none' }}/> : <div className="w-full h-full grid place-items-center"><Icon size={34} className="text-text-subdued"/></div>}</div><p className="font-bold text-sm truncate">{item.title || 'Untitled'}</p><p className="text-xs text-text-muted truncate mt-1">{item.subtitle || item.artist || label}</p><span className="inline-flex items-center gap-1 mt-2 text-[10px] uppercase tracking-wider font-bold text-text-subdued"><Icon size={11}/>{label}</span></button>
}

export default function SearchStable2() {
  const { user } = useAuth(); const navigate = useNavigate(); const location = useLocation(); const online = useOnlineStatus()
  const params = useMemo(() => new URLSearchParams(location.search), [location.search])
  const urlQuery = clean(params.get('q')); const urlCategory = CATEGORIES.some(x => x.id === params.get('category')) ? params.get('category') : 'all'
  const [query, setQuery] = useState(urlQuery); const [submitted, setSubmitted] = useState(urlQuery); const [category, setCategory] = useState(urlCategory)
  const initial = readCache(urlQuery, urlCategory)
  const [results, setResults] = useState(initial?.results || []); const [batch, setBatch] = useState(initial?.batch || 0); const [hasMore, setHasMore] = useState(Boolean(initial?.hasMore))
  const [loading, setLoading] = useState(false); const [error, setError] = useState(''); const [history, setHistory] = useState(readHistory)
  const requestSeq = useRef(0); const moreLoading = useRef(false); const sentinelRef = useRef(null); const stateRef = useRef({ results: [], batch: 0, hasMore: false })
  useEffect(() => { stateRef.current = { results, batch, hasMore } }, [results, batch, hasMore])

  const groups = useMemo(() => {
    const seen = new Set(); const unique = results.filter(item => { const type = item?.resultType || item?.type || 'song'; const id = item?.id || item?.videoId || item?.browseId || item?.playlistId || item?.albumId || item?.artistId; const k = `${type}:${id}`; if (!id || seen.has(k)) return false; seen.add(k); return true })
    return { all: unique, songs: unique.filter(x => ['song', 'video'].includes(String(x.resultType || x.type || '').toLowerCase()) || !x.resultType && !x.type), albums: unique.filter(x => String(x.resultType || x.type || '').toLowerCase() === 'album'), artists: unique.filter(x => String(x.resultType || x.type || '').toLowerCase() === 'artist'), playlists: unique.filter(x => ['playlist'].includes(String(x.resultType || x.type || '').toLowerCase())), jukebox: unique.filter(x => ['jukebox', 'mix'].includes(String(x.resultType || x.type || '').toLowerCase())) }
  }, [results])

  const fetchCategory = useCallback(async (q, nextCategory, nextBatch = 1, replace = true) => {
    if (!user || !online || q.length < 2) return
    const seq = ++requestSeq.current; if (nextBatch === 1) setLoading(true); setError('')
    try {
      let data = await searchMusicPage(q, { category: nextCategory, batch: nextBatch })
      let next = Array.isArray(data?.results) ? data.results : []
      if (!next.length && nextCategory !== 'all') {
        const mixed = await searchMusicPage(q, { category: 'all', batch: nextBatch }); const mixedResults = Array.isArray(mixed?.results) ? mixed.results : []
        const allowed = nextCategory === 'songs' ? new Set(['song', 'video']) : nextCategory === 'albums' ? new Set(['album']) : nextCategory === 'artists' ? new Set(['artist']) : nextCategory === 'playlists' ? new Set(['playlist']) : new Set(['jukebox', 'mix'])
        next = mixedResults.filter(x => allowed.has(String(x?.resultType || x?.type || '').toLowerCase()))
        if (nextCategory === 'songs' && !next.length) next = mixedResults.filter(x => x?.id || x?.videoId).map(x => ({ ...x, id: x.id || x.videoId, resultType: 'song' }))
        data = { ...mixed, category: nextCategory }
      }
      if (seq !== requestSeq.current) return
      const nextHasMore = Boolean(data?.hasMore && next.length)
      setResults(replace || nextBatch === 1 ? next : [...stateRef.current.results, ...next]); setBatch(nextBatch); setHasMore(nextHasMore)
      writeCache(q, nextCategory, { results: next, batch: nextBatch, hasMore: nextHasMore })
    } catch (e) { if (seq !== requestSeq.current || e?.name === 'AbortError') return; setResults([]); setBatch(0); setHasMore(false); setError(e?.message || 'Unable to search right now. Please try again.') }
    finally { if (seq === requestSeq.current && nextBatch === 1) setLoading(false) }
  }, [user, online])

  useEffect(() => {
    if (!user) return
    setQuery(urlQuery); setSubmitted(urlQuery); setCategory(urlCategory); const cached = readCache(urlQuery, urlCategory)
    setResults(cached?.results || []); setBatch(cached?.batch || 0); setHasMore(Boolean(cached?.hasMore)); setError('')
    if (urlQuery && !cached?.results?.length) fetchCategory(urlQuery, urlCategory, 1, true)
  }, [user, urlQuery, urlCategory, fetchCategory])
  useEffect(() => () => { requestSeq.current += 1 }, [])
  useEffect(() => { const node = sentinelRef.current; if (!node || !submitted || !hasMore) return undefined; const observer = new IntersectionObserver(entries => { if (!entries[0]?.isIntersecting || moreLoading.current || !stateRef.current.hasMore) return; moreLoading.current = true; fetchCategory(submitted, category, stateRef.current.batch + 1, false).finally(() => { moreLoading.current = false }) }, { root: document.querySelector('main'), rootMargin: '900px 0px' }); observer.observe(node); return () => observer.disconnect() }, [submitted, category, hasMore, fetchCategory])

  function chooseCategory(next) { if (next === category) return; const q = submitted || query; setCategory(next); setResults([]); setBatch(0); setHasMore(false); setError(''); navigate(q ? `/search?q=${encodeURIComponent(q)}&category=${next}` : `/search?category=${next}`, { replace: true }) }
  function submit(value = query, nextCategory = category) { const q = clean(value); if (q.length < 2) { setError('Enter at least 2 characters to search.'); return }; setHistory(addHistory(q)); setQuery(q); setSubmitted(q); navigate(`/search?q=${encodeURIComponent(q)}&category=${nextCategory}`) }
  function clear() { requestSeq.current += 1; setQuery(''); setSubmitted(''); setResults([]); setBatch(0); setHasMore(false); setError(''); navigate('/search', { replace: true }) }
  if (!user) return <div className="p-6 text-center pt-20"><SearchIcon size={42} className="mx-auto mb-4 text-text-subdued"/><h1 className="text-xl font-bold">Sign in to search</h1><p className="text-sm text-text-muted mt-2 mb-6">Sign in to search and stream music.</p><button onClick={() => navigate('/login')} className="sf-primary-button">Sign in</button></div>
  const visible = groups[category] || []; const label = CATEGORIES.find(x => x.id === category)?.label || 'All'; const suggestions = query ? history.filter(x => x.toLowerCase().includes(query.toLowerCase())).slice(0, 6) : []
  return <div className="p-4 md:p-6 max-w-[1500px] mx-auto pb-36 min-w-0">
    <form onSubmit={e => { e.preventDefault(); submit() }} className="relative max-w-3xl mb-4"><SearchIcon size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-black"/><input autoFocus value={query} onChange={e => { setQuery(e.target.value); setError('') }} placeholder="What do you want to play?" className="w-full bg-white text-black rounded-full pl-11 pr-28 py-3.5 outline-none"/>{query && <button type="button" onClick={clear} className="absolute right-24 top-1/2 -translate-y-1/2 text-gray-600 p-2"><X size={16}/></button>}<button type="submit" disabled={query.trim().length < 2} className="absolute right-1 top-1/2 -translate-y-1/2 sf-search-button">Search</button></form>
    <div className="search-category-tabs" role="tablist" aria-label="Search category">{CATEGORIES.map(({ id, label: tabLabel, icon: Icon }) => <button key={id} type="button" role="tab" aria-selected={category === id} onClick={() => chooseCategory(id)} className={`search-category-tab ${category === id ? 'is-active' : ''}`}><Icon size={16}/><span>{tabLabel}</span></button>)}</div>
    {!online && <div className="flex gap-2 items-center text-sm text-yellow-500 bg-yellow-500/10 rounded-xl p-3 mb-4"><WifiOff size={16}/> You're offline. Online search needs a connection.</div>}
    {history.length > 0 && !query && <div className="mb-6"><div className="flex justify-between mb-2"><span className="text-sm font-semibold">Recent searches</span><button type="button" onClick={() => { localStorage.removeItem(HISTORY_KEY); setHistory([]) }} className="sf-ghost-button"><Trash2 size={14}/> Clear</button></div><div className="flex flex-wrap gap-2">{history.slice(0, 10).map(x => <button key={x} type="button" onClick={() => submit(x)} className="sf-history-chip"><Clock size={12}/>{x}</button>)}</div></div>}
    {loading && <SkeletonRowList count={8}/>} {error && !loading && <div className="flex justify-between items-center gap-3 bg-red-500/10 text-red-300 rounded-xl p-3 mb-4 max-w-3xl"><span className="text-sm">{error}</span><button type="button" onClick={() => fetchCategory(submitted, category, 1, true)} className="sf-ghost-button"><RotateCw size={14}/> Retry</button></div>}
    {!loading && !error && submitted && batch > 0 && !visible.length && <div className="text-center py-16 text-text-muted"><SearchX size={40} className="mx-auto mb-3"/><p>No {label.toLowerCase()} results for “{submitted}”.</p></div>}
    {visible.length > 0 && <section><div className="flex items-end justify-between mb-4"><div><p className="text-xs text-brand uppercase tracking-widest font-bold">Search results</p><h2 className="text-2xl font-black">{label}</h2></div><span className="text-xs text-text-subdued">{visible.length} loaded</span></div>{category === 'songs' ? <div className="space-y-1">{groups.songs.map((track, index) => <TrackRow key={track.id || track.videoId} track={track} index={index} contextTracks={groups.songs}/>)}</div> : <div className="search-entity-grid">{visible.map(item => <EntityCard key={`${item.resultType || item.type || category}-${item.id || item.browseId || item.playlistId || item.albumId || item.artistId}`} item={item}/>)}</div>}</section>}
    {submitted && batch > 0 && <div ref={sentinelRef} className="min-h-16 flex items-center justify-center text-text-subdued">{hasMore && <LoaderCircle size={18} className="animate-spin"/>}</div>}
    {!submitted && history.length === 0 && <div className="py-14 text-center text-text-subdued text-sm">Search for a song, artist, album, playlist or jukebox.</div>}
  </div>
}