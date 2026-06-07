import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMe, getUserAvatar } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const avatarUrl = ref('')  // 全局头像 Object URL

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
    avatarUrl.value = ''  // 清除头像
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    try {
      const res = await getMe()
      userInfo.value = res
      return res
    } catch {
      clearToken()
      return null
    }
  }

  // 从后端加载头像（返回 Blob，生成 Object URL）
  async function loadAvatar() {
    if (!userInfo.value?.userId) {
      avatarUrl.value = ''
      return
    }
    try {
      const blob = await getUserAvatar(userInfo.value.userId)
      if (avatarUrl.value) {
        URL.revokeObjectURL(avatarUrl.value)
      }
      avatarUrl.value = URL.createObjectURL(blob)
    } catch {
      avatarUrl.value = ''
    }
  }

  // 直接设置头像（用于上传后立即更新，避免再次请求后端）
  function updateAvatarUrl(url) {
    if (avatarUrl.value) {
      URL.revokeObjectURL(avatarUrl.value)
    }
    avatarUrl.value = url
  }

  return {
    token,
    userInfo,
    avatarUrl,
    setToken,
    clearToken,
    fetchUserInfo,
    loadAvatar,
    updateAvatarUrl,
    isLoggedIn,
    isAdmin,
    isApproved
  }
})