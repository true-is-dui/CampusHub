import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMe } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  // [修改] 将函数改为 computed getter，供路由守卫和组件直接访问
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isApproved = computed(() => userInfo.value?.authStatus === 'APPROVED')

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function clearToken() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    try {
      // [修改] 直接使用 res，无需再取 .data（拦截器已返回 ApiResponse.data）
      const res = await getMe()
      userInfo.value = res
      return res
    } catch {
      clearToken()
      return null
    }
  }

  return { token, userInfo, setToken, clearToken, fetchUserInfo, isLoggedIn, isAdmin, isApproved }
})