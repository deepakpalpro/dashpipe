import { Router } from 'express'
import { asyncHandler, parseId } from '../http.js'
import * as storeService from '../services/storeService.js'

export const storeRouter = Router()

storeRouter.get(
  '/inventory',
  asyncHandler(async (_req, res) => {
    const inventory = await storeService.getInventory()
    res.json(inventory)
  }),
)

/** List orders for invoice pipelines (filterable). */
storeRouter.get(
  '/orders',
  asyncHandler(async (req, res) => {
    const complete =
      req.query.complete === undefined
        ? undefined
        : ['1', 'true', 'yes'].includes(String(req.query.complete).toLowerCase())
    const orders = await storeService.listOrders({
      status: req.query.status != null ? String(req.query.status) : undefined,
      complete,
    })
    res.json({ count: orders.length, orders })
  }),
)

storeRouter.post(
  '/order',
  asyncHandler(async (req, res) => {
    const order = await storeService.placeOrder(req.body)
    res.status(200).json(order)
  }),
)

storeRouter.get(
  '/order/:orderId',
  asyncHandler(async (req, res) => {
    const orderId = parseId(req.params.orderId, 'orderId')
    const order = await storeService.getOrderById(orderId)
    res.json(order)
  }),
)

storeRouter.delete(
  '/order/:orderId',
  asyncHandler(async (req, res) => {
    const orderId = parseId(req.params.orderId, 'orderId')
    await storeService.deleteOrder(orderId)
    res.status(200).json({ message: 'order deleted' })
  }),
)
