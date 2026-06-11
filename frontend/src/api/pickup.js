import request from './request'

export const getPickupList = (params) => request.get('/pickup-requests', { params })

// [修复] 删除手动 Content-Type，交由 axios 自动处理
export const createPickup = (formData) => request.post('/pickup-requests', formData)

export const getPickupDetail = (id) => request.get(`/pickup-requests/${id}`)

export const getPickupCredential = (id) =>
    request.get(`/pickup-requests/${id}/credential`, {
        responseType: 'blob',
        silentReasons: ['RESOURCE_NOT_FOUND']
    })

export const acceptPickup = (id) => request.post(`/pickup-requests/${id}/accept`)

// [修复] 删除手动 Content-Type，交由 axios 自动处理
export const uploadCompletionProof = (id, formData) =>
    request.post(`/pickup-requests/${id}/completion-proof`, formData)

export const getCompletionProof = (id, config = {}) =>
    request.get(`/pickup-requests/${id}/completion-proof`, {
        responseType: 'blob',
        ...config,
        silentReasons: [
            'RESOURCE_NOT_FOUND',
            ...(Array.isArray(config.silentReasons) ? config.silentReasons : [])
        ]
    })

export const confirmComplete = (id) => request.post(`/pickup-requests/${id}/completion-confirmation`)
export const cancelPickup = (id, data) => request.post(`/pickup-requests/${id}/cancel`, data)
export const getMyPickups = (params) => request.get('/users/me/pickup-requests', { params })

export const submitEvaluation = (pickupId, data) =>
    request.post(`/pickup-requests/${pickupId}/evaluations`, data)

export const getPickupEvaluations = (pickupId) =>
    request.get(`/pickup-requests/${pickupId}/evaluations`)

export const getEvaluationEligibility = (pickupId) =>
    request.get(`/pickup-requests/${pickupId}/evaluation-eligibility`)

export const getReceivedEvaluation = (pickupId) =>
    request.get(`/pickup-requests/${pickupId}/received-evaluation`)
