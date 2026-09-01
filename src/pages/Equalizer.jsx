import { usePlayer, EQ_BANDS } from '@/context/PlayerContext'

export default function Equalizer() {
  const player = usePlayer()
  return (
    <div className="lg-page lg-equalizer-page">
      <div className="lg-page-inner">
        <div className="lg-eq-title-row">
          <div><p className="lg-eyebrow">Audio</p><h1>Equalizer</h1><p>Shape your sound with a five-band EQ.</p></div>
          <button type="button" role="switch" aria-checked={player.eqSupported} className={`lg-switch ${player.eqSupported ? 'is-on' : ''}`} disabled={!player.eqSupported}><span/></button>
        </div>
        <div className="lg-preset-row">
          {player.eqPresetNames.map(name => <button key={name} onClick={() => player.applyEqPreset(name)} className={player.eqPreset === name ? 'is-active' : ''}>{name}</button>)}
        </div>
        {!player.eqSupported && <div className="lg-info">Equalizer processing is available for local files. Cross-origin YouTube playback cannot be routed through the browser EQ graph.</div>}
        <div className="lg-eq-card">
          <div className="lg-eq-scale"><span>+12dB</span><span>0dB</span><span>-12dB</span></div>
          <div className="lg-eq-bands">
            {EQ_BANDS.map((freq, i) => <div className="lg-eq-band" key={freq}>
              <input aria-label={`${freq} Hz EQ`} type="range" min={-12} max={12} step={1} value={player.eqGains[i]} disabled={!player.eqSupported} onChange={e => player.setEqBand(i, Number(e.target.value))}/>
              <span>{freq >= 1000 ? `${freq / 1000}k` : freq}</span>
            </div>)}
          </div>
        </div>
        <div className="lg-dial-row">
          <Dial label="Bass Boost" />
          <Dial label="Virtualizer" />
          <Dial label="Loudness" />
        </div>
      </div>
    </div>
  )
}

function Dial({ label }) {
  return <div className="lg-dial"><div className="lg-dial-ring"><div/></div><span>{label}</span></div>
}
