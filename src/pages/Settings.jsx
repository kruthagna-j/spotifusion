import { useEffect, useState } from 'react'
import {
  LogOut,
  Trash2,
  Sliders,
  HardDrive,
  User,
  Info,
  EyeOff,
  Bell,
  BellOff,
  CircleHelp,
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlayer, EQ_BANDS } from '@/context/PlayerContext'
import { useLocalStorageStats, clearLocalSongs } from '@/lib/localMusicDb'
import { isPrivateSession, setPrivateSession } from '@/lib/privacySettings'
import LocalFilesSection from '@/components/LocalFilesSection'

function formatBytes(bytes) {
  if (!bytes) return '0 MB'
  const mb = bytes / (1024 * 1024)
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  return `${(mb / 1024).toFixed(2)} GB`
}

function Toggle({ checked, onChange, label }) {
  return (
    <button
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={`w-11 h-6 rounded-full relative transition-colors shrink-0 ${
        checked ? 'bg-brand' : 'bg-surface-highlight'
      }`}
    >
      <span
        className={`absolute top-0.5 w-5 h-5 rounded-full bg-white transition-transform ${
          checked ? 'translate-x-[22px]' : 'translate-x-0.5'
        }`}
      />
    </button>
  )
}

export default function Settings() {
  const { user, signOut } = useAuth()
  const player = usePlayer()
  const [stats, refreshStats] = useLocalStorageStats()
  const [confirmingClear, setConfirmingClear] = useState(false)
  const [privateSession, setPrivateSessionState] = useState(isPrivateSession())
  const [notifPermission, setNotifPermission] = useState(
    typeof Notification !== 'undefined' ? Notification.permission : 'unsupported'
  )

  function togglePrivateSession(value) {
    setPrivateSession(value)
    setPrivateSessionState(value)
  }

  async function requestNotifications() {
    if (typeof Notification === 'undefined') return
    const result = await Notification.requestPermission()
    setNotifPermission(result)
  }

  async function handleClearAll() {
    await clearLocalSongs()
    refreshStats()
    setConfirmingClear(false)
  }

  return (
    <div className="p-4 md:p-6 max-w-2xl">
      <h1 className="text-2xl font-bold mb-6">Settings</h1>

      {/* Account */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <User size={16} /> Account
        </h2>
        {user ? (
          <div className="flex items-center justify-between bg-surface-elevated rounded-lg p-4">
            <div className="flex items-center gap-3 min-w-0">
              {user.photoURL && (
                <img src={user.photoURL} alt="" className="w-10 h-10 rounded-full" />
              )}
              <div className="min-w-0">
                <p className="text-sm font-medium truncate">{user.displayName}</p>
                <p className="text-xs text-text-subdued truncate">{user.email}</p>
              </div>
            </div>
            <button
              onClick={signOut}
              className="flex items-center gap-2 text-sm text-text-muted hover:text-text px-3 py-1.5 rounded-full hover:bg-surface-hover shrink-0"
            >
              <LogOut size={14} /> Log out
            </button>
          </div>
        ) : (
          <p className="text-sm text-text-subdued">Not signed in.</p>
        )}
      </section>

      {/* Playback / Privacy */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <EyeOff size={16} /> Playback & Privacy
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4 flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-medium mb-0.5">Private session</p>
            <p className="text-xs text-text-subdued">
              While on, songs you play won't be added to Recently Played. Resets when you reload
              the app, same as Spotify's own private session.
            </p>
          </div>
          <Toggle checked={privateSession} onChange={togglePrivateSession} label="Private session" />
        </div>
      </section>

      {/* Notifications */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <Bell size={16} /> Notifications
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4">
          {notifPermission === 'unsupported' ? (
            <p className="text-sm text-text-subdued">Notifications aren't supported in this browser.</p>
          ) : (
            <>
              <div className="flex items-center justify-between gap-4 mb-2">
                <div className="flex items-center gap-2">
                  {notifPermission === 'granted' ? (
                    <Bell size={16} className="text-brand" />
                  ) : (
                    <BellOff size={16} className="text-text-muted" />
                  )}
                  <span className="text-sm">
                    {notifPermission === 'granted'
                      ? 'Allowed'
                      : notifPermission === 'denied'
                        ? 'Blocked (change this in your browser\'s site settings)'
                        : 'Not requested yet'}
                  </span>
                </div>
                {notifPermission === 'default' && (
                  <button
                    onClick={requestNotifications}
                    className="bg-brand text-black text-xs font-bold px-3 py-1.5 rounded-full shrink-0"
                  >
                    Allow
                  </button>
                )}
              </div>
              <p className="text-xs text-text-subdued">
                Spotifusion doesn't send any notifications yet, so this doesn't do anything
                visible today — it's here so the permission is ready if/when that's added.
              </p>
            </>
          )}
        </div>
      </section>

      {/* Sleep timer */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <span aria-hidden="true">⏱</span> Sleep timer
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4 flex flex-wrap items-center gap-2">
          <span className="text-sm text-text-subdued mr-2">
            {player.sleepTimerSeconds
              ? `Stops playback in ${Math.ceil(player.sleepTimerSeconds / 60)} min`
              : 'No sleep timer is active'}
          </span>
          {[5, 10, 15, 30, 60].map((minutes) => (
            <button
              key={minutes}
              onClick={() => player.setSleepTimer(minutes * 60)}
              className="text-xs font-semibold px-3 py-1.5 rounded-full bg-surface-highlight hover:bg-surface-hover"
            >
              {minutes}m
            </button>
          ))}
          {player.sleepTimerSeconds && (
            <button
              onClick={player.clearSleepTimer}
              className="text-xs font-semibold px-3 py-1.5 rounded-full text-red-300 bg-surface-highlight hover:bg-surface-hover"
            >
              Cancel
            </button>
          )}
        </div>
      </section>

      {/* Equalizer */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <Sliders size={16} /> Equalizer
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4">
          {!player.eqSupported && (
            <div className="flex items-start gap-2 text-xs text-text-subdued bg-surface-highlight rounded-md p-3 mb-4">
              <Info size={14} className="shrink-0 mt-0.5" />
              <span>
                {player.currentTrack
                  ? "The equalizer only works for Local Files. It's not applied right now because "
                  : 'The equalizer only works for Local Files — '}
                browsers block reading/processing audio from YouTube's embedded player for
                security reasons (it runs in a separate, cross-origin frame), so there's no
                honest way to apply EQ to it. Play a local file to use these controls.
              </span>
            </div>
          )}

          <div className="flex flex-wrap gap-2 mb-5">
            {player.eqPresetNames.map((name) => (
              <button
                key={name}
                disabled={!player.eqSupported}
                onClick={() => player.applyEqPreset(name)}
                className={`text-xs font-semibold px-3 py-1.5 rounded-full disabled:opacity-40 disabled:cursor-not-allowed ${
                  player.eqPreset === name
                    ? 'bg-brand text-black'
                    : 'bg-surface-highlight hover:bg-surface-hover'
                }`}
              >
                {name}
              </button>
            ))}
          </div>

          <div className="grid grid-cols-5 gap-3 h-40">
            {EQ_BANDS.map((freq, i) => (
              <div key={freq} className="flex flex-col items-center h-full">
                <input
                  type="range"
                  min={-12}
                  max={12}
                  step={1}
                  value={player.eqGains[i]}
                  disabled={!player.eqSupported}
                  onChange={(e) => player.setEqBand(i, Number(e.target.value))}
                  className="flex-1 accent-brand disabled:opacity-40"
                  style={{ writingMode: 'vertical-lr', direction: 'rtl' }}
                />
                <span className="text-[10px] text-text-subdued mt-2">
                  {freq >= 1000 ? `${freq / 1000}k` : freq}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Data-saving and offline (local files) */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <HardDrive size={16} /> Offline & Local Files
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4">
          <LocalFilesSection />
        </div>
      </section>

      {/* Storage */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <HardDrive size={16} /> Storage
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4">
          <div className="flex items-center justify-between text-sm mb-1">
            <span className="text-text-subdued">Local songs stored</span>
            <span className="font-medium">{stats.count}</span>
          </div>
          <div className="flex items-center justify-between text-sm mb-4">
            <span className="text-text-subdued">Estimated storage used</span>
            <span className="font-medium">{formatBytes(stats.totalBytes)}</span>
          </div>

          {!confirmingClear ? (
            <button
              onClick={() => setConfirmingClear(true)}
              disabled={stats.count === 0}
              className="flex items-center gap-2 text-sm text-red-400 hover:text-red-300 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <Trash2 size={14} /> Delete all locally stored music
            </button>
          ) : (
            <div className="bg-surface-highlight rounded-md p-3">
              <p className="text-sm mb-3">Delete all locally stored music? This can't be undone.</p>
              <div className="flex gap-2">
                <button
                  onClick={handleClearAll}
                  className="bg-red-500 hover:bg-red-400 text-white text-xs font-bold py-2 px-3 rounded-full"
                >
                  Delete all
                </button>
                <button
                  onClick={() => setConfirmingClear(false)}
                  className="text-xs font-bold px-3 py-1.5 rounded-full hover:bg-surface-hover"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* About */}
      <section className="mb-8">
        <h2 className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-muted mb-3">
          <CircleHelp size={16} /> About & Support
        </h2>
        <div className="bg-surface-elevated rounded-lg p-4 text-sm text-text-muted">
          <p className="mb-2">
            Spotifusion is a free, independent music player — not affiliated with Spotify.
            Search is powered by a keyless YouTube Music integration (ytmusicapi), subject to
            YouTube's own availability and rate limits.
          </p>
          <a
            href="https://github.com/kruthagna-j/spotifusion"
            target="_blank"
            rel="noreferrer"
            className="text-brand hover:underline"
          >
            View source on GitHub
          </a>
        </div>
      </section>

      {/* Honest scope note, not a feature list */}
      <p className="text-xs text-text-subdued">
        Not included: media quality selection (YouTube's own API for this was disabled by Google
        and no longer has any effect — building a toggle for it would do nothing), account/social
        features like followers or multiple profiles, and theming/language options. These would
        need functionality Spotifusion doesn't have rather than a settings switch.
      </p>
    </div>
  )
}
