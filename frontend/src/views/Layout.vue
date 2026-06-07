<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-content">
        <div class="header-left">
          <span class="logo">CampusHub</span>
          <el-menu
              :default-active="activeMenu"
              mode="horizontal"
              :ellipsis="false"
              router
              class="nav-menu"
          >
            <el-menu-item index="/hall">代取大厅</el-menu-item>
            <el-menu-item index="/publish">发布代取</el-menu-item>
            <el-menu-item index="/my-pickups">我的代取</el-menu-item>
            <el-menu-item index="/notifications">
              通知
              <el-badge
                  v-if="unreadCount > 0"
                  :value="unreadCount"
                  :max="99"
                  class="notification-badge"
              />
            </el-menu-item>
            <el-menu-item index="/profile">个人中心</el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/verification">
              管理后台
            </el-menu-item>
          </el-menu>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-dropdown">
                <el-avatar :size="32" :src="avatarSrc" icon="UserFilled" />
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="transactions">交易记录</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="layout-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUnreadCount } from '@/api/notification'
import { getUserAvatar } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const avatarSrc = ref('')
let pollTimer = null // 轮询定时器

const activeMenu = computed(() => route.path)

async function loadAvatar() {
  if (!userStore.userInfo?.userId) return
  try {
    const blob = await getUserAvatar(userStore.userInfo.userId)
    if (avatarSrc.value) {
      URL.revokeObjectURL(avatarSrc.value)
    }
    avatarSrc.value = URL.createObjectURL(blob)
  } catch {
    avatarSrc.value = ''
  }
}

watch(() => userStore.userInfo?.userId, () => {
  loadAvatar()
})

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getUnreadCount()
    unreadCount.value = res?.unreadCount || 0
  } catch {
    // ignore
  }
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'transactions') {
    router.push('/transactions')
  } else if (command === 'logout') {
    userStore.clearToken()
    router.push('/login')
  }
}

onMounted(() => {
  fetchUnreadCount()
  loadAvatar()
  // [新增] 每30秒轮询未读通知数
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  // [新增] 清除轮询定时器
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  white-space: nowrap;
}

.nav-menu {
  border-bottom: none;
}

.notification-badge {
  margin-left: 4px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #303133;
}

.username {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}
</style>