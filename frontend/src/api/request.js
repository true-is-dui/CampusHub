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

/**
 * 根据错误码和 reason 生成用户提示文本
 * @param {number} code 业务错误码
 * @param {string|null} reason errors.reason
 * @param {string} defaultMsg 默认提示信息
 * @returns {{ message: string, shouldLogout: boolean }}
 */
function buildErrorInfo(code, reason, defaultMsg) {
  const info = { message: defaultMsg, shouldLogout: false }

  if (code === 40101) {
    if (reason === 'INVALID_CREDENTIALS') {
      info.message = '用户名或密码错误'
      info.shouldLogout = false
    } else if (reason === 'TOKEN_MISSING_OR_EXPIRED') {
      info.message = '登录已过期，请重新登录'
      info.shouldLogout = true
    } else {
      // 其他未列出的 reason 也视为 token 相关问题，默认清除并跳转
      info.message = '认证失败，请重新登录'
      info.shouldLogout = true
    }
  } else if (code === 40301) {
    const reasonMap = {
      AUTH_STATUS_NOT_ALLOWED: '账号状态不允许操作',
      NOT_PICKUP_PARTICIPANT: '您不是该服务的参与者',
      ADMIN_REQUIRED: '需要管理员权限',
      NOT_NOTIFICATION_RECEIVER: '您无权操作该通知'
    }
    info.message = reasonMap[reason] || defaultMsg || '无权限操作'
  } else if (code === 40901) {
    const reasonMap = {
      DUPLICATE_OR_CONFLICTED_OPERATION: '操作冲突，请重试',
      ACCEPT_DEADLINE_EXPIRED: '接单截止时间已过，无法接单',
      PICKUP_STATUS_NOT_ALLOWED: '当前订单状态不允许该操作',
      PICKUP_EVALUATION_NOT_ALLOWED: '当前不可评价',
      COMPLETION_PROOF_NOT_AVAILABLE: '完成凭证不可用',
      VERIFICATION_REVIEW_ALREADY_HANDLED: '该审核已处理',
      INSUFFICIENT_POINTS: '积分余额不足，无法发布有报酬代取'
    }
    info.message = reasonMap[reason] || defaultMsg || '操作冲突'
  }

  return info
}

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
      return Promise.reject({ ...res, config: response.config })
    },
    error => {
      const errorConfig = error.config || error.response?.config || {}
      const silentError = Boolean(errorConfig.silentError)
      const silentReasons = Array.isArray(errorConfig.silentReasons) ? errorConfig.silentReasons : []
      const shouldSilence = (reason) => silentError || (reason && silentReasons.includes(reason))

      // 处理由上方 Promise.reject(res) 抛出的业务错误对象
      if (typeof error.code === 'number' && error.message) {
        const { code, message, errors } = error
        const reason = errors?.reason
        const { message: displayMsg, shouldLogout } = buildErrorInfo(code, reason, message)

        if (shouldSilence(reason)) {
          return Promise.reject(error)
        }

        if (code === 40001) {
          // 参数校验失败：展示结构化字段错误
          const detail = errors
              ? Object.entries(errors).map(([k, v]) => `${k}: ${v}`).join('；')
              : displayMsg
          ElMessage.error(detail)
        } else {
          // 其他业务错误：根据 shouldLogout 决定是否清除 token 并跳转
          if (shouldLogout) {
            localStorage.removeItem('token')
            router.push('/login')
          }
          ElMessage.error(displayMsg)
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
          const reason = parsed?.errors?.reason
          const message = parsed?.message || '请求失败'
          if (!shouldSilence(reason)) {
            handleHttpError(code, reason, message)
          }
          return Promise.reject(error)
        })
      }

      const code = res?.code || error.response?.status || 0
      const reason = res?.errors?.reason
      const message = res?.message || '请求失败'
      if (!shouldSilence(reason)) {
        handleHttpError(code, reason, message)
      }
      return Promise.reject(error)
    }
)

/**
 * 统一处理 HTTP 错误（含 Blob 解析后的错误）
 * @param {number} code 业务错误码或 HTTP 状态码
 * @param {string|null} reason 结构化错误原因
 * @param {string} message 错误提示信息
 */
function handleHttpError(code, reason, message) {
  const { message: displayMsg, shouldLogout } = buildErrorInfo(code, reason, message)

  if (code === 40001 || code === 400) {
    ElMessage.error(displayMsg)
  } else if (code === 40101 || code === 401) {
    if (shouldLogout) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(displayMsg)
  } else if (code === 40301 || code === 403) {
    ElMessage.error(displayMsg)
  } else if (code === 40401 || code === 404) {
    ElMessage.error(displayMsg)
  } else if (code === 40901 || code === 409) {
    ElMessage.error(displayMsg)
  } else if (code === 50001 || code === 500) {
    ElMessage.error(displayMsg)
  } else if (code === 50201) {
    ElMessage.error(displayMsg)
  } else {
    ElMessage.error(displayMsg || '网络错误，请稍后重试')
  }
}

export default request
