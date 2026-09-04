import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import './liquid-glass.css'
import './liquid-glass-consistency.css'
import './reference-ui.css'
import './reference-ui-extra.css'
import './responsive-now-playing.css'
import './reference-screens.css'
import './spotifusion-redesign.css'
import App from './App.jsx'
import { AuthProvider } from '@/context/AuthContext'
import { PlayerProvider } from '@/context/PlayerContext'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <PlayerProvider>
          <App />
        </PlayerProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
