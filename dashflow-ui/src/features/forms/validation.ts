export type FieldErrors = Record<string, string>

export function validateConnectorForm(input: {
  connectorTypeId: string
  name: string
  baseUrl: string
}): FieldErrors {
  const errors: FieldErrors = {}
  if (!input.connectorTypeId.trim()) {
    errors.connectorTypeId = 'Connector type is required'
  }
  if (!input.name.trim()) {
    errors.name = 'Name is required'
  }
  if (!input.baseUrl.trim()) {
    errors.baseUrl = 'Base URL is required'
  }
  return errors
}

export function validateServiceForm(input: {
  serviceTypeId: string
  vendor: string
  name: string
}): FieldErrors {
  const errors: FieldErrors = {}
  if (!input.serviceTypeId.trim()) {
    errors.serviceTypeId = 'Service type is required'
  }
  if (!input.vendor.trim()) {
    errors.vendor = 'Vendor is required'
  }
  if (!input.name.trim()) {
    errors.name = 'Name is required'
  }
  return errors
}
