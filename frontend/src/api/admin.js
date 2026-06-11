import request from './request'

export const getVerificationReviews = (params) => request.get('/admin/verification-reviews', { params })

export const handleReview = (reviewId, data) =>
    request.post(`/admin/verification-reviews/${reviewId}/handle`, data)

// [修改] 发起 GET 请求获取实名认证材料照片（二进制），responseType 设为 blob
export const getReviewImage = (reviewId) =>
    request.get(`/admin/verification-reviews/${reviewId}/image`, {
        responseType: 'blob',
        silentReasons: ['RESOURCE_NOT_FOUND']
    })
