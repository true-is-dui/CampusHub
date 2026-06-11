import request from './request'

export const getMe = () => request.get('/users/me')

// [修复] 删除手动 Content-Type，交由 axios 自动处理（自动添加 boundary 参数）
export const updateProfile = (formData, config = {}) => request.put('/users/me/profile', formData, config)

// [修复] 删除手动 Content-Type，交由 axios 自动处理（自动添加 boundary 参数）
export const submitVerification = (formData) => request.post('/users/me/verification', formData)

export const getUserProfile = (userId, includeRating = false) =>
    request.get(`/users/${userId}/profile`, { params: { includeRating } })

// [修改] 改为通过 request 发起 GET 请求，返回二进制图片数据
export const getUserAvatar = (userId) =>
    request.get(`/users/${userId}/avatar`, { responseType: 'blob', silentError: true })

export const getUserRatingSummary = (userId) => request.get(`/users/${userId}/rating-summary`)

export const getUserEvaluations = (userId, params) => request.get(`/users/${userId}/evaluations`, { params })
