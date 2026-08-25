export function SkeletonRow() {
  return (
    <div className="flex items-center gap-3 px-4 py-2">
      <div className="skeleton w-10 h-10 rounded" />
      <div className="flex-1 min-w-0 flex flex-col gap-2">
        <div className="skeleton h-3 rounded w-1/3" />
        <div className="skeleton h-2.5 rounded w-1/5" />
      </div>
    </div>
  )
}

export function SkeletonCard() {
  return (
    <div className="bg-surface-elevated rounded-lg p-3">
      <div className="skeleton aspect-square rounded-md mb-3" />
      <div className="skeleton h-3 rounded w-3/4 mb-2" />
      <div className="skeleton h-2.5 rounded w-1/2" />
    </div>
  )
}

export function SkeletonCardGrid({ count = 5, className = '' }) {
  return (
    <div className={className}>
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  )
}

export function SkeletonRowList({ count = 5 }) {
  return (
    <div>
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonRow key={i} />
      ))}
    </div>
  )
}
