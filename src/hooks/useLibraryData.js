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
