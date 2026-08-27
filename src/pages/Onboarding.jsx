import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Check, Search, ArrowRight, ArrowLeft, LoaderCircle, Music2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { getUserPreferences, saveUserPreferences } from '@/lib/firebase'
import { searchMusicPage } from '@/lib/search'
import { getArtwork } from '@/lib/artwork'

const LANGUAGES = ['English', 'Telugu', 'Hindi', 'Tamil', 'Kannada', 'Malayalam']

export default function Onboarding() {
  const { user, profile, setProfile } = useAuth()
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [language, setLanguage] = useState(profile?.language || 'English')
  const [artists, setArtists] = useState(profile?.favoriteArtists || [])
  const [query, setQuery] = useState('')
  const [artistResults, setArtistResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) navigate('/login', { replace: true })
    else if (profile?.onboardingComplete) navigate('/', { replace: true })
  }, [user, profile, navigate])

  useEffect(() => {
    if (!query.trim() || query.trim().length < 2) { setArtistResults([]); return undefined }
    const timer = setTimeout(async () => {
      setSearching(true); setError('')
      try {
        const page = await searchMusicPage(query, { category: 'artists', batch: 1 })
        setArtistResults((page.results || []).filter((x) => x.resultType === 'artist').slice(0, 12))
      } catch (err) { setError(err?.message || 'Artist search failed.') }
      finally { setSearching(false) }
    }, 350)
    return () => clearTimeout(timer)
  }, [query])

  const selectedIds = useMemo(() => new Set(artists.map((x) => x.id)), [artists])

  function toggleArtist(item) {
    setArtists((prev) => selectedIds.has(item.id) ? prev.filter((x) => x.id !== item.id) : [...prev, { id: item.id, name: item.title, image: getArtwork(item, 'large') }])
  }

  async function finish() {
    if (!user) return
    setSaving(true); setError('')
    try {
      const preferences = { language, favoriteArtists: artists, onboardingComplete: true }
      await saveUserPreferences(user.uid, preferences)
      setProfile((prev) => ({ ...(prev || {}), ...preferences }))
      navigate('/', { replace: true })
    } catch (err) { setError(err?.message || 'Could not save your preferences.') }
    finally { setSaving(false) }
  }

  if (!user) return null

  return <div className="sf-onboarding-page">
    <div className="sf-onboarding-card">
      <div className="flex items-center gap-2 text-brand font-black"><Music2 size={22}/> Spotifusion</div>
      <div className="sf-onboarding-progress"><span style={{ width: `${(step / 3) * 100}%` }}/></div>
      {step === 1 && <>
        <p className="text-xs uppercase tracking-[.22em] text-brand font-black">Step 1 of 3</p>
        <h1 className="sf-onboarding-title">What language should Spotifusion use?</h1>
        <p className="text-sm text-text-muted mb-6">You can change this later in Settings.</p>
        <div className="sf-language-grid">{LANGUAGES.map((item) => <button key={item} onClick={() => setLanguage(item)} className={`sf-language-option ${language === item ? 'is-selected' : ''}`}>{item}{language === item && <Check size={18}/>}</button>)}</div>
        <button onClick={() => setStep(2)} className="sf-primary-button w-full mt-7">Continue <ArrowRight size={17}/></button>
      </>}
      {step === 2 && <>
        <p className="text-xs uppercase tracking-[.22em] text-brand font-black">Step 2 of 3</p>
        <h1 className="sf-onboarding-title">Pick your favorite artists.</h1>
        <p className="text-sm text-text-muted mb-5">We'll use them to personalize your Home page. Pick as many as you like.</p>
        <div className="relative mb-4"><Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued"/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search artists" className="w-full bg-white text-black rounded-xl py-3 pl-10 pr-4 outline-none"/></div>
        {searching && <div className="flex items-center gap-2 text-xs text-text-muted mb-3"><LoaderCircle size={15} className="animate-spin"/> Finding artists…</div>}
        {error && <p className="text-xs text-red-300 mb-3">{error}</p>}
        <div className="sf-artist-picker">{artistResults.map((item) => { const selected = selectedIds.has(item.id); const image = getArtwork(item, 'large'); return <button key={item.id} onClick={() => toggleArtist(item)} className={`sf-artist-option ${selected ? 'is-selected' : ''}`}><div className="sf-artist-image">{image ? <img src={image} alt=""/> : <Music2 size={20}/>}</div><span className="truncate">{item.title}</span>{selected && <span className="sf-artist-check"><Check size={14}/></span>}</button> })}</div>
        {artists.length > 0 && <p className="text-xs text-brand mt-4">{artists.length} artist{artists.length === 1 ? '' : 's'} selected</p>}
        <div className="flex gap-2 mt-6"><button onClick={() => setStep(1)} className="sf-secondary-button flex-1"><ArrowLeft size={16}/> Back</button><button onClick={() => setStep(3)} className="sf-primary-button flex-1">Continue <ArrowRight size={17}/></button></div>
      </>}
      {step === 3 && <>
        <p className="text-xs uppercase tracking-[.22em] text-brand font-black">Step 3 of 3</p>
        <h1 className="sf-onboarding-title">You're ready to listen.</h1>
        <p className="text-sm text-text-muted mb-6">Spotifusion will use these preferences to shape your music experience.</p>
        <div className="sf-summary"><div><span>Language</span><strong>{language}</strong></div><div><span>Favorite artists</span><strong>{artists.length ? artists.map((x) => x.name).join(', ') : 'Not selected'}</strong></div></div>
        {error && <p className="text-xs text-red-300 mt-4">{error}</p>}
        <div className="flex gap-2 mt-6"><button onClick={() => setStep(2)} disabled={saving} className="sf-secondary-button flex-1"><ArrowLeft size={16}/> Back</button><button onClick={finish} disabled={saving} className="sf-primary-button flex-1">{saving ? 'Saving…' : 'Start listening'} <ArrowRight size={17}/></button></div>
      </>}
    </div>
  </div>
}
