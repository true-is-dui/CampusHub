<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-content">
        <div class="header-left">
          <router-link class="brand-mark" to="/hall" aria-label="返回代取大厅">
            <img class="brand-logo-img" :src="brandLogo" alt="CampusHub" />
          </router-link>
          <el-menu
              :default-active="activeMenu"
              mode="horizontal"
              :ellipsis="false"
              router
              class="nav-menu"
          >
            <el-menu-item index="/hall">
              <span class="nav-item-content">
                <el-icon class="nav-item-icon"><House /></el-icon>
                <span>大厅</span>
              </span>
            </el-menu-item>
            <el-menu-item index="/my-pickups">
              <span class="nav-item-content">
                <el-icon class="nav-item-icon"><Tickets /></el-icon>
                <span>历史订单</span>
              </span>
            </el-menu-item>
            <el-menu-item index="/points">
              <span class="nav-item-content">
                <el-icon class="nav-item-icon"><Coin /></el-icon>
                <span>积分</span>
              </span>
            </el-menu-item>
            <el-menu-item index="/my-evaluations">
              <span class="nav-item-content">
                <el-icon class="nav-item-icon"><Star /></el-icon>
                <span>历史评价</span>
              </span>
            </el-menu-item>
            <el-menu-item v-if="userStore.isAdmin" index="/admin/verification">
              管理后台
            </el-menu-item>
          </el-menu>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <div class="avatar-popover-anchor">
              <el-popover
                  v-model:visible="userPopoverVisible"
                  placement="bottom"
                  trigger="manual"
                  :width="240"
                  :offset="16"
                  popper-class="user-popover"
              >
                <template #reference>
                  <button
                      class="avatar-entry"
                      :class="{ active: userPopoverVisible }"
                      type="button"
                      @mouseenter="showUserPopover"
                      @mouseleave="scheduleHideUserPopover"
                      @click="goProfileFromCard"
                      aria-label="个人中心"
                  >
                    <el-avatar :size="38" :src="userStore.avatarUrl" icon="UserFilled" class="nav-avatar" />
                  </button>
                </template>
                <div class="user-card" @mouseenter="showUserPopover" @mouseleave="scheduleHideUserPopover">
                  <div class="profile-card-link" @click="goProfileFromCard">
                    <el-avatar :size="92" :src="userStore.avatarUrl" icon="UserFilled" class="card-avatar" />
                    <div class="card-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</div>
                  </div>
                  <div class="card-meta">
                    <el-tag :type="authTagType" size="small" :class="{ 'approved-auth-tag': userStore.userInfo?.authStatus === 'APPROVED' }">
                      {{ authStatusLabel }}
                    </el-tag>
                    <el-tag v-if="userStore.userInfo?.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
                  </div>
                  <div class="card-points">
                    <span>积分余额</span>
                    <strong>{{ userStore.userInfo?.pointBalance ?? 0 }}</strong>
                  </div>
                  <div class="card-actions">
                    <button class="logout-action" type="button" @click="handleLogout">
                      <el-icon><SwitchButton /></el-icon>
                      <span>退出登录</span>
                    </button>
                  </div>
                </div>
              </el-popover>
            </div>
            <button
                class="header-icon-entry checkin-entry"
                :class="{ done: checkedIn }"
                type="button"
                :disabled="checkedIn || checkInLoading"
                @click="handleCheckIn"
                aria-label="每日签到"
            >
              <el-badge is-dot :hidden="!checkInStatusLoaded || checkedIn">
                <span class="checkin-icon-shell" :class="{ checked: checkedIn }">
                  <span v-if="checkedIn" class="checked-calendar-icon" aria-hidden="true"></span>
                  <el-icon v-else class="header-action-icon"><Calendar /></el-icon>
                </span>
              </el-badge>
              <span class="header-icon-label">{{ checkedIn ? '已签' : '签到' }}</span>
            </button>
            <button class="message-entry" type="button" @click="openNotificationDrawer" aria-label="消息通知">
              <el-badge :value="unreadCount > 99 ? '99+' : unreadCount" :hidden="unreadCount <= 0">
                <el-icon class="message-icon"><Message /></el-icon>
              </el-badge>
              <span class="message-label">消息</span>
            </button>
            <el-button class="header-publish-button" type="primary" @click="router.push('/publish')">
              <el-icon><Upload /></el-icon>
              <span>发布</span>
            </el-button>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <div v-if="showProfilePerspectiveFilter" class="profile-perspective-bar">
      <span class="profile-perspective-label">视角</span>
      <el-segmented
          v-model="profilePerspective"
          class="profile-perspective-switch"
          :options="profilePerspectiveOptions"
      />
    </div>
    <el-main class="layout-main">
      <router-view :key="$route.fullPath" />
    </el-main>
    <el-drawer
        v-model="notificationDrawerVisible"
        direction="rtl"
        size="420px"
        :with-header="false"
        destroy-on-close
        append-to-body
        class="notification-drawer"
        modal-class="notification-drawer-overlay"
    >
      <NotificationList @navigate="closeNotificationDrawer" />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getUnreadCount } from '@/api/notification'
