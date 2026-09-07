import { createContext, useContext, useState, useEffect } from 'react'

const AuxPaneContext = createContext(null)
const LS_KEY = 'spotifusion:aux-pane-open'

export function AuxPaneProvider({ children }) {
  const [auxOpen, setAuxOpen] = useState(() => {
    try { return localStorage.getItem(LS_KEY) !== 'false' } catch { return true }
  })

  useEffect(() => {
    try { localStorage.setItem(LS_KEY, String(auxOpen)) } catch {}
  }, [auxOpen])

  return (
    <AuxPaneContext.Provider value={{ auxOpen, toggleAux: () => setAuxOpen(v => !v), openAux: () => setAuxOpen(true), closeAux: () => setAuxOpen(false) }}>
      {children}
    </AuxPaneContext.Provider>
  )
}

export function useAuxPane() {
  const ctx = useContext(AuxPaneContext)
  if (!ctx) throw new Error('useAuxPane must be used within AuxPaneProvider')
  return ctx
}
