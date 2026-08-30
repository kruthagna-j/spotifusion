import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, ArrowRight, Check, LoaderCircle, Music2, Play, Search, Sparkles } from 'lucide-react'
import { searchMusicPage } from '@/lib/search'
import { getArtwork } from '@/lib/artwork'

const LANGUAGES = ['English','Telugu','Hindi','Tamil','Kannada','Malayalam','Marathi','Bengali','Punjabi','Gujarati','Odia']
const languageQueries = Object.fromEntries(LANGUAGES.map((x) => [x, `${x} popular singers`]))

export default function PreferenceWizard({ initialLanguages = [], initialArtists = [], initialSongs = [], onFinish, onCancel }) {
  const [step, setStep] = useState(1)
  const [languages, setLanguages] = useState(initialLanguages.length ? initialLanguages : [])
  const [artists, setArtists] = useState(initialArtists)
  const [songs, setSongs] = useState(initialSongs)
  const [suggestedArtists, setSuggestedArtists] = useState([])
  const [songSuggestions, setSongSuggestions] = useState([])
  const [query, setQuery] = useState('')
  const [artistResults, setArtistResults] = useState([])
  const [loadingArtists, setLoadingArtists] = useState(false)
  const [loadingSongs, setLoadingSongs] = useState(false)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState('')

  const artistIds = useMemo(() => new Set(artists.map((x) => x.id)), [artists])
  const songIds = useMemo(() => new Set(songs.map((x) => x.id)), [songs])

  useEffect(() => {
    if (step !== 2 || !languages.length || suggestedArtists.length) return
    let cancelled = false
    async function load() {
      setLoadingArtists(true); setError('')
      const results = await Promise.allSettled(languages.map((lang) => searchMusicPage(languageQueries[lang] || `${lang} singers`, { category: 'artists', batch: 1 })))
      if (cancelled) return
      const merged = []; const seen = new Set()
      results.forEach((r) => {
        if (r.status !== 'fulfilled') return
        ;(r.value.results || []).forEach((item) => {
          if (item.resultType !== 'artist' || !item.id || seen.has(item.id)) return
          seen.add(item.id); merged.push(item)
        })
      })
      setSuggestedArtists(merged.slice(0, 30))
      if (!merged.length) setError('No artist suggestions were returned. Search for an artist below.')
      setLoadingArtists(false)
    }
    load().catch((e) => { if (!cancelled) { setError(e?.message || 'Could not load artist suggestions.'); setLoadingArtists(false) } })
    return () => { cancelled = true }
  }, [step, languages, suggestedArtists.length])

  useEffect(() => {
    if (step !== 3 || !artists.length) return
    let cancelled = false
    async function load() {
      setLoadingSongs(true); setError('')
      const targets = artists.slice(0, 8)
      const languageText = languages.slice(0, 6).join(' ')
      const results = await Promise.allSettled(targets.map((artist) => searchMusicPage(`${artist.name} songs ${languageText}`, { category: 'songs', batch: 1 })))
      if (cancelled) return
      const merged = []; const seen = new Set()
      results.forEach((r, i) => {
        if (r.status !== 'fulfilled') return
        ;(r.value.results || []).forEach((item) => {
          if (item.resultType !== 'song' || !item.id || seen.has(item.id)) return
          seen.add(item.id); merged.push({ ...item, matchedArtist: targets[i].name })
        })
      })
      setSongSuggestions(merged.slice(0, 40))
      if (!merged.length) setError('No song suggestions were returned. You can still finish and search anytime.')
      setLoadingSongs(false)
    }
    load().catch((e) => { if (!cancelled) { setError(e?.message || 'Could not load song suggestions.'); setLoadingSongs(false) } })
    return () => { cancelled = true }
  }, [step])

  useEffect(() => {
    if (step !== 2 || query.trim().length < 2) { setArtistResults([]); return }
    const timer = setTimeout(async () => {
      setSearching(true)
      try {
        const page = await searchMusicPage(query, { category: 'artists', batch: 1 })
        setArtistResults((page.results || []).filter((x) => x.resultType === 'artist').slice(0, 12))
      } catch (e) { setError(e?.message || 'Artist search failed.') }
      finally { setSearching(false) }
    }, 350)
    return () => clearTimeout(timer)
  }, [query, step])

  function toggleLanguage(language) {
    setLanguages((prev) => prev.includes(language) ? prev.filter((x) => x !== language) : [...prev, language])
    setSuggestedArtists([])
  }
  function toggleArtist(item) {
    setArtists((prev) => artistIds.has(item.id) ? prev.filter((x) => x.id !== item.id) : [...prev, { id: item.id, name: item.title, image: getArtwork(item, 'large') }])
  }
  function toggleSong(item) {
    setSongs((prev) => songIds.has(item.id) ? prev.filter((x) => x.id !== item.id) : [...prev, item])
  }

  return <div className="sf-onboarding-page px-3 py-4 sm:px-6">
    <div className="sf-onboarding-card w-full max-w-3xl max-h-[calc(100vh-2rem)] overflow-y-auto">
      <div className="flex items-center gap-2 text-brand font-black"><Music2 size={22}/> Spotifusion</div>
      <div className="sf-onboarding-progress"><span style={{ width: `${step * 33.333}%` }}/></div>

      {step === 1 && <section>
        <p className="text-xs uppercase tracking-[.18em] text-brand font-black">Step 1 of 3</p>
        <h1 className="sf-onboarding-title text-[clamp(2rem,7vw,3.5rem)] leading-[1.05]">What languages do you listen to?</h1>
        <p className="text-sm text-text-muted mb-5">Choose as many as you want. These choices control the artists and songs suggested to you.</p>
        <div className="flex flex-wrap gap-2.5">{LANGUAGES.map((language) => {
          const selected = languages.includes(language)
          return <button key={language} type="button" onClick={() => toggleLanguage(language)} className={`px-4 py-3 rounded-full border text-sm font-bold ${selected ? 'border-brand bg-brand/10 text-brand' : 'border-white/10 bg-white/[.03] text-text hover:bg-white/[.07]'}`}><span className="flex items-center gap-2">{language}{selected && <Check size={16}/>}</span></button>
        })}</div>
        <p className="text-xs text-brand font-semibold mt-4">{languages.length} language{languages.length === 1 ? '' : 's'} selected</p>
        <button disabled={!languages.length} onClick={() => setStep(2)} className="sf-primary-button w-full mt-5 disabled:opacity-40">Continue <ArrowRight size={17}/></button>
      </section>}

      {step === 2 && <section>
        <p className="text-xs uppercase tracking-[.18em] text-brand font-black">Step 2 of 3</p>
        <h1 className="sf-onboarding-title text-[clamp(2rem,7vw,3.5rem)] leading-[1.05]">Choose your favorite artists.</h1>
        <p className="text-sm text-text-muted mb-4">We suggest artists from the languages you selected. You can also search for anyone.</p>
        <div className="flex items-center gap-2 text-brand text-xs font-bold mb-3"><Sparkles size={15}/> Suggestions for {languages.join(', ')}</div>
        {loadingArtists && <div className="flex items-center gap-2 text-xs text-text-muted mb-3"><LoaderCircle size={15} className="animate-spin"/> Finding artists…</div>}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-[42vh] overflow-y-auto pr-1">
          {suggestedArtists.map((item) => { const selected = artistIds.has(item.id); const image = getArtwork(item, 'large'); return <button key={`s-${item.id}`} type="button" onClick={() => toggleArtist(item)} className={`flex items-center gap-2.5 p-2 rounded-xl border text-left ${selected ? 'border-brand bg-brand/10' : 'border-white/10 bg-white/[.03]'}`}><div className="w-11 h-11 rounded-full overflow-hidden bg-white/5 grid place-items-center shrink-0">{image ? <img src={image} alt="" className="w-full h-full object-cover"/> : <Music2 size={18}/>}</div><span className="truncate text-sm font-semibold flex-1">{item.title}</span>{selected && <Check size={15} className="text-brand"/>}</button> })}
        </div>
        <div className="relative my-4"><Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued"/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search another artist" className="w-full bg-white text-black rounded-xl py-3 pl-10 pr-4 outline-none"/></div>
        {searching && <div className="text-xs text-text-muted mb-2">Searching artists…</div>}
        {artistResults.length > 0 && <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-36 overflow-y-auto">{artistResults.map((item) => { const selected = artistIds.has(item.id); const image = getArtwork(item, 'large'); return <button key={item.id} type="button" onClick={() => toggleArtist(item)} className={`flex items-center gap-2 p-2 rounded-xl border text-left ${selected ? 'border-brand bg-brand/10' : 'border-white/10 bg-white/[.03]'}`}><div className="w-9 h-9 rounded-full overflow-hidden bg-white/5 shrink-0">{image && <img src={image} alt="" className="w-full h-full object-cover"/>}</div><span className="truncate text-sm flex-1">{item.title}</span>{selected && <Check size={14} className="text-brand"/>}</button>})}</div>}
        {error && <p className="text-xs text-red-300 mt-3">{error}</p>}
        <p className="text-xs text-brand mt-4">{artists.length} artist{artists.length === 1 ? '' : 's'} selected</p>
        <div className="flex gap-2 mt-5"><button onClick={() => setStep(1)} className="sf-secondary-button flex-1"><ArrowLeft size={16}/> Back</button><button disabled={!artists.length} onClick={() => setStep(3)} className="sf-primary-button flex-1 disabled:opacity-40">Suggest songs <ArrowRight size={17}/></button></div>
      </section>}

      {step === 3 && <section>
        <p className="text-xs uppercase tracking-[.18em] text-brand font-black">Step 3 of 3</p>
        <h1 className="sf-onboarding-title text-[clamp(2rem,7vw,3.5rem)] leading-[1.05]">Songs for your taste.</h1>
        <p className="text-sm text-text-muted mb-4">These are searched from your selected artists using your chosen languages. Select the songs you want as starting recommendations.</p>
        {loadingSongs && <div className="flex items-center gap-2 text-xs text-text-muted mb-3"><LoaderCircle size={15} className="animate-spin"/> Finding songs…</div>}
        {error && <p className="text-xs text-red-300 mb-3">{error}</p>}
        <div className="space-y-2 max-h-[48vh] overflow-y-auto pr-1">{songSuggestions.map((item) => { const selected = songIds.has(item.id); return <button key={item.id} type="button" onClick={() => toggleSong(item)} className={`w-full flex items-center gap-3 p-2.5 rounded-xl border text-left ${selected ? 'border-brand bg-brand/10' : 'border-white/10 bg-white/[.03]'}`}><div className="w-12 h-12 rounded-lg overflow-hidden bg-white/5 shrink-0">{item.thumbnail ? <img src={item.thumbnail} alt="" className="w-full h-full object-cover"/> : <Music2 size={20} className="m-3"/>}</div><div className="min-w-0 flex-1"><p className="truncate text-sm font-bold">{item.title}</p><p className="truncate text-xs text-text-muted">{item.artist || item.matchedArtist}</p><p className="truncate text-[11px] text-text-subdued">From {item.matchedArtist}</p></div><span className={`w-7 h-7 rounded-full grid place-items-center shrink-0 ${selected ? 'bg-brand text-black' : 'bg-white/10 text-text-muted'}`}>{selected ? <Check size={15}/> : <Play size={13}/>}</span></button>})}</div>
        <p className="text-xs text-brand mt-4">{songs.length} song{songs.length === 1 ? '' : 's'} selected</p>
        <div className="flex gap-2 mt-5"><button onClick={() => setStep(2)} className="sf-secondary-button flex-1"><ArrowLeft size={16}/> Back</button><button onClick={() => onFinish({ languages, language: languages[0], favoriteArtists: artists, preferredSongs: songs, onboardingComplete: true })} className="sf-primary-button flex-1">Start listening <ArrowRight size={17}/></button></div>
      </section>}
      {onCancel && <button onClick={onCancel} className="w-full text-xs text-text-subdued mt-3">Cancel</button>}
    </div>
  </div>
}