import { checkIn, getPointBalance, getPointTransactions } from '@/api/points'
import NotificationList from '@/views/notification/NotificationList.vue'
import brandLogo from '@/assets/brand-logo.png'
// 不再需要单独引入 getUserAvatar，已由 Store 管理

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const userPopoverVisible = ref(false)
const notificationDrawerVisible = ref(false)
const checkedIn = ref(false)
const checkInStatusLoaded = ref(false)
const checkInLoading = ref(false)
let pollTimer = null
let userPopoverTimer = null
const POINT_STATUS_UPDATED_EVENT = 'campushub:point-status-updated'

const activeMenu = computed(() => route.path)
const showProfilePerspectiveFilter = computed(() => route.path === '/profile')
const profilePerspectiveOptions = [
  { label: '我自己', value: 'self' },
  { label: '访客', value: 'visitor' }
]
const profilePerspective = computed({
  get() {
    return route.query.perspective === 'visitor' ? 'visitor' : 'self'
  },
  set(value) {
    const query = { ...route.query }
    if (value === 'visitor') {
      query.perspective = 'visitor'
    } else {
      delete query.perspective
    }
    router.replace({ path: route.path, query })
  }
})

const authStatusLabel = computed(() => {
  const map = {
    UNVERIFIED: '未认证',
    REVIEWING: '审核中',
    APPROVED: '已认证',
    REJECTED: '认证失败'
  }
  return map[userStore.userInfo?.authStatus] || '未知'
})

const authTagType = computed(() => {
  const map = {
    UNVERIFIED: 'info',
    REVIEWING: 'warning',
    APPROVED: '',
    REJECTED: 'danger'
  }
  return map[userStore.userInfo?.authStatus] || 'info'
})

