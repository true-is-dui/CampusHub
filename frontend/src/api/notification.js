import request from './request'

export const getNotifications = (params) => request.get('/users/me/notifications', { params })
export const getUnreadCount = () => request.get('/users/me/notifications/unread-count')
export const markRead = (id) => request.post(`/users/me/notifications/${id}/read`)
