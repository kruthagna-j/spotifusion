import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Check, Search, ArrowRight, ArrowLeft, LoaderCircle, Music2 } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { saveUserPreferences } from '@/lib/firebase'
import { searchMusicPage } from '@/lib/search'
import { getArtwork } from '@/lib/artwork'

const LANGUAGES = ['English', 'Telugu', 'Hindi', 'Tamil', 'Kannada', 'Malayalam', 'Marathi', 'Bengali', 'Punjabi', 'Gujarati', 'Odia']

export default function Onboarding() {
  const { user, profile, setProfile } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const editMode = params.get('source') === 'local'
  const initialLanguages = profile?.languages?.length ? profile.languages : (profile?.language ? [profile.language] : ['English'])
  const [step, setStep] = useState(1)
  const [languages, setLanguages] = useState(initialLanguages)
  const [artists, setArtists] = useState(profile?.favoriteArtists || [])
  const [query, setQuery] = useState('')
  const [artistResults, setArtistResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) navigate('/login', { replace: true })
  }, [user, navigate])

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

  function toggleLanguage(item) {
    setLanguages((prev) => prev.includes(item) ? prev.filter((x) => x !== item) : [...prev, item])
  }

  function toggleArtist(item) {
    setArtists((prev) => selectedIds.has(item.id)
      ? prev.filter((x) => x.id !== item.id)
      : [...prev, { id: item.id, name: item.title, image: getArtwork(item, 'large') }])
  }

  async function finish() {
    if (!user || !languages.length) return
    setSaving(true); setError('')
    try {
      const preferences = {
        languages,
        language: languages[0],
        favoriteArtists: artists,
        onboardingComplete: true,
      }
      await saveUserPreferences(user.uid, preferences)
      setProfile((prev) => ({ ...(prev || {}), ...preferences }))
      navigate(editMode ? '/settings' : '/', { replace: true })
    } catch (err) { setError(err?.message || 'Could not save your preferences.') }
    finally { setSaving(false) }
  }

  if (!user) return null

  return <div className="sf-onboarding-page px-3 py-4 sm:px-6">
    <div className="sf-onboarding-card w-full max-w-2xl max-h-[calc(100vh-2rem)] overflow-y-auto">
      <div className="flex items-center gap-2 text-brand font-black"><Music2 size={22}/> Spotifusion</div>
      <div className="sf-onboarding-progress"><span style={{ width: `${(step / 2) * 100}%` }}/></div>

      {step === 1 && <>
        <p className="text-xs uppercase tracking-[.18em] text-brand font-black">Step 1 of 2</p>
        <h1 className="sf-onboarding-title text-[clamp(2rem,7vw,3.5rem)] leading-[1.05]">Which languages do you listen to?</h1>
        <p className="text-sm text-text-muted mb-5">Choose as many as you want. These preferences help Spotifusion personalize music for you.</p>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
          {LANGUAGES.map((item) => {
            const selected = languages.includes(item)
            return <button key={item} type="button" onClick={() => toggleLanguage(item)} aria-pressed={selected}
              className={`min-h-12 px-3 py-2.5 rounded-xl border text-left text-sm font-bold transition ${selected ? 'border-brand bg-brand/10 text-brand' : 'border-white/10 bg-white/[.03] text-text hover:bg-white/[.07]'}`}>
              <span className="flex items-center justify-between gap-2"><span>{item}</span>{selected && <Check size={17}/>}</span>
            </button>
          })}
        </div>
        <p className="text-xs text-brand font-semibold mt-4">{languages.length} language{languages.length === 1 ? '' : 's'} selected</p>
        <button disabled={!languages.length} onClick={() => setStep(2)} className="sf-primary-button w-full mt-5 disabled:opacity-40">Continue <ArrowRight size={17}/></button>
      </>}

      {step === 2 && <>
        <p className="text-xs uppercase tracking-[.18em] text-brand font-black">Step 2 of 2</p>
        <h1 className="sf-onboarding-title text-[clamp(2rem,7vw,3.5rem)] leading-[1.05]">Pick your favorite artists.</h1>
        <p className="text-sm text-text-muted mb-5">Pick as many as you like. You can change these anytime.</p>
        <div className="relative mb-4"><Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-subdued"/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search artists" className="w-full bg-white text-black rounded-xl py-3 pl-10 pr-4 outline-none"/></div>
        {searching && <div className="flex items-center gap-2 text-xs text-text-muted mb-3"><LoaderCircle size={15} className="animate-spin"/> Finding artists…</div>}
        {error && <p className="text-xs text-red-300 mb-3">{error}</p>}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-[42vh] overflow-y-auto pr-1">
          {artistResults.map((item) => {
            const selected = selectedIds.has(item.id); const image = getArtwork(item, 'large')
            return <button key={item.id} type="button" onClick={() => toggleArtist(item)} aria-pressed={selected} className={`flex items-center gap-2.5 p-2 rounded-xl border text-left ${selected ? 'border-brand bg-brand/10' : 'border-white/10 bg-white/[.03]'}`}>
              <div className="w-10 h-10 rounded-full overflow-hidden bg-white/5 grid place-items-center shrink-0">{image ? <img src={image} alt="" className="w-full h-full object-cover"/> : <Music2 size={18}/>}</div>
              <span className="truncate text-sm font-semibold flex-1">{item.title}</span>{selected && <Check size={15} className="text-brand shrink-0"/>}
            </button>
          })}
        </div>
        {artists.length > 0 && <p className="text-xs text-brand mt-4">{artists.length} artist{artists.length === 1 ? '' : 's'} selected</p>}
        <div className="flex gap-2 mt-5"><button onClick={() => setStep(1)} className="sf-secondary-button flex-1"><ArrowLeft size={16}/> Back</button><button onClick={finish} disabled={saving} className="sf-primary-button flex-1">{saving ? 'Saving…' : editMode ? 'Save preferences' : 'Start listening'} <ArrowRight size={17}/></button></div>
      </>}
    </div>
  </div>
}
