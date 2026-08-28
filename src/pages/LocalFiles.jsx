import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { SlidersHorizontal } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import LocalFilesSection from '@/components/LocalFilesSection'

const LOCAL_PERSONALIZATION_KEY = 'spotifusion:local-personalization:v1'

export default function LocalFiles() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [personalize, setPersonalize] = useState(() => {
    try { return localStorage.getItem(LOCAL_PERSONALIZATION_KEY) === 'true' } catch { return false }
  })

  useEffect(() => {
    try { localStorage.setItem(LOCAL_PERSONALIZATION_KEY, String(personalize)) } catch {}
  }, [personalize])

  function handlePersonalizationToggle(next) {
    if (!next) {
      setPersonalize(false)
      return
    }
    if (!user) {
      navigate('/login', { state: { returnTo: '/local-files' } })
      return
    }
    setPersonalize(true)
    navigate('/onboarding?source=local')
  }

  return (
    <div className="p-4 md:p-6 pb-40 max-w-5xl mx-auto">
      <div className="flex items-start justify-between gap-4 mb-5">
        <div>
          <h1 className="text-2xl font-bold">Local Music</h1>
          <p className="text-sm text-text-muted mt-1">Play and manage music stored on this device.</p>
        </div>
      </div>

      <div className="mb-6 rounded-2xl border border-white/10 bg-surface-elevated/70 p-4 md:p-5">
        <div className="flex items-center gap-3">
          <span className="w-10 h-10 rounded-xl bg-brand/10 text-brand grid place-items-center shrink-0"><SlidersHorizontal size={19}/></span>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-bold">Personalize local music</p>
            <p className="text-xs text-text-subdued mt-1 leading-5">Turn this on to choose multiple languages and your favorite artists. Spotifusion will use them for local-music recommendations.</p>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={personalize}
            aria-label="Personalize local music"
            onClick={() => handlePersonalizationToggle(!personalize)}
            className={`relative w-12 h-7 rounded-full shrink-0 transition ${personalize ? 'bg-brand' : 'bg-white/15'}`}
          >
            <span className={`absolute top-1 w-5 h-5 rounded-full bg-white shadow transition-transform ${personalize ? 'translate-x-6' : 'translate-x-1'}`} />
          </button>
        </div>
        {personalize && <p className="text-[11px] text-brand font-semibold mt-3">Personalization is on. Your selected languages and artists can be changed by tapping the switch again.</p>}
      </div>

      <LocalFilesSection />
    </div>
  )
}
