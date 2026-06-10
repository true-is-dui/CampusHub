<template>
  <div v-loading="loading" class="my-evaluations">
    <div class="rating-overview-shell">
      <div class="rating-overview">
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
              <el-tag :type="getRatingTag(evalItem.ratingLevel)" size="small">
                {{ getRatingLabel(evalItem.ratingLevel) }}
              </el-tag>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getUserEvaluations, getUserProfile } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const loading = ref(false)
const evalLoading = ref(false)
const ratingSummary = ref(null)
const evaluations = ref([])
const evalPage = ref(1)
const evalPageSize = 20
const evalTotal = ref(0)

const emptyRoleSummary = {
  totalCount: 0,
  positiveRate: null
}

const publisherSummary = computed(() => ratingSummary.value?.publisherRoleSummary || emptyRoleSummary)
const acceptorSummary = computed(() => ratingSummary.value?.acceptorRoleSummary || emptyRoleSummary)

function currentUserId() {
  return userStore.userInfo?.userId
}

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

function getRatingTag(level) {
  const map = { GOOD: 'success', NEUTRAL: 'info', BAD: 'danger' }
  return map[level] || 'info'
}

function getRatingLabel(level) {
  const map = { GOOD: '好评', NEUTRAL: '中评', BAD: '差评' }
  return map[level] || level
}

async function loadSummary() {
  const userId = currentUserId()
  if (!userId) return
  loading.value = true
  try {
    const res = await getUserProfile(userId, true)
    ratingSummary.value = res?.ratingSummary || null
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadEvaluations() {
  const userId = currentUserId()
  if (!userId) return
  evalLoading.value = true
  try {
    const res = await getUserEvaluations(userId, {
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

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  loadSummary()
  loadEvaluations()
})
</script>

<style scoped>
.my-evaluations {
  max-width: 980px;
  margin: 0 auto;
  scroll-padding-top: 84px;
}

.rating-overview-shell {
  margin-bottom: 20px;
}

.rating-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.overview-item {
  height: 360px;
  min-height: 0;
  padding: 28px 30px;
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
  width: 170px;
  height: 170px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}

.overview-item-publisher {
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 54%, #ffc0cf 100%);
}

.overview-item-acceptor {
  background: linear-gradient(135deg, #4f8cff 0%, #6db7ff 56%, #a5dcff 100%);
}

.overview-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.overview-label {
  font-size: 22px;
  line-height: 28px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.96);
}

.overview-badge {
  min-width: 72px;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: rgba(255, 255, 255, 0.94);
  background: rgba(255, 255, 255, 0.18);
  font-size: 14px;
  font-weight: 700;
  box-sizing: border-box;
  backdrop-filter: blur(6px);
}

.thumb-icon {
  width: 17px;
  height: 17px;
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
  font-size: 15px;
  line-height: 20px;
  color: rgba(255, 255, 255, 0.78);
  font-weight: 600;
}

.rate-number-row {
  margin-top: 8px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-height: 1em;
}

.overview-item strong {
  color: #fff;
  font-size: 108px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 8px 22px rgba(118, 42, 72, 0.16);
}

.rate-percent {
  padding-bottom: 10px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 42px;
  line-height: 1;
  font-weight: 800;
}

.overview-total {
  position: relative;
  z-index: 1;
  align-self: flex-end;
  color: rgba(255, 255, 255, 0.86);
  font-size: 15px;
  line-height: 20px;
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

@media (max-width: 700px) {
  .rating-overview {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .overview-item {
    height: 230px;
    padding: 22px 24px;
  }

  .overview-item strong {
    font-size: 62px;
  }

  .eval-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
