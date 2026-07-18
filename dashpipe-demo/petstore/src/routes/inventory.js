import { Router } from 'express'
import { asyncHandler, HttpError } from '../http.js'
import * as inventoryService from '../services/inventoryService.js'

export const inventoryRouter = Router()

inventoryRouter.get(
  '/summary',
  asyncHandler(async (_req, res) => {
    const summary = await inventoryService.getProductSummary()
    res.json(summary)
  }),
)

inventoryRouter.get(
  '/items',
  asyncHandler(async (req, res) => {
    const items = await inventoryService.listItems({
      category: req.query.category != null ? String(req.query.category) : undefined,
      status: req.query.status != null ? String(req.query.status) : undefined,
    })
    res.json(items)
  }),
)

inventoryRouter.get(
  '/items/:sku',
  asyncHandler(async (req, res) => {
    if (!req.params.sku) throw new HttpError(400, 'SKU is required')
    const item = await inventoryService.getItemBySku(req.params.sku)
    res.json(item)
  }),
)

inventoryRouter.post(
  '/items',
  asyncHandler(async (req, res) => {
    const item = await inventoryService.createItem(req.body)
    res.status(201).json(item)
  }),
)

inventoryRouter.put(
  '/items/:sku',
  asyncHandler(async (req, res) => {
    if (!req.params.sku) throw new HttpError(400, 'SKU is required')
    const item = await inventoryService.upsertItem(req.params.sku, req.body)
    res.json(item)
  }),
)

inventoryRouter.delete(
  '/items/:sku',
  asyncHandler(async (req, res) => {
    if (!req.params.sku) throw new HttpError(400, 'SKU is required')
    await inventoryService.deleteItem(req.params.sku)
    res.status(200).json({ message: 'Inventory item deleted' })
  }),
)

/** Bulk upload for data pipelines */
inventoryRouter.post(
  '/upload',
  asyncHandler(async (req, res) => {
    const result = await inventoryService.uploadItems(req.body)
    res.status(200).json(result)
  }),
)
