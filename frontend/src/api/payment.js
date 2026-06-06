import request from './request'

export const getPaymentRecord = (id) => request.get(`/payments/${id}`)
export const getTransactions = (params) => request.get('/users/me/transactions', { params })
