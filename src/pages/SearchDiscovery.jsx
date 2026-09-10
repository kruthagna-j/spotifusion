import { ArrowUpRight, Clock, Music2, TrendingUp, X } from 'lucide-react'

const POPULAR_SEARCHES = ['billie eilish', 'michael jackson', 'arijit singh hits', 'lofi hip hop', 'dua lipa', 'synthwave']

export default function SearchDiscovery({ history, onSelect, onClear }) {
  return <div className="sf-emergent-search-discovery">
    {history.length > 0 && <section><div className="sf-emergent-discovery-heading"><h2><Clock size={16} /> Recent Searches</h2><button type="button" onClick={onClear}>Clear all</button></div><div className="sf-emergent-discovery-list">{history.slice(0, 6).map((term) => <button type="button" key={term} onClick={() => onSelect(term)}><span><Clock size={15} />{term}</span><X size={14} /></button>)}</div></section>}
    <section><div className="sf-emergent-discovery-heading"><h2><TrendingUp size={16} /> Popular Searches</h2></div><div className="sf-emergent-discovery-list">{POPULAR_SEARCHES.map((term) => <button type="button" key={term} onClick={() => onSelect(term)}><span><Music2 size={15} />{term}</span><ArrowUpRight size={16} /></button>)}</div></section>
  </div>
}
