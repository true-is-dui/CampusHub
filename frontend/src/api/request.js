import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== undefined && data.code !== 0) {
      if (data.code === 40101) {
        localStorage.removeItem('token')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else {
        ElMessage.error(data.message || '请求失败')
      }
      return Promise.reject(data)
    }
    return data
  },
  error => {
    ElMessage.error('网络错误')
    return Promise.reject(error)
  }
)

export default request
