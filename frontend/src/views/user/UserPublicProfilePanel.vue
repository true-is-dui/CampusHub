<template>
  <div v-loading="loading" class="user-public-profile-panel">
    <template v-if="user">
      <div class="public-profile-grid">
        <el-card class="profile-section-card identity-card visitor-identity-card" shadow="never">
          <div class="identity-shell">
            <div class="identity-content">
              <el-avatar :size="104" :src="avatarSrc" icon="UserFilled" class="profile-avatar" />
              <div class="identity-text">
                <div class="visitor-name-row">
                  <div class="identity-name">{{ user.nickname || '匿名用户' }}</div>
                  <span :class="['auth-status-pill', authStatusClass]">{{ authStatusLabel }}</span>
                </div>
                <div class="visitor-public-list">
                  <div class="info-item visitor-public-row">
                    <span class="info-label">学院</span>
                    <span class="info-value">{{ user.college || '未设置' }}</span>
                  </div>
                  <div class="info-item visitor-public-row">
                    <span class="info-label">联系方式</span>
                    <span class="info-value">{{ user.contact || '未设置' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <div class="overview-item overview-item-publisher">
          <div class="overview-topline">
            <span class="overview-label">作为发布方</span>
            <span class="overview-badge">
              <span class="thumb-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <path d="M7 21H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.7l4.4-7.1c.4-.6 1.2-.8 1.8-.5 1.2.6 1.6 2 1.1 3.2L15 10h4.6c1.4 0 2.5 1.2 2.4 2.6l-.7 5.7A3 3 0 0 1 18.4 21H9Z" />
                </svg>
              </span>
              <span>好评</span>
            </span>
          </div>
          <div class="overview-rate-block">
            <span class="rate-label">好评率</span>
            <div class="rate-number-row">
              <strong>{{ formatRateNumber(publisherSummary.positiveRate) }}</strong>
              <span v-if="publisherSummary.positiveRate != null" class="rate-percent">%</span>
            </div>
          </div>
          <div class="overview-total">共收到 {{ publisherSummary.totalCount }} 条历史评价</div>
        </div>

        <div class="overview-item overview-item-acceptor">
          <div class="overview-topline">
            <span class="overview-label">作为接单方</span>
            <span class="overview-badge">
              <span class="thumb-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <path d="M7 21H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.7l4.4-7.1c.4-.6 1.2-.8 1.8-.5 1.2.6 1.6 2 1.1 3.2L15 10h4.6c1.4 0 2.5 1.2 2.4 2.6l-.7 5.7A3 3 0 0 1 18.4 21H9Z" />
                </svg>
              </span>
              <span>好评</span>
            </span>
          </div>
          <div class="overview-rate-block">
            <span class="rate-label">好评率</span>
            <div class="rate-number-row">
              <strong>{{ formatRateNumber(acceptorSummary.positiveRate) }}</strong>
              <span v-if="acceptorSummary.positiveRate != null" class="rate-percent">%</span>
            </div>
          </div>
          <div class="overview-total">共收到 {{ acceptorSummary.totalCount }} 条历史评价</div>
        </div>
      </div>

      <el-card class="eval-card" shadow="never">
        <template #header>
          <span>评价详情</span>
        </template>
        <div v-loading="evalLoading" class="eval-scroll-body">
          <el-empty v-if="!evalLoading && evaluations.length === 0" description="暂无评价" :image-size="70" />
          <div v-for="evalItem in evaluations" :key="evalItem.evaluationId" class="eval-item">
            <div class="eval-header">
              <div class="eval-title">
                <RatingTag :level="evalItem.ratingLevel" />
                <span v-if="evalItem.revieweeRoleInBusiness" class="role-text">
                  {{ evalItem.revieweeRoleInBusiness === 'PUBLISHER' ? '作为发布者' : '作为接单者' }}
                </span>
              </div>
              <span class="eval-time">{{ formatTime(evalItem.createdAt) }}</span>
            </div>
            <p class="eval-content">{{ evalItem.contentPreview || '对方未填写评价内容' }}</p>
          </div>
        </div>
        <div v-if="evalTotal > evalPageSize" class="pagination-wrapper">
          <el-pagination
              v-model:current-page="evalPage"
              :page-size="evalPageSize"
              :total="evalTotal"
              layout="prev, pager, next"
              @current-change="loadEvaluations"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { getUserAvatar, getUserEvaluations, getUserProfile } from '@/api/user'
import RatingTag from '@/components/RatingTag.vue'

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  },
  authStatus: {
    type: String,
    default: ''
  },
  role: {
    type: String,
    default: ''
  }
})

