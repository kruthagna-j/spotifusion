import { useEffect, useMemo, useState } from 'react'
import { Bell, Heart, Menu, Music2, Play, Search, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { useRecentlyPlayedStatus, useLikedSongsStatus } from '@/hooks/useLibraryData'
import { useLocalSongs } from '@/lib/localMusicDb'
import { usePlayer } from '@/context/PlayerContext'
import { getDiscover } from '@/lib/musicApi'
import { searchMusicPage } from '@/lib/search'
import { getArtwork } from '@/lib/artwork'

const chips = ['All', 'Music', 'Podcasts', 'Live']

function Art({ track, className = '' }) {
  const src = getArtwork(track, 'medium')
  return src ? (
    <img src={src} alt="" className={className} />
  ) : (
    <div className={`${className} grid place-items-center bg-[#111817]`}>
      <Music2 size={28} />
    </div>
  )
}

function Track({ track, tracks, player }) {
  return (
    <button
      type="button"
      className="neon-track w-full text-left"
      onClick={() => player.playTrack(track, tracks)}
    >
      <Art track={track} />
      <span className="neon-track-main">
        <span className="neon-track-title block">{track.title}</span>
        <span className="neon-track-artist block">{track.artist || 'Unknown artist'}</span>
      </span>
      <span className="neon-track-time">{track.duration || track.length || ''}</span>
      <span className="text-[#66716b]">•••</span>
    </button>
  )
}

export default function Home() {
  const { user, profile } = useAuth()
  const player = usePlayer()
  const [tab, setTab] = useState('All')
  const [discover, setDiscover] = useState({ sections: [] })
  const { data: recent = [] } = useRecentlyPlayedStatus(user?.uid, 12)
  const { data: liked = [] } = useLikedSongsStatus(user?.uid)
  const [localSongs] = useLocalSongs()
  const [recommended, setRecommended] = useState([])

  useEffect(() => {
    if (!user || !navigator.onLine) return
    let dead = false
    getDiscover()
      .then((value) => {
        if (!dead) setDiscover(value || { sections: [] })
      })
      .catch(() => {})
    return () => {
      dead = true
    }
  }, [user])

  useEffect(() => {
    if (!user || !navigator.onLine) return
    let dead = false
    const artists = (profile?.favoriteArtists || [])
      .map((artist) => (typeof artist === 'string' ? artist : artist?.name))
      .filter(Boolean)
      .slice(0, 4)
    const languages = (profile?.languages || []).filter(Boolean).slice(0, 2)
    const queries = [...artists, ...languages.map((language) => `${language} songs`)].slice(0, 6)

    if (!queries.length) {
      setRecommended([])
      return
    }

    Promise.all(
      queries.map((query) =>
        searchMusicPage(query, { category: 'songs', batch: 1 }).catch(() => ({ results: [] }))
      )
    ).then((responses) => {
      if (dead) return
      const seen = new Set()
      const results = responses
        .flatMap((response) => response.results || [])
        .filter((track) => {
          if (!track?.id || seen.has(track.id)) return false
          seen.add(track.id)
          return true
        })
        .slice(0, 8)
      setRecommended(results)
    })

    return () => {
      dead = true
    }
  }, [user, profile?.favoriteArtists, profile?.languages])

  const mixes = useMemo(
    () => (recent.length ? recent : liked.length ? liked : localSongs),
    [recent, liked, localSongs]
  )
  const trending = discover.sections?.flatMap((section) => section.tracks || []).slice(0, 7) || []
  const feed = recommended.length ? recommended : trending

  return (
    <div className="neon-app min-h-full">
      <div className="neon-home">
        <header className="neon-topbar">
          <button type="button" className="neon-icon-btn" aria-label="Menu">
            <Menu size={20} />
          </button>
          <div className="neon-brand">
            <span className="neon-logo"><Music2 size={19} /></span>
            Spotifusion
          </div>
          <button type="button" className="neon-icon-btn" aria-label="Notifications">
            <Bell size={19} />
          </button>
        </header>

        <section className="neon-greeting">
          <div>
            <h1>
              Good evening{profile?.displayName ? `, ${profile.displayName.split(' ')[0]}` : ''} 👋
            </h1>
            <p>Your music universe is waiting.</p>
          </div>
          {profile?.photoURL ? (
            <img className="neon-avatar" src={profile.photoURL} alt="" />
          ) : (
            <div className="neon-avatar bg-[#18221e] grid place-items-center">
              <Music2 size={18} />
            </div>
          )}
        </section>

        <div className="neon-chips">
          {chips.map((chip) => (
            <button
              type="button"
              key={chip}
              onClick={() => setTab(chip)}
              className={`neon-chip ${tab === chip ? 'active' : ''}`}
            >
              {chip}
            </button>
          ))}
        </div>

        <section className="neon-section">
          <div className="neon-section-head">
            <h2>Recently played</h2>
            <Link className="neon-see" to="/recently-played">See all</Link>
          </div>
          {mixes.length ? (
            <div className="neon-scroll">
              {mixes.slice(0, 10).map((track) => (
                <button
                  type="button"
                  key={track.id}
                  className="neon-recent-card text-left"
                  onClick={() => player.playTrack(track, mixes)}
                >
                  <Art track={track} className="neon-cover" />
                  <div className="neon-recent-title">{track.title}</div>
                  <div className="neon-recent-sub">{track.artist || 'Playlist'}</div>
                </button>
              ))}
            </div>
          ) : (
            <div className="text-xs text-[#77817c] py-6">
              Start listening and your recent music will appear here.
            </div>
          )}
        </section>

        <section className="neon-section">
          <div className="neon-section-head">
            <h2>Made for you</h2>
            <Sparkles size={16} className="text-[#1ed760]" />
          </div>
          <div className="neon-mix-grid">
            <div className="neon-mix one">
              <small>PERSONAL MIX</small>
              <h3>Daily Mix 1</h3>
              <p>Fresh tracks built from your listening.</p>
              <div className="neon-mix-actions">
                <button type="button" className="neon-heart" aria-label="Like Daily Mix 1">
                  <Heart size={19} />
                </button>
                <button
                  type="button"
                  className="neon-play"
                  aria-label="Play Daily Mix 1"
                  onClick={() => mixes[0] && player.playTrack(mixes[0], mixes)}
                >
                  <Play size={18} fill="currentColor" />
                </button>
              </div>
            </div>
            <div className="neon-mix two">
              <small>DISCOVERY MIX</small>
              <h3>Daily Mix 2</h3>
              <p>New sounds that fit your vibe.</p>
              <div className="neon-mix-actions">
                <button type="button" className="neon-heart" aria-label="Like Daily Mix 2">
                  <Heart size={19} />
                </button>
                <button
                  type="button"
                  className="neon-play"
                  aria-label="Play Daily Mix 2"
                  onClick={() => feed[0] && player.playTrack(feed[0], feed)}
                >
                  <Play size={18} fill="currentColor" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className="neon-section">
          <div className="neon-section-head">
            <h2>Trending now</h2>
            <Link className="neon-see" to="/discover">See all</Link>
          </div>
          <div className="neon-track-list">
            {feed.length ? (
              feed.map((track) => (
                <Track key={track.id} track={track} tracks={feed} player={player} />
              ))
            ) : (
              <div className="text-xs text-[#77817c] py-6">
                Connect to the internet to discover trending music.
              </div>
            )}
          </div>
        </section>

        <section className="neon-section">
          <div className="neon-section-head"><h2>Your shortcuts</h2></div>
          <div className="grid grid-cols-2 gap-3">
            <Link to="/search" className="neon-mix !min-h-0 !p-4">
              <Search className="text-[#1ed760]" size={19} />
              <h3 className="!text-[15px] mt-3">Search music</h3>
              <p>Find songs, artists and albums.</p>
            </Link>
            <Link to="/liked-songs" className="neon-mix !min-h-0 !p-4">
              <Heart className="text-[#1ed760]" size={19} />
              <h3 className="!text-[15px] mt-3">Liked Songs</h3>
              <p>{liked.length} saved tracks.</p>
            </Link>
          </div>
        </section>
      </div>
    </div>
  )
}
