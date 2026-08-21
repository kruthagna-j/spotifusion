import { useState, useRef, useEffect } from 'react'
import { LogOut, Trash2, X, AlertTriangle, KeyRound, ExternalLink } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { deleteCurrentUserAccount } from '@/lib/firebase'
import { deleteAllUserData, setYoutubeApiKey } from '@/lib/library'
import { useUserSettings } from '@/hooks/useLibraryData'
import { GoogleAuthProvider, reauthenticateWithPopup } from 'firebase/auth'
import { auth } from '@/lib/firebase'

export default function ProfilePanel({ onClose }) {
  const { user, signOut } = useAuth()
  const { youtubeApiKey } = useUserSettings(user.uid)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState(null)
  const [keyDraft, setKeyDraft] = useState(youtubeApiKey || '')
  const [keySaved, setKeySaved] = useState(false)
  const panelRef = useRef(null)

  useEffect(() => {
    setKeyDraft(youtubeApiKey || '')
  }, [youtubeApiKey])

  useEffect(() => {
    function onClickOutside(e) {
      if (panelRef.current && !panelRef.current.contains(e.target)) onClose()
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [onClose])

  async function handleDeleteAccount() {
    setDeleting(true)
    setError(null)
    try {
      await deleteAllUserData(user.uid)
      await deleteCurrentUserAccount()
      onClose()
    } catch (err) {
      // Firebase requires a *recent* sign-in before allowing account deletion.
      // If it's been a while since login, re-auth once and retry.
      if (err.code === 'auth/requires-recent-login') {
        try {
          await reauthenticateWithPopup(auth.currentUser, new GoogleAuthProvider())
          await deleteAllUserData(user.uid)
          await deleteCurrentUserAccount()
          onClose()
        } catch (err2) {
          setError('Could not verify your identity. Please try again.')
        }
      } else {
        setError('Something went wrong deleting your account. Please try again.')
      }
    } finally {
      setDeleting(false)
    }
  }

  async function handleSaveKey() {
    await setYoutubeApiKey(user.uid, keyDraft.trim())
    setKeySaved(true)
    setTimeout(() => setKeySaved(false), 2000)
  }

  return (
    <div className="absolute right-0 top-full mt-2 w-80 bg-surface-elevated rounded-lg shadow-2xl border border-border z-40 overflow-hidden" ref={panelRef}>
      <div className="flex items-center justify-between p-4 border-b border-border">
        <p className="font-bold text-sm">Account</p>
        <button onClick={onClose} className="text-text-muted hover:text-text">
          <X size={18} />
        </button>
      </div>

      <div className="p-4 flex items-center gap-3 border-b border-border">
        {user.photoURL ? (
          <img src={user.photoURL} alt="" className="w-14 h-14 rounded-full" />
        ) : (
          <div className="w-14 h-14 rounded-full bg-brand" />
        )}
        <div className="min-w-0">
          <p className="font-semibold truncate">{user.displayName}</p>
          <p className="text-xs text-text-subdued truncate">{user.email}</p>
        </div>
      </div>

      <div className="p-4 border-b border-border">
        <div className="flex items-center gap-2 mb-2">
          <KeyRound size={14} className="text-text-muted" />
          <p className="text-xs font-bold uppercase tracking-wide text-text-muted">Your YouTube API key</p>
        </div>
        <p className="text-xs text-text-subdued mb-2">
          Optional. Use your own free key so search doesn't share a quota with other users.
        </p>
        <input
          type="password"
          value={keyDraft}
          onChange={(e) => setKeyDraft(e.target.value)}
          placeholder="Paste API key here"
          className="w-full bg-surface-highlight rounded px-3 py-2 text-xs outline-none mb-2"
        />
        <div className="flex items-center gap-2">
          <button
            onClick={handleSaveKey}
            className="bg-brand text-black text-xs font-bold px-3 py-1.5 rounded-full"
          >
            {keySaved ? 'Saved ✓' : 'Save'}
          </button>
          <a
            href="https://console.cloud.google.com/apis/library/youtube.googleapis.com"
            target="_blank"
            rel="noreferrer"
            className="text-xs text-text-muted hover:text-text flex items-center gap-1"
          >
            Get a free key <ExternalLink size={12} />
          </a>
        </div>
      </div>

      <div className="p-2">
        <button
          onClick={signOut}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md hover:bg-surface-hover text-sm text-left"
        >
          <LogOut size={18} className="text-text-muted" />
          Log out
        </button>

        {!confirmingDelete ? (
          <button
            onClick={() => setConfirmingDelete(true)}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md hover:bg-surface-hover text-sm text-left text-red-400"
          >
            <Trash2 size={18} />
            Delete account
          </button>
        ) : (
          <div className="m-2 p-3 rounded-md bg-red-950/40 border border-red-900/60">
            <div className="flex gap-2 mb-2">
              <AlertTriangle size={16} className="text-red-400 shrink-0 mt-0.5" />
              <p className="text-xs text-red-200">
                This permanently deletes your account, playlists, liked songs, and history. This
                can't be undone.
              </p>
            </div>
            {error && <p className="text-xs text-red-400 mb-2">{error}</p>}
            <div className="flex gap-2">
              <button
                onClick={handleDeleteAccount}
                disabled={deleting}
                className="flex-1 bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white text-xs font-bold py-2 rounded"
              >
                {deleting ? 'Deleting…' : 'Yes, delete everything'}
              </button>
              <button
                onClick={() => setConfirmingDelete(false)}
                disabled={deleting}
                className="flex-1 bg-surface-hover text-xs font-bold py-2 rounded"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
