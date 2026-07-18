import { Router } from 'express'
import { asyncHandler, parseId } from '../http.js'
import * as petService from '../services/petService.js'

export const petRouter = Router()

petRouter.post(
  '/',
  asyncHandler(async (req, res) => {
    const pet = await petService.addPet(req.body)
    res.status(200).json(pet)
  }),
)

petRouter.put(
  '/',
  asyncHandler(async (req, res) => {
    const pet = await petService.updatePet(req.body)
    res.status(200).json(pet)
  }),
)

petRouter.get(
  '/findByStatus',
  asyncHandler(async (req, res) => {
    const status = String(req.query.status ?? 'available')
    const pets = await petService.findPetsByStatus(status)
    res.json(pets)
  }),
)

petRouter.get(
  '/findByTags',
  asyncHandler(async (req, res) => {
    const raw = req.query.tags
    const tags = Array.isArray(raw) ? raw.map(String) : raw != null ? [String(raw)] : []
    const pets = await petService.findPetsByTags(tags)
    res.json(pets)
  }),
)

petRouter.get(
  '/:petId',
  asyncHandler(async (req, res) => {
    const petId = parseId(req.params.petId, 'petId')
    const pet = await petService.getPetById(petId)
    res.json(pet)
  }),
)

petRouter.post(
  '/:petId',
  asyncHandler(async (req, res) => {
    const petId = parseId(req.params.petId, 'petId')
    const pet = await petService.updatePetWithForm(petId, {
      name: req.query.name != null ? String(req.query.name) : undefined,
      status: req.query.status != null ? String(req.query.status) : undefined,
    })
    res.json(pet)
  }),
)

petRouter.delete(
  '/:petId',
  asyncHandler(async (req, res) => {
    const petId = parseId(req.params.petId, 'petId')
    await petService.deletePet(petId)
    res.status(200).json({ message: 'Pet deleted' })
  }),
)

petRouter.post(
  '/:petId/uploadImage',
  asyncHandler(async (req, res) => {
    const petId = parseId(req.params.petId, 'petId')
    const chunks = []
    for await (const chunk of req) {
      chunks.push(chunk)
    }
    const buffer = Buffer.concat(chunks)
    const result = await petService.uploadImage(petId, {
      additionalMetadata:
        req.query.additionalMetadata != null ? String(req.query.additionalMetadata) : undefined,
      buffer,
    })
    res.json(result)
  }),
)
