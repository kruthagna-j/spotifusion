import { useEffect, useMemo, useState } from 'react'
import {
  User, LogOut, Bell, BellOff, EyeOff, Sliders, HardDrive, Trash2, Info,
  ChevronRight, ChevronDown, Volume2, Repeat2, Shuffle, Timer, Mic2,
  Palette, Shield, Gauge, Database, CircleHelp, RotateCcw
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlayer, EQ_BANDS } from '@/context/PlayerContext'
import { useLocalStorageStats, clearLocalSongs } from '@/lib/localMusicDb'
import { isPrivateSession, setPrivateSession } from '@/lib/privacySettings'
import LocalFilesSection from '@/components/LocalFilesSection'

const SETTINGS_KEY = 'spotifusion:settings:v3'
const defaults = {
  lyricsAuto: true,
  lyricsAutoScroll: true,
  lyricsTranslation: false,
  reduceMotion: false,
  compactLayout: false,
  dataSaver: false,
  personalizedRecommendations: true,
  newReleaseNotifications: true,
}

function loadSettings() {
  try { return { ...defaults, ...JSON.parse(localStorage.getItem(SETTINGS_KEY) || '{}') } }
  catch { return defaults }
}
function saveSettings(value) { try { localStorage.setItem(SETTINGS_KEY, JSON.stringify(value)) } catch {} }
function formatBytes(bytes) {
  if (!bytes) return '0 MB'
  const mb = bytes / (1024 * 1024)
  return mb < 1024 ? `${mb.toFixed(1)} MB` : `${(mb / 1024).toFixed(2)} GB`
}

function Toggle({ checked, onChange, label, disabled = false }) {
  return <button
    type="button" role="switch" aria-checked={checked} aria-label={label} disabled={disabled}
    onClick={() => onChange(!checked)}
    className={`sf-setting-toggle ${checked ? 'is-on' : ''} ${disabled ? 'opacity-40 cursor-not-allowed' : ''}`}
  ><span /></button>
}

function Section({ icon: Icon, title, description, open, onToggle, children }) {
  return <section className="sf-settings-section">
    <button type="button" onClick={onToggle} className="sf-settings-heading" aria-expanded={open}>
      <span className="sf-settings-heading-icon"><Icon size={18}/></span>
      <span className="min-w-0 flex-1 text-left"><strong>{title}</strong>{description && <small>{description}</small>}</span>
      {open ? <ChevronDown size={18}/> : <ChevronRight size={18}/>} 
    </button>
    {open && <div className="sf-settings-content">{children}</div>}
  </section>
}

function Row({ icon: Icon, title, description, control, children }) {
  return <div className="sf-setting-row">
    <div className="flex items-start gap-3 min-w-0 flex-1">
      {Icon && <span className="sf-setting-row-icon"><Icon size={17}/></span>}
      <div className="min-w-0"><p className="text-sm font-semibold">{title}</p>{description && <p className="text-xs text-text-subdued mt-1 leading-5">{description}</p>}</div>
    </div>
    {control || children}
  </div>
}

function Choice({ active, children, onClick }) {
  return <button type="button" onClick={onClick} className={`sf-setting-choice ${active ? 'is-active' : ''}`}>{children}</button>
}

