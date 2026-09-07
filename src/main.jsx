import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from '@/context/AuthContext'
import { PlayerProvider } from '@/context/PlayerContext'
import { AuxPaneProvider } from '@/context/AuxPaneContext'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <PlayerProvider>
          <AuxPaneProvider>
            <App />
          </AuxPaneProvider>
        </PlayerProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
