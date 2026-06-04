import request from './request'

export const getVerificationReviews = (params) => request.get('/admin/verification-reviews', { params })
export const handleReview = (reviewId, data) =>
  request.post(`/admin/verification-reviews/${reviewId}/handle`, data)
export const getReviewImage = (reviewId) => `/api/v1/admin/verification-reviews/${reviewId}/image`
