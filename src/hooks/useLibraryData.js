import { useEffect, useState } from 'react'
import { watchPlaylists, watchLikedSongs, watchRecentlyPlayed } from '@/lib/library'

export function usePlaylists(uid) {
  const [playlists, setPlaylists] = useState([])
  useEffect(() => {
    if (!uid) return setPlaylists([])
    return watchPlaylists(uid, setPlaylists)
  }, [uid])
  return playlists
}

export function useLikedSongs(uid) {
  const [tracks, setTracks] = useState([])
  useEffect(() => {
    if (!uid) return setTracks([])
    return watchLikedSongs(uid, setTracks)
  }, [uid])
  return tracks
}

export function useRecentlyPlayed(uid, max = 12) {
  const [tracks, setTracks] = useState([])
  useEffect(() => {
    if (!uid) return setTracks([])
    return watchRecentlyPlayed(uid, setTracks, max)
  }, [uid, max])
  return tracks
}

// ---- Status-aware variants (data + loading), for pages that want a real
// skeleton state instead of treating "still waiting on Firestore" the same
// as "genuinely has nothing yet". Kept separate from the hooks above so
// existing call sites (Sidebar, LibraryMobile, AddToPlaylistMenu, ...) don't
// need to change how they destructure the result. ----

export function usePlaylistsStatus(uid) {
  const [state, setState] = useState({ data: [], loading: !!uid })
  useEffect(() => {
    if (!uid) return setState({ data: [], loading: false })
    setState((s) => ({ ...s, loading: true }))
    return watchPlaylists(uid, (data) => setState({ data, loading: false }))
  }, [uid])
  return state
}

export function useLikedSongsStatus(uid) {
  const [state, setState] = useState({ data: [], loading: !!uid })
  useEffect(() => {
    if (!uid) return setState({ data: [], loading: false })
    setState((s) => ({ ...s, loading: true }))
    return watchLikedSongs(uid, (data) => setState({ data, loading: false }))
  }, [uid])
  return state
}

export function useRecentlyPlayedStatus(uid, max = 12) {
  const [state, setState] = useState({ data: [], loading: !!uid })
  useEffect(() => {
    if (!uid) return setState({ data: [], loading: false })
    setState((s) => ({ ...s, loading: true }))
    return watchRecentlyPlayed(uid, (data) => setState({ data, loading: false }), max)
  }, [uid, max])
  return state
}
