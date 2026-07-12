import { Router } from 'express'
import { asyncHandler, HttpError } from '../http.js'
import * as userService from '../services/userService.js'

export const userRouter = Router()

userRouter.post(
  '/',
  asyncHandler(async (req, res) => {
    const user = await userService.createUser(req.body)
    res.status(200).json(user)
  }),
)

userRouter.post(
  '/createWithList',
  asyncHandler(async (req, res) => {
    const user = await userService.createUsersWithList(req.body)
    res.status(200).json(user)
  }),
)

userRouter.get(
  '/login',
  asyncHandler(async (req, res) => {
    const result = await userService.loginUser(
      req.query.username != null ? String(req.query.username) : undefined,
      req.query.password != null ? String(req.query.password) : undefined,
    )
    res.setHeader('X-Rate-Limit', String(result.rateLimit))
    res.setHeader('X-Expires-After', result.expiresAfter)
    res.type('text/plain').send(result.token)
  }),
)

userRouter.get(
  '/logout',
  asyncHandler(async (_req, res) => {
    await userService.logoutUser()
    res.status(200).json({ message: 'ok' })
  }),
)

userRouter.get(
  '/:username',
  asyncHandler(async (req, res) => {
    if (!req.params.username) throw new HttpError(400, 'Invalid username supplied')
    const user = await userService.getUserByName(req.params.username)
    res.json(user)
  }),
)

userRouter.put(
  '/:username',
  asyncHandler(async (req, res) => {
    if (!req.params.username) throw new HttpError(400, 'Invalid username supplied')
    await userService.updateUser(req.params.username, req.body)
    res.status(200).json({ message: 'ok' })
  }),
)

userRouter.delete(
  '/:username',
  asyncHandler(async (req, res) => {
    if (!req.params.username) throw new HttpError(400, 'Invalid username supplied')
    await userService.deleteUser(req.params.username)
    res.status(200).json({ message: 'User deleted' })
  }),
)
