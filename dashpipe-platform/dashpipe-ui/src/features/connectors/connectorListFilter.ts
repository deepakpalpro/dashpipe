export type ConnectorListItem = {
  id: string
  name: string
  connectorTypeId: string
  status: string
}

export type ConnectorListFilter = {
  search?: string
  typeId?: string | 'All'
  status?: string | 'All'
}

export function filterConnectors<T extends ConnectorListItem>(
  items: T[],
  input: ConnectorListFilter,
): T[] {
  const search = (input.search ?? '').trim().toLowerCase()
  const typeId = input.typeId && input.typeId !== 'All' ? input.typeId : null
  const status =
    input.status && input.status !== 'All'
      ? input.status.toUpperCase()
      : null

  return items.filter((item) => {
    if (typeId && item.connectorTypeId !== typeId) {
      return false
    }
    if (status && item.status.toUpperCase() !== status) {
      return false
    }
    if (!search) {
      return true
    }
    return (
      item.name.toLowerCase().includes(search) ||
      item.id.toLowerCase().includes(search) ||
      item.connectorTypeId.toLowerCase().includes(search)
    )
  })
}

export function paginateItems<T>(
  items: T[],
  page: number,
  pageSize: number,
): { pageItems: T[]; page: number; totalPages: number; total: number } {
  const total = items.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize) || 1)
  const safePage = Math.min(Math.max(0, page), totalPages - 1)
  const start = safePage * pageSize
  return {
    pageItems: items.slice(start, start + pageSize),
    page: safePage,
    totalPages,
    total,
  }
}
