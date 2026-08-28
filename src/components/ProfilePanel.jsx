import { useState, useRef, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { LogOut, Trash2, X, AlertTriangle, Settings as SettingsIcon, User } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { deleteCurrentUserAccount } from '@/lib/firebase'
import { deleteAllUserData } from '@/lib/library'
import { GoogleAuthProvider, reauthenticateWithPopup } from 'firebase/auth'
import { auth } from '@/lib/firebase'

export default function ProfilePanel({ onClose }) {
  const { user, signOut } = useAuth()
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState(null)
  const panelRef = useRef(null)

  useEffect(() => {
    function onClickOutside(e) {
      if (panelRef.current && !panelRef.current.contains(e.target)) onClose()
    }
    function onKeyDown(e) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('mousedown', onClickOutside)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onClickOutside)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [onClose])

  async function handleDeleteAccount() {
    setDeleting(true)
    setError(null)
    try {
      await deleteAllUserData(user.uid)
      await deleteCurrentUserAccount()
      onClose()
    } catch (err) {
      if (err.code === 'auth/requires-recent-login') {
        try {
          await reauthenticateWithPopup(auth.currentUser, new GoogleAuthProvider())
          await deleteAllUserData(user.uid)
          await deleteCurrentUserAccount()
          onClose()
        } catch {
          setError('Could not verify your identity. Please try again.')
        }
      } else {
        setError('Something went wrong deleting your account. Please try again.')
      }
    } finally {
      setDeleting(false)
    }
  }

  const content = (
    <>
      <div className="fixed inset-0 z-[2000] bg-black/70 md:hidden" aria-hidden="true" onClick={onClose} />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Account menu"
        className="fixed inset-y-0 right-0 z-[2001] w-[min(88vw,380px)] bg-surface-elevated shadow-panel overflow-y-auto overscroll-contain md:absolute md:inset-y-auto md:right-0 md:top-full md:mt-2 md:w-80 md:max-w-none md:rounded-lg md:border md:border-border"
      >
        <div className="flex items-center justify-between p-4 border-b border-border">
          <p className="font-bold text-sm">Account</p>
          <button onClick={onClose} aria-label="Close account menu" className="text-text-muted hover:text-text"><X size={18} /></button>
        </div>
        <div className="p-4 flex items-center gap-3 border-b border-border">
          {user.photoURL ? <img src={user.photoURL} alt="" className="w-14 h-14 rounded-full" /> : <div className="w-14 h-14 rounded-full bg-brand flex items-center justify-center"><User size={24} className="text-black" /></div>}
          <div className="min-w-0"><p className="font-semibold truncate">{user.displayName || 'Spotifusion user'}</p><p className="text-xs text-text-subdued truncate">{user.email}</p></div>
        </div>
        <div className="p-2">
          <Link to="/settings" onClick={onClose} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md hover:bg-surface-hover text-sm text-left"><SettingsIcon size={18} className="text-text-muted" />Settings and privacy</Link>
          <button onClick={signOut} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md hover:bg-surface-hover text-sm text-left"><LogOut size={18} className="text-text-muted" />Log out</button>
          {!confirmingDelete ? <button onClick={() => setConfirmingDelete(true)} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-md hover:bg-surface-hover text-sm text-left text-red-400"><Trash2 size={18} />Delete account</button> : <div className="m-2 p-3 rounded-md bg-red-950/40 border border-red-900/60"><div className="flex gap-2 mb-2"><AlertTriangle size={16} className="text-red-400 shrink-0 mt-0.5" /><p className="text-xs text-red-200">This permanently deletes your account, playlists, liked songs, and history. This can't be undone.</p></div>{error && <p className="text-xs text-red-400 mb-2">{error}</p>}<div className="flex gap-2"><button onClick={handleDeleteAccount} disabled={deleting} className="flex-1 bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white text-xs font-bold py-2 rounded">{deleting ? 'Deleting…' : 'Yes, delete everything'}</button><button onClick={() => setConfirmingDelete(false)} disabled={deleting} className="flex-1 bg-surface-hover text-xs font-bold py-2 rounded">Cancel</button></div></div>}
        </div>
      </div>
    </>
  )
  return typeof document !== 'undefined' ? createPortal(content, document.body) : content
}
