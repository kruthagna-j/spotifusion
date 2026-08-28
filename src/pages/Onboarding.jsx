import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { saveUserPreferences } from '@/lib/firebase'
import PreferenceWizard from '@/components/PreferenceWizard'

export default function Onboarding() {
  const { user, profile, setProfile } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const editMode = params.get('source') === 'local'

  useEffect(() => {
    if (!user) navigate('/login', { replace: true })
  }, [user, navigate])

  if (!user) return null

  async function finish(preferences) {
    await saveUserPreferences(user.uid, preferences)
    setProfile((prev) => ({ ...(prev || {}), ...preferences }))
    navigate(editMode ? '/settings' : '/', { replace: true })
  }

  return <PreferenceWizard
    initialLanguages={profile?.languages?.length ? profile.languages : (profile?.language ? [profile.language] : [])}
    initialArtists={profile?.favoriteArtists || []}
    initialSongs={profile?.preferredSongs || []}
    onFinish={finish}
    onCancel={editMode ? () => navigate('/settings') : undefined}
  />
}
