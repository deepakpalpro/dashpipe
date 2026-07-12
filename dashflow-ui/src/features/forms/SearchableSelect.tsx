import { useEffect, useId, useMemo, useRef, useState } from 'react'

export type SearchableSelectOption = {
  value: string
  label: string
  meta?: string
}

type Props = {
  label: string
  value: string
  options: SearchableSelectOption[]
  onChange: (value: string) => void
  placeholder?: string
  emptyLabel?: string
  noneLabel?: string
  allowNone?: boolean
}

export function SearchableSelect({
  label,
  value,
  options,
  onChange,
  placeholder = 'Search…',
  emptyLabel = 'No matches',
  noneLabel = 'None',
  allowNone = true,
}: Props) {
  const listId = useId()
  const rootRef = useRef<HTMLDivElement>(null)
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')

  const selected = options.find((o) => o.value === value) ?? null

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) {
      return options
    }
    return options.filter(
      (o) =>
        o.label.toLowerCase().includes(q) ||
        o.value.toLowerCase().includes(q) ||
        (o.meta ?? '').toLowerCase().includes(q),
    )
  }, [options, query])

  useEffect(() => {
    if (!open) {
      return
    }
    function onDocClick(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) {
        setOpen(false)
        setQuery('')
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        setOpen(false)
        setQuery('')
      }
    }
    document.addEventListener('mousedown', onDocClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDocClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  function pick(next: string) {
    onChange(next)
    setOpen(false)
    setQuery('')
  }

  return (
    <div className="searchable-select" ref={rootRef}>
      <span className="searchable-select-label">{label}</span>
      <button
        type="button"
        className="searchable-select-trigger"
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => {
          setOpen((v) => !v)
          setQuery('')
        }}
      >
        <span className={selected ? undefined : 'muted'}>
          {selected ? selected.label : noneLabel}
        </span>
        <span aria-hidden="true" className="searchable-select-caret">
          ▾
        </span>
      </button>

      {open ? (
        <div className="searchable-select-popover" role="presentation">
          <input
            type="search"
            className="searchable-select-search"
            aria-label={`${label} search`}
            placeholder={placeholder}
            value={query}
            autoFocus
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && filtered[0]) {
                e.preventDefault()
                pick(filtered[0].value)
              }
            }}
          />
          <ul
            id={listId}
            className="searchable-select-options"
            role="listbox"
            aria-label={label}
          >
            {allowNone ? (
              <li role="option" aria-selected={value === ''}>
                <button
                  type="button"
                  className={value === '' ? 'active' : undefined}
                  onClick={() => pick('')}
                >
                  {noneLabel}
                </button>
              </li>
            ) : null}
            {filtered.length === 0 ? (
              <li className="muted searchable-select-empty">{emptyLabel}</li>
            ) : (
              filtered.map((o) => (
                <li key={o.value} role="option" aria-selected={o.value === value}>
                  <button
                    type="button"
                    className={o.value === value ? 'active' : undefined}
                    onClick={() => pick(o.value)}
                  >
                    <span>{o.label}</span>
                    {o.meta ? (
                      <span className="muted searchable-select-meta">{o.meta}</span>
                    ) : null}
                  </button>
                </li>
              ))
            )}
          </ul>
        </div>
      ) : null}
    </div>
  )
}
