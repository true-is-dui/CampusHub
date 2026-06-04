import request from './request'

export const getPickupList = (params) => request.get('/pickup-requests', { params })
export const createPickup = (formData) => request.post('/pickup-requests', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getPickupDetail = (id) => request.get(`/pickup-requests/${id}`)
export const getPickupCredential = (id) => `/api/v1/pickup-requests/${id}/credential`
export const getPaymentEntry = (id) => request.get(`/pickup-requests/${id}/payment-entry`)
export const acceptPickup = (id) => request.post(`/pickup-requests/${id}/accept`)
export const uploadCompletionProof = (id, formData) =>
  request.post(`/pickup-requests/${id}/completion-proof`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const getCompletionProof = (id) => `/api/v1/pickup-requests/${id}/completion-proof`
export const confirmComplete = (id) => request.post(`/pickup-requests/${id}/completion-confirmation`)
export const cancelPickup = (id, data) => request.post(`/pickup-requests/${id}/cancel`, data)
export const getMyPickups = (params) => request.get('/users/me/pickup-requests', { params })
export const submitEvaluation = (pickupId, data) =>
  request.post(`/pickup-requests/${pickupId}/evaluations`, data)
export const getEvaluationEligibility = (pickupId) =>
  request.get(`/pickup-requests/${pickupId}/evaluation-eligibility`)
