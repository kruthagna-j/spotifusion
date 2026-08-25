export default function Logo({ withWordmark = true, className = '' }) {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <img src="/favicon.svg" alt="" className="w-7 h-7 shrink-0" aria-hidden="true" />
      {withWordmark && <span className="font-extrabold text-lg tracking-tight">Spotifusion</span>}
    </div>
  )
}
