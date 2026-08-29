import { NavLink } from 'react-router-dom'
import { Home, Search, Library, Crown } from 'lucide-react'

const items=[
 {to:'/',icon:Home,label:'Home',end:true},
 {to:'/search',icon:Search,label:'Search'},
 {to:'/library',icon:Library,label:'Library'},
 {to:'/discover',icon:Crown,label:'Premium'},
]

export default function MobileNav(){return <nav aria-label="Primary" className="mobile-bottom-nav neon-bottom md:hidden"><div className="mobile-bottom-nav-inner">
 {items.map(({to,icon:Icon,label,end})=><NavLink key={to} to={to} end={end} className={({isActive})=>`mobile-tab ${isActive?'is-active':''}`}><span className="mobile-tab-icon"><Icon size={19}/></span><span>{label}</span></NavLink>)}
 </div></nav>}
