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

// [修改] 改为通过 request 发起 GET 请求，返回二进制图片数据
export const getUserAvatar = (userId) =>
    request.get(`/users/${userId}/avatar`, { responseType: 'blob' })

export const getUserRatingSummary = (userId) => request.get(`/users/${userId}/rating-summary`)

export const getUserEvaluations = (userId, params) => request.get(`/users/${userId}/evaluations`, { params })