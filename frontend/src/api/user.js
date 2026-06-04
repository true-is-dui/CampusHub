import request from './request'

export const getMe = () => request.get('/users/me')
export const updateProfile = (formData) => request.put('/users/me/profile', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const submitVerification = (formData) => request.post('/users/me/verification', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const getUserProfile = (userId, includeRating = false) =>
  request.get(`/users/${userId}/profile`, { params: { includeRating } })
export const getUserAvatar = (userId) => `/api/v1/users/${userId}/avatar`
export const getUserRatingSummary = (userId) => request.get(`/users/${userId}/rating-summary`)
export const getUserEvaluations = (userId, params) => request.get(`/users/${userId}/evaluations`, { params })