const loading = ref(true)
const evalLoading = ref(false)
const user = ref(null)
const ratingSummary = ref(null)
const evaluations = ref([])
const evalPage = ref(1)
const evalPageSize = 20
const evalTotal = ref(0)
const avatarSrc = ref('')

const emptyRoleSummary = {
  totalCount: 0,
  positiveRate: null
}

const publisherSummary = computed(() => ratingSummary.value?.publisherRoleSummary || emptyRoleSummary)
const acceptorSummary = computed(() => ratingSummary.value?.acceptorRoleSummary || emptyRoleSummary)

const isAdmin = computed(() => props.role === 'ADMIN' || user.value?.role === 'ADMIN')
const displayAuthStatus = computed(() => props.authStatus || user.value?.authStatus || 'APPROVED')
const authStatusLabel = computed(() => {
  if (isAdmin.value) return '管理员'
  const map = {
    UNVERIFIED: '未认证',
    REVIEWING: '审核中',
    APPROVED: '已认证',
    REJECTED: '认证失败'
  }
  return map[displayAuthStatus.value] || '已认证'
})
const authStatusClass = computed(() => {
  if (isAdmin.value) return 'auth-status-pill--approved'
  const map = {
    UNVERIFIED: 'auth-status-pill--unverified',
    REVIEWING: 'auth-status-pill--reviewing',
    APPROVED: 'auth-status-pill--approved',
    REJECTED: 'auth-status-pill--unverified'
  }
  return map[displayAuthStatus.value] || 'auth-status-pill--approved'
})

