export type QuotaBlockedInfo = {
  code: string
  message: string
}

type Props = {
  info: QuotaBlockedInfo | null
  onDismiss?: () => void
}

export function QuotaBlockedAlert({ info, onDismiss }: Props) {
  if (!info) {
    return null
  }
  const title =
    info.code === 'NO_CREDIT'
      ? 'Run blocked — no credit'
      : info.code === 'HARD_BLOCK'
        ? 'Run blocked — quota hard limit'
        : 'Run blocked'

  return (
    <div className="quota-alert" role="alert" data-testid="quota-alert">
      <strong>{title}</strong>
      <p>{info.message}</p>
      {onDismiss ? (
        <button type="button" className="secondary" onClick={onDismiss}>
          Dismiss
        </button>
      ) : null}
    </div>
  )
}
