import NowPlaying from '@/components/NowPlaying'
import ReferenceNowPlaying from '@/components/ReferenceNowPlaying'

export default function ResponsiveNowPlaying() {
  return (
    <>
      <div className="reference-now-playing-legacy"><NowPlaying /></div>
      <ReferenceNowPlaying />
    </>
  )
}
