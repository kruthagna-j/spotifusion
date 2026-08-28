import { useEffect, useState } from 'react'
import { LogOut, Music2, RefreshCw } from 'lucide-react'
import {
  clearSpotifyToken,
  getSpotifyDevices,
  getSpotifyProfile,
  getSpotifyToken,
  spotifyConfigured,
  startSpotifyAuth,
  transferSpotifyPlayback,
} from '@/lib/spotifyConnect'

export default function SpotifyConnectSection() {
  const [connected, setConnected] = useState(Boolean(getSpotifyToken()))
  const [profile, setProfile] = useState(null)
  const [devices, setDevices] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function load() {
    if (!getSpotifyToken()) { setConnected(false); setProfile(null); setDevices([]); return }
    setLoading(true); setError('')
    try {
      const [me, deviceData] = await Promise.all([getSpotifyProfile(), getSpotifyDevices()])
      setProfile(me); setDevices(deviceData.devices || []); setConnected(true)
    } catch (e) {
      setError(e?.message || 'Unable to load Spotify Connect.')
      setConnected(Boolean(getSpotifyToken()))
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function connect() {
    setError('')
    try { await startSpotifyAuth() }
    catch (e) { setError(e?.message || 'Unable to start Spotify connection.') }
  }

  function disconnect() {
    clearSpotifyToken(); setConnected(false); setProfile(null); setDevices([]); setError('')
  }

  async function selectDevice(id) {
    setError('')
    try { await transferSpotifyPlayback(id, false); await load() }
    catch (e) { setError(e?.message || 'Could not transfer playback.') }
  }

  if (!spotifyConfigured()) {
    return <div className="sf-info-box">
      <Music2 size={17}/>
      <div className="min-w-0"><p className="font-semibold">Spotify Connect is ready to configure</p><p className="mt-1">Add <code>VITE_SPOTIFY_CLIENT_ID</code> in Vercel, and register <code>{window.location.origin}/spotify-callback</code> as a Spotify Redirect URI. Then this section will let users connect Spotify and choose their active Connect device.</p></div>
    </div>
  }

  return <div className="space-y-4">
    {!connected ? <>
      <div className="sf-info-box"><Music2 size={17}/><span>Connect Spotify to see your Spotify Connect devices and transfer playback from Spotifusion.</span></div>
      <button type="button" onClick={connect} className="sf-primary-button w-full">Connect Spotify</button>
    </> : <>
      <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/[.03] p-3">
        {profile?.images?.[0]?.url ? <img src={profile.images[0].url} alt="" className="w-11 h-11 rounded-full object-cover"/> : <div className="w-11 h-11 rounded-full bg-brand/15 grid place-items-center"><Music2 size={19} className="text-brand"/></div>}
        <div className="min-w-0 flex-1"><p className="font-bold truncate">{profile?.display_name || 'Spotify account'}</p><p className="text-xs text-text-subdued truncate">Spotify connected</p></div>
        <button type="button" onClick={disconnect} className="sf-outline-button" title="Disconnect Spotify"><LogOut size={15}/> Disconnect</button>
      </div>
      <div className="flex items-center justify-between gap-3"><div><p className="text-sm font-semibold">Spotify Connect devices</p><p className="text-xs text-text-subdued mt-1">Choose where Spotify playback should continue.</p></div><button type="button" onClick={load} disabled={loading} className="sf-outline-button"><RefreshCw size={15} className={loading ? 'animate-spin' : ''}/> Refresh</button></div>
      {devices.length ? <div className="space-y-2">{devices.map((device) => <button key={device.id} type="button" onClick={() => selectDevice(device.id)} className={`w-full flex items-center gap-3 rounded-xl border p-3 text-left transition ${device.is_active ? 'border-brand bg-brand/10' : 'border-white/10 bg-white/[.03] hover:bg-white/[.06]'}`}><Music2 size={18} className={device.is_active ? 'text-brand' : 'text-text-subdued'}/><span className="min-w-0 flex-1"><strong className="block truncate text-sm">{device.name}</strong><small className="text-text-subdued">{device.type}{device.is_active ? ' · Active' : ''}</small></span>{device.supports_volume && <span className="text-[11px] text-text-subdued">{device.volume_percent ?? 0}%</span>}</button>)}</div> : <p className="text-sm text-text-muted py-2">No Spotify Connect devices are currently available. Open Spotify on a phone, computer, speaker or other supported device and refresh.</p>}
    </>}
    {error && <p className="text-xs text-red-300 leading-5">{error}</p>}
    <p className="text-[11px] text-text-subdued leading-5">Spotify playback control requires a Spotify Premium account. Spotifusion uses Spotify's official OAuth and Web API Connect controls; it does not copy Spotify audio into Spotifusion.</p>
  </div>
}
