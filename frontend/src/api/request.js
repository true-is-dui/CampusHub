import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// 请求拦截器：携带 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 统一处理业务错误：成功返回 data，失败从 error.response.data 提取业务码
request.interceptors.response.use(
    response => {
      // 若请求配置为 blob 响应类型（如图片），直接返回二进制数据，不做业务码校验
      if (response.config.responseType === 'blob') {
        return response.data
      }

      const res = response.data
      if (res.code === 0) {
        return res.data           // 成功时返回 ApiResponse.data
      }
      // 业务码非 0，直接交给错误处理分支（抛出包含业务码的对象）
      return Promise.reject(res)
    },
    error => {
      // 处理由上方 Promise.reject(res) 抛出的业务错误对象
      if (typeof error.code === 'number' && error.message) {
        const { code, message, errors } = error
        // 按错误码分类处理
        switch (code) {
          case 40001: {
            // 提取结构化字段错误，如 { role: "role 必填", rewardType: "..." }
            const detail = errors
                ? Object.entries(errors).map(([k, v]) => `${k}: ${v}`).join('；')
                : (message || '参数校验失败')
            ElMessage.error(detail)
            break
          }
          case 40101:
            localStorage.removeItem('token')
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
            break
          case 40301:
            ElMessage.error(message || '无权限操作')
            break
          case 40401:
            ElMessage.error(message || '资源不存在')
            break
          case 40901:
            ElMessage.error(message || '操作冲突')
            break
          case 50001:
            ElMessage.error('服务器内部错误')
            break
          case 50201:
            ElMessage.error(message || '第三方服务异常，请稍后重试')
            break
          default:
            ElMessage.error(message || '请求失败')
            break
        }
        return Promise.reject(error)
      }

      // 以下为 HTTP 错误处理逻辑（网络错误或 HTTP 状态码错误）
      let res = error.response?.data
      // 若错误响应为 blob 类型（例如获取头像失败），需要解析 JSON 错误体
      if (error.response?.config?.responseType === 'blob' && res instanceof Blob) {
        return res.text().then(text => {
          let parsed = null
          try { parsed = JSON.parse(text) } catch (e) { /* ignore */ }
          const code = parsed?.code || error.response?.status || 0
          const message = parsed?.message || '请求失败'
          // 复用错误码处理逻辑
          handleHttpError(code, message)
          return Promise.reject(error)
        })
      }

      const code = res?.code || error.response?.status || 0
      const message = res?.message || '请求失败'
      handleHttpError(code, message)
      return Promise.reject(error)
    }
)

/**
 * 统一处理 HTTP 错误（含 Blob 解析后的错误）
 * @param {number|string} code 业务错误码或 HTTP 状态码
 * @param {string} message 错误提示信息
 */
function handleHttpError(code, message) {
  if (code === 40001 || code === 400) {
    ElMessage.error(message || '参数校验失败')
  } else if (code === 40101 || code === 401) {
    localStorage.removeItem('token')
    router.push('/login')
    ElMessage.error('登录已过期，请重新登录')
  } else if (code === 40301 || code === 403) {
    ElMessage.error(message || '无权限操作')
  } else if (code === 40401 || code === 404) {
    ElMessage.error(message || '资源不存在')
  } else if (code === 40901 || code === 409) {
    ElMessage.error(message || '操作冲突')
  } else if (code === 50001 || code === 500) {
    ElMessage.error('服务器内部错误')
  } else if (code === 50201) {
    ElMessage.error(message || '第三方服务异常，请稍后重试')
  } else {
    ElMessage.error(message || '网络错误，请稍后重试')
  }
}

export default request