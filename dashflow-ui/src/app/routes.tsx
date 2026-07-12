import { Navigate, Route, Routes } from 'react-router-dom'
import { ConnectorsPage } from '../features/connectors/ConnectorsPage'
import { ServicesPage } from '../features/services/ServicesPage'
import { PipeletsCatalogPage } from '../features/pipelets/PipeletsCatalogPage'
import { PipelinesListPage } from '../features/pipelines/PipelinesListPage'
import { PipelineBuilderPage } from '../features/pipelines/builder/PipelineBuilderPage'
import { ObservabilityPage } from '../features/observability/ObservabilityPage'
import { BillingPage } from '../features/billing/BillingPage'
import { TenantsPage } from '../features/tenants/TenantsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/pipelets" replace />} />
      <Route path="/pipelets" element={<PipeletsCatalogPage />} />
      <Route path="/pipelines" element={<PipelinesListPage />} />
      <Route path="/pipelines/new" element={<PipelineBuilderPage />} />
      <Route path="/pipelines/:pipelineId" element={<PipelineBuilderPage />} />
      <Route path="/connectors" element={<ConnectorsPage />} />
      <Route path="/services" element={<ServicesPage />} />
      <Route path="/tenants" element={<TenantsPage />} />
      <Route path="/observability" element={<ObservabilityPage />} />
      <Route path="/billing" element={<BillingPage />} />
      <Route path="*" element={<Navigate to="/pipelets" replace />} />
    </Routes>
  )
}