export default function Settings() {
  const { user, signOut } = useAuth()
  const player = usePlayer()
  const [stats, refreshStats] = useLocalStorageStats()
  const [confirmingClear, setConfirmingClear] = useState(false)
  const [privateSession, setPrivateSessionState] = useState(isPrivateSession())
  const [notifPermission, setNotifPermission] = useState(typeof Notification !== 'undefined' ? Notification.permission : 'unsupported')
  const [settings, setSettings] = useState(loadSettings)
  const [open, setOpen] = useState({ account: true, playback: true, audio: false, lyrics: false, appearance: false, notifications: false, privacy: false, storage: false, performance: false, about: false })

  useEffect(() => saveSettings(settings), [settings])
  useEffect(() => {
    document.documentElement.dataset.reducedMotion = settings.reduceMotion ? 'true' : 'false'
    document.documentElement.dataset.compact = settings.compactLayout ? 'true' : 'false'
  }, [settings.reduceMotion, settings.compactLayout])

  const setSetting = (key, value) => setSettings((prev) => ({ ...prev, [key]: value }))
  const toggle = (key) => setSetting(key, !settings[key])
  const flip = (key) => setOpen((prev) => ({ ...prev, [key]: !prev[key] }))

  async function requestNotifications() {
    if (typeof Notification === 'undefined') return
    setNotifPermission(await Notification.requestPermission())
  }
  async function handleClearAll() {
    await clearLocalSongs(); refreshStats(); setConfirmingClear(false)
  }
  function resetPreferences() {
    setSettings(defaults)
    setPrivateSession(false)
    setPrivateSessionState(false)
  }

  const repeatLabel = player.repeatMode === 'one' ? 'Repeat one' : player.repeatMode === 'all' ? 'Repeat all' : 'Repeat off'
  const playbackSummary = useMemo(() => `${player.volume}% volume · ${player.shuffle ? 'Shuffle on' : 'Shuffle off'} · ${repeatLabel}`, [player.volume, player.shuffle, repeatLabel])

  return <div className="p-4 md:p-7 max-w-4xl mx-auto pb-40">
    <div className="mb-6 md:mb-8">
      <p className="text-xs text-brand uppercase tracking-[.2em] font-black">Preferences</p>
      <h1 className="text-3xl md:text-4xl font-black mt-2">Settings</h1>
      <p className="text-sm text-text-muted mt-2">Control playback, lyrics, privacy, storage and the way Spotifusion behaves on this device.</p>
    </div>

    <div className="space-y-3">
      <Section icon={User} title="Account" description={user ? (user.email || 'Signed in') : 'Not signed in'} open={open.account} onToggle={() => flip('account')}>
        {user ? <>
          <div className="sf-account-card">
            {user.photoURL ? <img src={user.photoURL} alt="" /> : <div className="sf-account-avatar"><User size={22}/></div>}
            <div className="min-w-0 flex-1"><p className="font-bold truncate">{user.displayName || 'Spotifusion user'}</p><p className="text-xs text-text-subdued truncate mt-1">{user.email}</p></div>
            <button type="button" onClick={signOut} className="sf-outline-button"><LogOut size={15}/> Sign out</button>
          </div>
          <Row icon={User} title="Google account" description="Authentication is managed by your existing Firebase project."><span className="text-xs text-text-subdued">Connected</span></Row>
        </> : <p className="text-sm text-text-muted">Sign in to manage your account and online music features.</p>}
      </Section>

      <Section icon={Sliders} title="Playback" description={playbackSummary} open={open.playback} onToggle={() => flip('playback')}>
        <Row icon={Shuffle} title="Shuffle" description="Randomize the current queue without repeatedly selecting the same track." control={<Toggle checked={player.shuffle} onChange={() => player.toggleShuffle()} label="Shuffle"/>}/>
        <Row icon={Repeat2} title="Repeat mode" description="Choose how playback behaves when the queue reaches its end.">
          <div className="flex gap-1.5 flex-wrap justify-end"><Choice active={player.repeatMode === 'off'} onClick={() => player.setRepeatMode('off')}>Off</Choice><Choice active={player.repeatMode === 'all'} onClick={() => player.setRepeatMode('all')}>All</Choice><Choice active={player.repeatMode === 'one'} onClick={() => player.setRepeatMode('one')}>One</Choice></div>
        </Row>
        <Row icon={Timer} title="Sleep timer" description={player.sleepTimerSeconds ? `Playback stops in ${Math.ceil(player.sleepTimerSeconds / 60)} minutes.` : 'Automatically stop playback after a set time.'}>
          <div className="flex gap-1.5 flex-wrap justify-end">{[5,15,30,60].map((m) => <Choice key={m} active={Math.ceil((player.sleepTimerSeconds || 0) / 60) === m} onClick={() => player.setSleepTimer(m * 60)}>{m}m</Choice>)}{player.sleepTimerSeconds && <Choice active={false} onClick={player.clearSleepTimer}>Clear</Choice>}</div>
        </Row>
      </Section>

      <Section icon={Volume2} title="Audio" description={`${player.volume}% volume${player.eqSupported ? ' · Equalizer available' : ''}`} open={open.audio} onToggle={() => flip('audio')}>
        <Row icon={Volume2} title="Volume" description="Applies immediately to local and YouTube playback."><div className="flex items-center gap-3 w-44"><input aria-label="Volume" type="range" min="0" max="100" value={player.volume} onChange={(e) => player.setVolume(Number(e.target.value))} className="w-full accent-brand"/><span className="text-xs w-8 text-right">{player.volume}%</span></div></Row>
        <div className="pt-4"><div className="flex flex-wrap gap-2 mb-4">{player.eqPresetNames.map((name) => <Choice key={name} active={player.eqPreset === name} onClick={() => player.applyEqPreset(name)}>{name}</Choice>)}</div>
          {!player.eqSupported && <div className="sf-info-box"><Info size={15}/><span>Equalizer processing is available for local files. YouTube playback runs in a cross-origin player and cannot be routed through the browser EQ graph.</span></div>}
          <div className="grid grid-cols-5 gap-3 h-36 mt-4">{EQ_BANDS.map((freq, i) => <div key={freq} className="flex flex-col items-center"><input aria-label={`${freq} Hz EQ`} type="range" min={-12} max={12} step={1} value={player.eqGains[i]} disabled={!player.eqSupported} onChange={(e) => player.setEqBand(i, Number(e.target.value))} className="flex-1 accent-brand disabled:opacity-40" style={{ writingMode: 'vertical-lr', direction: 'rtl' }}/><span className="text-[10px] text-text-subdued mt-2">{freq >= 1000 ? `${freq / 1000}k` : freq}</span></div>)}</div>
        </div>
      </Section>

      <Section icon={Mic2} title="Lyrics" description={settings.lyricsAuto ? 'Automatic lyrics enabled' : 'Manual lyrics'} open={open.lyrics} onToggle={() => flip('lyrics')}>
        <Row icon={Mic2} title="Show lyrics automatically" description="Open available lyrics when the Now Playing view is opened." control={<Toggle checked={settings.lyricsAuto} onChange={() => toggle('lyricsAuto')} label="Show lyrics automatically"/>}/>
        <Row title="Synchronized scrolling" description="Follow the active timestamped lyric while the song plays." control={<Toggle checked={settings.lyricsAutoScroll} onChange={() => toggle('lyricsAutoScroll')} label="Synchronized lyric scrolling"/>}/>
        <Row title="Translations" description="Enable translated lyrics when the lyrics provider supplies them." control={<Toggle checked={settings.lyricsTranslation} onChange={() => toggle('lyricsTranslation')} label="Lyrics translations"/>}/>
      </Section>

      <Section icon={Palette} title="Appearance" description={settings.compactLayout ? 'Compact layout' : 'Standard layout'} open={open.appearance} onToggle={() => flip('appearance')}>
        <Row title="Compact layout" description="Reduce spacing in dense lists and settings on smaller screens." control={<Toggle checked={settings.compactLayout} onChange={() => toggle('compactLayout')} label="Compact layout"/>}/>
        <Row title="Reduce motion" description="Disable decorative transitions and animations while keeping functional feedback." control={<Toggle checked={settings.reduceMotion} onChange={() => toggle('reduceMotion')} label="Reduce motion"/>}/>
        <div className="sf-info-box"><Info size={15}/><span>Spotifusion currently follows its existing dark visual system. This section changes layout behavior without replacing your current theme or environment configuration.</span></div>
      </Section>

      <Section icon={Bell} title="Notifications" description={notifPermission === 'granted' ? 'Browser permission granted' : 'Browser permission not enabled'} open={open.notifications} onToggle={() => flip('notifications')}>
        <Row icon={Bell} title="Browser permission" description={notifPermission === 'unsupported' ? 'Notifications are not supported by this browser.' : notifPermission === 'denied' ? 'Permission is blocked in browser site settings.' : notifPermission === 'granted' ? 'Permission is already granted.' : 'Allow browser notifications for future Spotifusion notification features.'}>
          {notifPermission === 'default' && <button type="button" onClick={requestNotifications} className="sf-outline-button"><Bell size={15}/> Allow</button>}
          {notifPermission === 'granted' && <span className="text-xs text-brand font-bold">Allowed</span>}
          {notifPermission === 'denied' && <BellOff size={17} className="text-text-subdued"/>}
        </Row>
        <Row title="New release alerts" description="Store your preference for new-release notification features." control={<Toggle checked={settings.newReleaseNotifications} onChange={() => toggle('newReleaseNotifications')} label="New release alerts" disabled={notifPermission === 'denied'}/>} />
      </Section>

      <Section icon={Shield} title="Privacy" description={privateSession ? 'Private session on' : 'Normal listening history'} open={open.privacy} onToggle={() => flip('privacy')}>
        <Row icon={EyeOff} title="Private session" description="Do not add online playback to Recently Played while enabled. The preference resets when the app session ends." control={<Toggle checked={privateSession} onChange={(v) => { setPrivateSession(v); setPrivateSessionState(v) }} label="Private session"/>}/>
        <Row title="Personalized recommendations" description="Use your recent and liked tracks to build recommendation sections on Home." control={<Toggle checked={settings.personalizedRecommendations} onChange={() => toggle('personalizedRecommendations')} label="Personalized recommendations"/>}/>
        <button type="button" onClick={() => { localStorage.removeItem('spotifusion:search-history:v2'); localStorage.removeItem('spotifusion:search-history') }} className="sf-danger-button"><Trash2 size={15}/> Clear search history on this device</button>
      </Section>

      <Section icon={Gauge} title="Performance" description={settings.dataSaver ? 'Data saver on' : 'Standard loading'} open={open.performance} onToggle={() => flip('performance')}>
        <Row title="Data saver" description="Prefer smaller artwork and reduce aggressive preloading where supported." control={<Toggle checked={settings.dataSaver} onChange={() => toggle('dataSaver')} label="Data saver"/>}/>
        <div className="sf-info-box"><Gauge size={15}/><span>Search uses request cancellation, client caching and backend cache coalescing. Large result sets should be rendered incrementally rather than creating thousands of DOM nodes at once.</span></div>
      </Section>

      <Section icon={Database} title="Offline & Storage" description={`${stats.count} local songs · ${formatBytes(stats.totalBytes)}`} open={open.storage} onToggle={() => flip('storage')}>
        <LocalFilesSection />
        <div className="sf-storage-card mt-4"><div><p className="text-sm font-semibold">Local music</p><p className="text-xs text-text-subdued mt-1">{stats.count} songs · {formatBytes(stats.totalBytes)}</p></div>{!confirmingClear ? <button type="button" onClick={() => setConfirmingClear(true)} disabled={!stats.count} className="sf-danger-button disabled:opacity-40"><Trash2 size={15}/> Clear</button> : <div className="flex gap-2"><button type="button" onClick={handleClearAll} className="sf-danger-button">Delete all</button><button type="button" onClick={() => setConfirmingClear(false)} className="sf-outline-button">Cancel</button></div>}</div>
      </Section>

      <Section icon={RotateCcw} title="Reset preferences" description="Restore Spotifusion's local settings to defaults" open={open.reset || false} onToggle={() => flip('reset')}>
        <button type="button" onClick={resetPreferences} className="sf-outline-button"><RotateCcw size={15}/> Reset local preferences</button>
      </Section>

      <Section icon={CircleHelp} title="About" description="Spotifusion" open={open.about} onToggle={() => flip('about')}>
        <div className="sf-info-box"><Info size={15}/><span>Spotifusion is an independent music player and is not affiliated with Spotify. Online search uses your existing keyless YouTube Music backend. No existing Firebase, Vercel, Render or environment-variable configuration is replaced by these settings.</span></div>
        <a href="https://github.com/kruthagna-j/spotifusion" target="_blank" rel="noreferrer" className="inline-flex mt-4 text-sm text-brand font-semibold hover:underline">View source on GitHub</a>
      </Section>
    </div>
  </div>
}
