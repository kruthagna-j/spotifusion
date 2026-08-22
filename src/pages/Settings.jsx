import { useState } from 'react'
import { LogOut, Trash2, Sliders, HardDrive, User, Info } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { usePlayer, EQ_BANDS } from '@/context/PlayerContext'
import { useLocalStorageStats, clearLocalSongs } from '@/lib/localMusicDb'

function formatBytes(bytes) {
  if (!bytes) return '0 MB'
  const mb = bytes / (1024 * 1024)
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  return `${(mb / 1024).toFixed(2)} GB`
}

export default function Settings() {
  const { user, signOut } = useAuth()
  const player = usePlayer()
  const [stats, refreshStats] = useLocalStorageStats()
  const [confirmingClear, setConfirmingClear] = useState(false)

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
                  className="bg-red-500 hover:bg-red-400 text-white text-xs font-bold px-3 py-1.5 rounded-full"
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
    </div>
  )
}
