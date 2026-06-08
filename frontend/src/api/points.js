import request from './request'

export const getPointBalance = () => request.get('/users/me/point-balance')

export const checkIn = () => request.post('/users/me/check-in')

export const getPointTransactions = (params) => request.get('/users/me/point-transactions', { params })
