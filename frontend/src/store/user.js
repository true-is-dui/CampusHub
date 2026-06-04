import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMe } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

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
      const res = await getMe()
      userInfo.value = res.data
      return res.data
    } catch {
      clearToken()
      return null
    }
  }

  const isLoggedIn = () => !!token.value
  const isAdmin = () => userInfo.value?.role === 'ADMIN'
  const isApproved = () => userInfo.value?.authStatus === 'APPROVED'

  return { token, userInfo, setToken, clearToken, fetchUserInfo, isLoggedIn, isAdmin, isApproved }
})