function formatRateNumber(rate) {
  if (rate == null) return '暂无'
  return (rate * 100).toFixed(1)
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function revokeAvatar() {
  if (avatarSrc.value) {
    URL.revokeObjectURL(avatarSrc.value)
    avatarSrc.value = ''
  }
}

async function loadAvatar() {
  if (!props.userId) return
  try {
    const blob = await getUserAvatar(props.userId)
    revokeAvatar()
    avatarSrc.value = URL.createObjectURL(blob)
  } catch {
    revokeAvatar()
  }
}

async function loadUser() {
  if (!props.userId) return
  loading.value = true
  try {
    const profileRes = await getUserProfile(props.userId, true)
    user.value = profileRes
    ratingSummary.value = profileRes?.ratingSummary || null
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadEvaluations() {
  if (!props.userId) return
  evalLoading.value = true
  try {
    const res = await getUserEvaluations(props.userId, {
      page: evalPage.value,
      pageSize: evalPageSize
    })
    evaluations.value = res?.list || []
    evalTotal.value = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    evalLoading.value = false
  }
}

watch(() => props.userId, () => {
  if (!props.userId) return
  evalPage.value = 1
  loadUser()
  loadEvaluations()
  loadAvatar()
}, { immediate: true })

onBeforeUnmount(() => {
  revokeAvatar()
})
</script>

<style scoped>
.user-public-profile-panel {
  width: 100%;
}

.public-profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-areas:
    "identity identity"
    "publisher acceptor";
  gap: 18px;
  margin-top: 24px;
  align-items: stretch;
}

.profile-section-card {
  display: flex;
  flex-direction: column;
  min-height: 232px;
  border-radius: 8px;
}

.identity-card {
  grid-area: identity;
  border-color: #d8ebff;
  background: linear-gradient(135deg, #eef7ff 0%, #f8fbff 100%);
}

.identity-card :deep(.el-card__body) {
  flex: 1;
  box-sizing: border-box;
  padding: 22px 24px;
}

.identity-shell {
  height: 100%;
}

.identity-content {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
}

.profile-avatar {
  flex: 0 0 auto;
  box-shadow: 0 12px 28px rgba(64, 158, 255, 0.16);
}

.profile-avatar :deep(.el-icon) {
  font-size: 46px;
}

.identity-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
}

.visitor-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.identity-name {
  color: #1f5f9f;
  font-size: 28px;
  line-height: 34px;
  font-weight: 800;
  letter-spacing: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visitor-public-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.visitor-public-row {
  flex-direction: row;
  align-items: baseline;
  gap: 12px;
}

.visitor-public-row .info-label {
  width: 64px;
  flex: 0 0 auto;
  color: #5d7f9f;
  font-size: 15px;
  font-weight: 700;
}

.visitor-public-row .info-value {
  min-width: 0;
  flex: 1;
  color: #2b3a4a;
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
  word-break: break-word;
}

.auth-status-pill {
  min-width: 68px;
  height: 24px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  color: #fff;
  border: 0;
  font-size: 14px;
  line-height: 1;
  font-weight: 700;
}

.auth-status-pill--reviewing {
  background: #e8c56c;
  box-shadow: 0 4px 10px rgba(232, 197, 108, 0.22);
}

.auth-status-pill--approved {
  background: #ff6f9f;
  box-shadow: 0 4px 10px rgba(255, 111, 159, 0.22);
}

.auth-status-pill--unverified {
  background: #909399;
  box-shadow: 0 4px 10px rgba(144, 147, 153, 0.18);
}

.overview-item {
  min-height: 232px;
  padding: 22px 24px;
  border-radius: 8px;
  border: none;
  color: #fff;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 18px 36px rgba(37, 48, 78, 0.14);
}

.overview-item::after {
  content: '';
  position: absolute;
  inset: auto -44px -58px auto;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}

.overview-item-publisher {
  grid-area: publisher;
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 54%, #ffc0cf 100%);
}

.overview-item-acceptor {
  grid-area: acceptor;
  background: linear-gradient(135deg, #4f8cff 0%, #6db7ff 56%, #a5dcff 100%);
}

.overview-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  position: relative;
  z-index: 1;
}

.overview-label {
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.96);
}

.overview-badge {
  min-width: 68px;
  height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: rgba(255, 255, 255, 0.94);
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
  font-weight: 700;
  box-sizing: border-box;
  backdrop-filter: blur(6px);
}

.thumb-icon {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.thumb-icon svg {
  width: 100%;
  height: 100%;
  display: block;
  fill: currentColor;
}

.overview-rate-block {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.rate-label {
  font-size: 14px;
  line-height: 20px;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 600;
}

.rate-number-row {
  margin-top: 8px;
  display: flex;
  align-items: flex-end;
  gap: 7px;
  min-height: 1em;
}

.overview-item strong {
  color: #fff;
  font-size: 58px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 8px 22px rgba(118, 42, 72, 0.16);
}

.rate-percent {
  padding-bottom: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.overview-total {
  position: relative;
  z-index: 1;
  align-self: flex-end;
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  line-height: 18px;
  font-weight: 600;
}

.eval-card {
  margin-top: 20px;
  border-radius: 8px;
}

.eval-card :deep(.el-card__header) {
  font-size: 18px;
  font-weight: 700;
}

.eval-scroll-body {
  max-height: 430px;
  min-height: 220px;
  overflow-y: auto;
  padding-right: 8px;
}

.eval-scroll-body::-webkit-scrollbar {
  width: 8px;
}

.eval-scroll-body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #dcdfe6;
}

.eval-scroll-body::-webkit-scrollbar-track {
  background: transparent;
}

.eval-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.eval-item:last-child {
  border-bottom: none;
}

.eval-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.eval-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-text {
  color: #606266;
  font-size: 13px;
}

.eval-time {
  color: #909399;
  font-size: 13px;
  white-space: nowrap;
}

.eval-content {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 640px) {
  .public-profile-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "identity"
      "publisher"
      "acceptor";
  }

  .identity-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .visitor-name-row,
  .eval-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
