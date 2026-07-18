export function isAdminRole(displayName: string, isAuthenticated: boolean): boolean {
  if (!isAuthenticated) {
    return false
  }
  const name = displayName.toLowerCase()
  return name.includes('admin') || name.includes('operator')
}
