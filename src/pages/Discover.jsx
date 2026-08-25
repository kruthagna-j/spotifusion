import { Compass, Search, Music2, Sparkles } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
const suggestions = ['Fein', 'The Weeknd', 'Taylor Swift', 'Arijit Singh', 'A.R. Rahman', 'Imagine Dragons', 'Billie Eilish', 'Drake', 'Anirudh Ravichander', 'Linkin Park']
export default function Discover() {
 const navigate=useNavigate()
 return <div className="p-4 md:p-7 max-w-6xl mx-auto">
  <div className="sf-panel p-7 md:p-10 mb-8 bg-gradient-to-br from-brand/20 via-white/5 to-transparent">
   <div className="flex items-center gap-2 text-brand text-xs uppercase tracking-[.2em] font-black"><Compass size={16}/> Discover</div>
   <h1 className="text-4xl md:text-5xl font-black mt-3">Find something you’ll love.</h1>
   <p className="text-text-muted mt-3 max-w-xl">Search the Spotifusion catalog and jump straight into playback.</p>
   <button onClick={()=>navigate('/search')} className="mt-6 inline-flex items-center gap-2 bg-white text-black font-black px-5 py-3 rounded-full"><Search size={17}/> Search music</button>
  </div>
  <h2 className="text-xl font-black mb-4">Quick searches</h2>
  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">{suggestions.map((s,i)=><button key={s} onClick={()=>navigate('/search',{state:{query:s}})} className="sf-panel p-4 text-left hover:bg-white/10 transition"><div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center mb-4"><Music2 size={18}/></div><span className="font-bold text-sm">{s}</span><span className="block text-xs text-text-subdued mt-1">Search</span></button>)}</div>
  <div className="mt-9 sf-panel p-6 flex gap-4 items-start"><Sparkles className="text-brand shrink-0" size={22}/><div><h3 className="font-bold">Tip</h3><p className="text-sm text-text-muted mt-1">Open any track from Search to launch the expanded player with lyrics, queue, shuffle, repeat and sleep timer.</p></div></div>
 </div>
}