// 监听用户 ID 变化，自动触发 Store 加载头像
watch(() => userStore.userInfo?.userId, () => {
  userStore.loadAvatar()
  fetchUnreadCount()
  loadCheckInStatus()
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

// 将 fetchUnreadCount 提供给子组件，以便标记已读后立即刷新
provide('refreshUnreadCount', fetchUnreadCount)

function openNotificationDrawer() {
  notificationDrawerVisible.value = true
  fetchUnreadCount()
}

function closeNotificationDrawer() {
  notificationDrawerVisible.value = false
}

function isToday(dateStr) {
  if (!dateStr) return false
  const d = new Date(dateStr)
  if (Number.isNaN(d.getTime())) return false
  const now = new Date()
  return d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
}

function formatCheckInReward(points) {
  return points == null ? '积分已到账' : `积分+${points}`
}

async function loadCheckInStatus() {
  if (!userStore.isLoggedIn) {
    checkedIn.value = false
    checkInStatusLoaded.value = false
    return
  }
  checkInStatusLoaded.value = false
  try {
    const [balanceRes, txRes] = await Promise.all([
      getPointBalance(),
      getPointTransactions({
        type: 'EARN_CHECK_IN',
        page: 1,
        pageSize: 1
      })
    ])
    if (userStore.userInfo) {
      userStore.userInfo.pointBalance = balanceRes?.pointBalance ?? userStore.userInfo.pointBalance
    }
    checkedIn.value = isToday(txRes?.list?.[0]?.createdAt)
  } catch {
    // error handled by interceptor
  } finally {
    checkInStatusLoaded.value = true
  }
}

function emitPointStatusUpdated(earnedPoints = null) {
  window.dispatchEvent(new CustomEvent(POINT_STATUS_UPDATED_EVENT, {
    detail: {
      source: 'layout',
      earnedPoints
    }
  }))
}

function handlePointStatusUpdated(event) {
  if (event.detail?.source === 'layout') return
  loadCheckInStatus()
}

async function handleCheckIn() {
  if (checkedIn.value || checkInLoading.value) return
  checkInLoading.value = true
  try {
    const res = await checkIn()
    checkedIn.value = true
    checkInStatusLoaded.value = true
    if (userStore.userInfo) {
      userStore.userInfo.pointBalance = res?.pointBalance ?? userStore.userInfo.pointBalance
    }
    await userStore.fetchUserInfo()
    emitPointStatusUpdated(res?.earnedPoints ?? null)
    await ElMessageBox.alert(formatCheckInReward(res?.earnedPoints), '签到成功', {
      confirmButtonText: '知道了',
      type: 'success',
      center: true,
      customClass: 'checkin-success-dialog'
    })
  } catch (err) {
    if (err?.errors?.reason === 'ALREADY_CHECKED_IN_TODAY') {
      checkedIn.value = true
      emitPointStatusUpdated()
    }
  } finally {
    checkInLoading.value = false
  }
}

function handleLogout() {
  userStore.clearToken()
  router.push('/login')
}

function hideUserPopoverNow() {
  if (userPopoverTimer) {
    clearTimeout(userPopoverTimer)
    userPopoverTimer = null
  }
  userPopoverVisible.value = false
}

function goProfileFromCard() {
  hideUserPopoverNow()
  router.push('/profile')
}

function showUserPopover() {
  if (userPopoverTimer) {
    clearTimeout(userPopoverTimer)
    userPopoverTimer = null
  }
  userPopoverVisible.value = true
}

function scheduleHideUserPopover() {
  if (userPopoverTimer) {
    clearTimeout(userPopoverTimer)
  }
  userPopoverTimer = setTimeout(() => {
    userPopoverVisible.value = false
    userPopoverTimer = null
  }, 140)
}

onMounted(() => {
  fetchUnreadCount()
  loadCheckInStatus()
  window.addEventListener(POINT_STATUS_UPDATED_EVENT, handlePointStatusUpdated)
  // 若用户信息已存在，立即从 Store 加载头像
  if (userStore.userInfo?.userId) {
    userStore.loadAvatar()
  }
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  window.removeEventListener(POINT_STATUS_UPDATED_EVENT, handlePointStatusUpdated)
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (userPopoverTimer) {
    clearTimeout(userPopoverTimer)
    userPopoverTimer = null
  }
})
</script>

<style scoped>
/* 样式完全不变 */
.layout-container {
  min-height: 100vh;
  position: relative;
}

.layout-header {
  background: #fff;
  height: 64px;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 3001;
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
  gap: 16px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  width: 172px;
  height: 50px;
  overflow: hidden;
  text-decoration: none;
  cursor: pointer;
}

.brand-mark:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 3px;
  border-radius: 4px;
}

.brand-logo-img {
  width: 202px;
  height: auto;
  display: block;
  flex: 0 0 auto;
  transform: translate(-21px, 8px);
}

.nav-menu {
  border-bottom: none;
  margin-left: 36px;
}

.nav-menu :deep(.el-menu-item) {
  padding: 0 20px;
  font-size: 16px;
  font-weight: 600;
}

.nav-item-content {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  line-height: 1;
}

.nav-item-icon {
  font-size: 19px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-entry,
.message-entry {
  border: none;
  background: transparent;
  width: 42px;
  min-height: 42px;
  padding: 3px 2px;
  color: #606266;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  line-height: 1;
}

.header-icon-entry:disabled {
  cursor: not-allowed;
}

.header-icon-entry:not(:disabled):hover,
.message-entry:hover {
  color: #409eff;
}

.header-icon-entry :deep(.el-badge),
.message-entry :deep(.el-badge) {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-entry :deep(.el-badge__content) {
  min-width: 16px;
  width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0;
  font-size: 10px;
  border-radius: 50%;
}

.message-entry :deep(.el-badge__content.is-fixed) {
  transform: translateY(-40%) translateX(100%);
}

.message-entry :deep(.el-badge__content:not(.is-dot)) {
  box-sizing: border-box;
}

.header-action-icon,
.message-icon {
  font-size: 22px;
}

.checkin-icon-shell {
  width: 26px;
  height: 26px;
  color: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.18s ease;
}

.checkin-icon-shell.checked {
  color: #67c23a;
}

.checked-calendar-icon {
  width: 18px;
  height: 18px;
  border: 2px solid currentColor;
  border-radius: 4px;
  box-sizing: border-box;
  position: relative;
}

.checked-calendar-icon::before {
  content: '';
  position: absolute;
  left: 3px;
  right: 3px;
  top: 5px;
  border-top: 2px solid currentColor;
}

.checked-calendar-icon::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 8px;
  width: 8px;
  height: 5px;
  border-left: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: rotate(-45deg);
}

.header-icon-label,
.message-label {
  font-size: 12px;
  line-height: 14px;
  color: inherit;
}

.checkin-entry.done {
  color: #606266;
  opacity: 1;
}

.header-publish-button {
  height: 36px;
  min-width: 92px;
  border-radius: 7px;
  margin-left: 8px;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(64, 158, 255, 0.22);
}

.header-publish-button :deep(.el-icon) {
  font-size: 19px;
}

.avatar-popover-anchor {
  margin-right: 16px;
  display: flex;
  align-items: center;
}

.avatar-entry {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 3002;
}

.nav-avatar {
  transform-origin: center center;
  transition: transform 0.18s ease;
}

.avatar-entry.active .nav-avatar {
  transform: scale(2.30) translateY(13px);
}

.nav-avatar :deep(.el-icon) {
  font-size: 22px;
}

.user-card {
  padding: 40px 4px 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.profile-card-link {
  width: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-avatar {
  margin-top: -106px;
  opacity: 0;
  pointer-events: none;
}

.card-avatar :deep(.el-icon) {
  font-size: 38px;
}

.card-name {
  margin-top: 10px;
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
}

.card-meta {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.card-points {
  min-width: 118px;
  display: grid;
  grid-template-columns: auto minmax(42px, 1fr);
  align-items: center;
  column-gap: 10px;
  margin-top: 14px;
  color: #bd8b1f;
  font-size: 14px;
  font-weight: 600;
}

.card-points strong {
  color: #d49a22;
  font-size: 21px;
  line-height: 1;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.card-actions {
  width: 100%;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.approved-auth-tag {
  --el-tag-bg-color: #ff6f9f;
  --el-tag-border-color: #ff6f9f;
  --el-tag-text-color: #fff;
  height: 24px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(255, 111, 159, 0.22);
}

.logout-action {
  width: 100%;
  min-height: 46px;
  border: none;
  background: transparent;
  color: #f56c6c;
  cursor: pointer;
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-size: 18px;
  font-weight: 700;
  text-align: center;
}

.logout-action:hover {
  background: #fef0f0;
  border-radius: 6px;
}

.logout-action .el-icon {
  font-size: 24px;
}

.layout-main {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
}

.profile-perspective-bar {
  position: absolute;
  top: 84px;
  right: max(16px, calc((100vw - 1120px) / 2 + 16px));
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.profile-perspective-label {
  color: #606266;
  font-size: 14px;
  font-weight: 700;
}

.profile-perspective-switch {
  --el-segmented-item-selected-bg-color: #409eff;
  --el-segmented-item-selected-color: #fff;
  --el-segmented-bg-color: #f4f7fb;
  padding: 3px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(37, 48, 78, 0.08);
}

.profile-perspective-switch :deep(.el-segmented__item) {
  min-width: 54px;
  height: 30px;
  border-radius: 6px;
  font-weight: 700;
}

.profile-perspective-switch :deep(.el-segmented__item-selected) {
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.18);
}

:global(.checkin-success-dialog) {
  width: 380px;
  border-radius: 10px;
  padding: 20px 24px 22px;
}

:global(.checkin-success-dialog .el-message-box__header) {
  padding-bottom: 12px;
}

:global(.checkin-success-dialog .el-message-box__title) {
  font-size: 21px;
  font-weight: 700;
}

:global(.checkin-success-dialog .el-message-box__content) {
  padding: 16px 0 18px;
  font-size: 18px;
  line-height: 1.6;
}

:global(.checkin-success-dialog .el-message-box__status) {
  font-size: 28px !important;
}

:global(.checkin-success-dialog .el-message-box__btns) {
  padding-top: 8px;
}

:global(.notification-drawer) {
  max-width: min(420px, 92vw);
}

:global(.notification-drawer-overlay) {
  top: 64px;
  height: calc(100% - 64px);
}

:global(.notification-drawer .el-drawer__body) {
  padding: 18px 18px 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
</style>
