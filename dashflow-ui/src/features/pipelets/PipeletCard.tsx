import type { PipeletCatalogEntry } from './catalogFilter'

type Props = {
  pipelet: PipeletCatalogEntry
  onSelect?: (pipelet: PipeletCatalogEntry) => void
}

export function PipeletCard({ pipelet, onSelect }: Props) {
  const active = pipelet.active === true
  return (
    <article
      className={active ? 'pipelet-card' : 'pipelet-card pipelet-card-inactive'}
      data-testid="pipelet-card"
      data-active={active ? 'true' : 'false'}
    >
      <header className="pipelet-card-header">
        <h3>{pipelet.name}</h3>
        <div className="pipelet-card-badges">
          <span className="badge category">{pipelet.category}</span>
          <span
            className={active ? 'badge status-active' : 'badge status-inactive'}
          >
            {active ? 'Active' : 'Inactive'}
          </span>
        </div>
      </header>
      <p className="pipelet-card-desc">{pipelet.description}</p>
      <footer className="pipelet-card-meta">
        <span className="badge runtime">{pipelet.runtime}</span>
        <span className="version">v{pipelet.version}</span>
        <code className="pipelet-id">{pipelet.id}</code>
        {pipelet.imageRef ? (
          <code className="pipelet-image" title="Image / binary ref">
            {pipelet.imageRef}
          </code>
        ) : null}
      </footer>
      {onSelect ? (
        <button type="button" className="secondary" onClick={() => onSelect(pipelet)}>
          Details
        </button>
      ) : null}
    </article>
  )
}
