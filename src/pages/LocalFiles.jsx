import LocalFilesSection from '@/components/LocalFilesSection'

export default function LocalFiles() {
  return (
    <div className="p-4 md:p-6 pb-40 max-w-5xl mx-auto">
      <div className="mb-5">
        <h1 className="text-2xl font-bold">Local Music</h1>
        <p className="text-sm text-text-muted mt-1">Play and manage music stored on this device.</p>
      </div>
      <LocalFilesSection />
    </div>
  )
}
