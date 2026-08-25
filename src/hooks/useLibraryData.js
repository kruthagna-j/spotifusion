import { useMemo, useSyncExternalStore } from 'react'
import { watchPlaylists, watchLikedSongs, watchRecentlyPlayed } from '@/lib/library'

// Share one Firestore listener per user/resource instead of creating one from
'the row. This prevents O(rows) listeners and keeps library updates bounded.
const stores=new Map()
const EMPTY_ARRAY=Object.freeze([])
function getStore(key,subscribeFactory,emptyValue){
  let store=stores.get(key); if(store) return store
  store={value:emptyValue,listeners:new Set(),unsubscribe:null,refCount:0,initialized:false,
    subscribe(listener){
      store.listeners.add(listener); store.refCount++
      if(!store.unsubscribe) store.unsubscribe=subscribeFactory(next=>{store.value=next;store.initialized=true;store.listeners.forEach(fn=>fn())})
      return ()=>{store.listeners.delete(listener);store.refCount--;if(store.refCount<=0&&store.unsubscribe){store.unsubscribe();store.unsubscribe=null;store.refCount=0}}
    },
    getSnapshot(){return store.value}
  }
  stores.set(key,store); return store
}
function useSharedResource(uid,kind,subscribeFactory,emptyValue){
  const store=useMemo(()=>uid?getStore(`${kind}:${uid}`,subscribeFactory,emptyValue):null,[uid,kind])
  return useSyncExternalStore(store?store.subscribe:()=>()=>{},store?store.getSnapshot:()=>emptyValue,()=>emptyValue)
}
export function usePlaylists(uid){return useSharedResource(uid,'playlists',cb=>watchPlaylists(uid,cb),EMPTY_ARRAY)}
export function useLikedSongs(uid){return useSharedResource(uid,'likedSongs',cb=>watchLikedSongs(uid,cb),EMPTY_ARRAY)}
export function useLikedSongIds(uid){const liked=useLikedSongs(uid);return useMemo(()=>new Set(liked.map(t=>t.id)),[liked])}
export function useRecentlyPlayed(uid,max=12){return useSharedResource(uid,`recentlyPlayed:${max}`,cb=>watchRecentlyPlayed(uid,cb,max),EMPTY_ARRAY)}
export function usePlaylistsStatus(uid){const data=usePlaylists(uid);const store=uid?stores.get(`playlists:${uid}`):null;return {data,loading:!!uid&&!store?.initialized}}
export function useLikedSongsStatus(uid){const data=useLikedSongs(uid);const store=uid?stores.get(`likedSongs:${uid}`):null;return {data,loading:!!uid&&!store?.initialized}}
export function useRecentlyPlayedStatus(uid,max=12){const data=useRecentlyPlayed(uid,max);const store=uid?stores.get(`recentlyPlayed:${max}:${uid}`):null;return {data,loading:!!uid&&!store?.initialized}}
