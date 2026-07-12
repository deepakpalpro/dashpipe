import { useRef, useState } from 'react'
import {
  exportPipeline,
  importPipeline,
} from '../../api/resources'
import type { PipelineBundle } from '../../api/types'

type Props = {
  tenantId: string
  /** When set, shows Export for this pipeline. */
  pipelineId?: string | null
  pipelineName?: string | null
  onImported?: (pipelineId: string, message: string) => void
  onError?: (message: string) => void
}

function downloadJson(filename: string, data: unknown) {
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json',
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export function PipelineImportExportControls({
  tenantId,
  pipelineId,
  pipelineName,
  onImported,
  onError,
}: Props) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState(false)
  const [reuse, setReuse] = useState(true)

  async function handleExport() {
    if (!pipelineId) {
      return
    }
    setBusy(true)
    try {
      const bundle = await exportPipeline(tenantId, pipelineId)
      const safeName = (pipelineName || bundle.pipeline.name || 'pipeline')
        .replace(/[^a-zA-Z0-9._-]+/g, '-')
        .toLowerCase()
      downloadJson(`${safeName}.pipeline.json`, bundle)
    } catch (err) {
      onError?.(err instanceof Error ? err.message : 'Export failed')
    } finally {
      setBusy(false)
    }
  }

  async function handleFile(file: File) {
    setBusy(true)
    try {
      const text = await file.text()
      const parsed = JSON.parse(text) as PipelineBundle
      if (!parsed?.pipeline || !parsed.format_version) {
        throw new Error('Invalid pipeline bundle JSON')
      }
      const result = await importPipeline(tenantId, {
        bundle: parsed,
        conflict_strategy: reuse ? 'reuse' : 'create',
      })
      const warn =
        result.warnings?.length > 0
          ? ` Warnings: ${result.warnings.join('; ')}`
          : ''
      onImported?.(
        result.pipeline_id,
        `Imported “${result.name}” (${result.pipeline_id}).${warn}`,
      )
    } catch (err) {
      onError?.(err instanceof Error ? err.message : 'Import failed')
    } finally {
      setBusy(false)
      if (fileRef.current) {
        fileRef.current.value = ''
      }
    }
  }

  return (
    <div className="button-row pipeline-io-controls" aria-label="Import export">
      {pipelineId ? (
        <button
          type="button"
          className="secondary"
          disabled={busy}
          onClick={() => {
            void handleExport()
          }}
        >
          {busy ? 'Working…' : 'Export'}
        </button>
      ) : null}
      <label className="button-link import-file-label">
        <input
          ref={fileRef}
          type="file"
          accept="application/json,.json"
          className="sr-only"
          disabled={busy}
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) {
              void handleFile(file)
            }
          }}
        />
        {busy ? 'Working…' : 'Import'}
      </label>
      <label className="inline-check muted">
        <input
          type="checkbox"
          checked={reuse}
          disabled={busy}
          onChange={(e) => setReuse(e.target.checked)}
        />
        Reuse existing connectors/services
      </label>
    </div>
  )
}
