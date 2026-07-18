import { useId, useState, type ReactNode } from 'react'

type Props = {
  title: string
  /** Short hint shown in the header when collapsed (optional). */
  summary?: string
  defaultOpen?: boolean
  open?: boolean
  onOpenChange?: (open: boolean) => void
  children: ReactNode
  className?: string
  'aria-label'?: string
}

export function BuilderCollapsible({
  title,
  summary,
  defaultOpen = true,
  open: controlledOpen,
  onOpenChange,
  children,
  className,
  'aria-label': ariaLabel,
}: Props) {
  const panelId = useId()
  const [uncontrolledOpen, setUncontrolledOpen] = useState(defaultOpen)
  const isControlled = controlledOpen !== undefined
  const open = isControlled ? controlledOpen : uncontrolledOpen

  function toggle() {
    const next = !open
    if (!isControlled) {
      setUncontrolledOpen(next)
    }
    onOpenChange?.(next)
  }

  return (
    <section
      className={['builder-collapsible', className].filter(Boolean).join(' ')}
      aria-label={ariaLabel ?? title}
      data-open={open ? 'true' : 'false'}
    >
      <button
        type="button"
        className="builder-collapsible-toggle"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={toggle}
      >
        <span className="builder-collapsible-chevron" aria-hidden>
          {open ? '▾' : '▸'}
        </span>
        <span className="builder-collapsible-title">{title}</span>
        {!open && summary ? (
          <span className="builder-collapsible-summary muted">{summary}</span>
        ) : null}
      </button>
      {open ? (
        <div id={panelId} className="builder-collapsible-body">
          {children}
        </div>
      ) : null}
    </section>
  )
}
