<template>
  <div class="points-page">
    <aside class="points-sidebar">
      <el-card class="balance-card" shadow="never">
        <span class="balance-icon-main" aria-hidden="true">
          <el-icon><Money /></el-icon>
        </span>
        <div class="balance-info">
          <div class="summary-label">余额</div>
          <div
              class="summary-value"
              :class="{ 'is-static': !shouldRollBalance }"
              :style="{ '--balance-font-size': `${balanceFontSize}px` }"
              :aria-label="`${displayedBalance} 积分`"
          >
            <template v-if="shouldRollBalance">
              <span
                  v-for="(digit, index) in balanceDigits"
                  :key="index"
                  class="balance-digit-window"
                  aria-hidden="true"
              >
                <span class="balance-digit-track" :style="getBalanceDigitStyle(digit, index)">
                  <span v-for="wheelDigit in digitWheel" :key="wheelDigit" class="balance-digit">
                    {{ wheelDigit }}
                  </span>
                </span>
              </span>
            </template>
            <span v-else class="balance-static-value">{{ displayedBalance }}</span>
          </div>
        </div>
        <span class="balance-trend-line" aria-hidden="true">
          <svg viewBox="0 0 150 82" focusable="false">
            <path
                class="trend-area"
                d="M8 67 C24 55 34 50 49 55 C65 61 74 62 88 49 C102 36 112 24 142 12 L142 82 L8 82 Z"
            />
            <path
                class="trend-line trend-line-soft"
                d="M4 45 C25 38 37 41 52 34 C70 26 87 36 105 24 C119 15 132 16 148 6"
            />
            <path
                class="trend-line trend-line-faint"
                d="M0 70 C20 62 34 64 49 55 C65 45 80 52 98 39 C116 26 127 28 150 18"
            />
            <path
                class="trend-line trend-line-main"
                d="M8 67 C24 55 34 50 49 55 C65 61 74 62 88 49 C102 36 112 24 142 12"
            />
            <circle class="trend-dot" cx="8" cy="67" r="3.5" />
            <circle class="trend-dot" cx="49" cy="55" r="3.5" />
            <circle class="trend-dot" cx="88" cy="49" r="3.5" />
            <circle class="trend-dot trend-dot-end" cx="142" cy="12" r="4.5" />
          </svg>
        </span>
      </el-card>
      <el-card class="checkin-card" shadow="never">
        <div class="checkin-icon-shell" :class="{ checked: checkedIn, flipping: checkInFlipping }">
          <el-icon v-if="checkedIn"><CircleCheckFilled /></el-icon>
          <el-icon v-else><Calendar /></el-icon>
        </div>
        <div class="checkin-title">{{ checkedIn ? '已签到' : '每日签到' }}</div>
        <div class="checkin-subtitle">{{ checkedIn ? '明天再来领取积分' : checkInRewardText() }}</div>
        <el-button
            class="checkin-button"
            type="primary"
            :disabled="checkedIn"
            :loading="checkInLoading"
            @click="handleCheckIn"
        >
          {{ checkedIn ? '已签到' : '签到' }}
        </el-button>
      </el-card>
    </aside>

    <el-card class="transactions-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>积分流水</span>
          <el-select
              v-model="typeFilter"
              placeholder="流水类型"
              clearable
              style="width: 180px"
              @change="onFilterChange"
          >
            <el-option label="全部" value="" />
            <el-option label="实名认证赠送" value="EARN_VERIFICATION" />
            <el-option label="每日签到" value="EARN_CHECK_IN" />
            <el-option label="发布扣减" value="SPEND_PUBLISH" />
            <el-option label="取消退回" value="REFUND_CANCEL" />
            <el-option label="完成入账" value="INCOME_COMPLETE" />
          </el-select>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="list"
          border
          stripe
          height="100%"
          class="transactions-table"
          style="width: 100%"
      >
        <el-table-column label="流水类型" min-width="140">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="积分变动" width="120">
          <template #default="{ row }">
            <span :class="row.amount > 0 ? 'amount-income' : 'amount-spend'">
              {{ row.amount > 0 ? '+' : '' }}{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后余额" width="120" />
        <el-table-column label="关联代取" width="110">
          <template #default="{ row }">
            <el-link
                v-if="row.relatedPickupId"
                type="primary"
                @click.stop="$router.push(`/pickup/${row.relatedPickupId}`)"
            >
              #{{ row.relatedPickupId }}
            </el-link>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadList"
            @size-change="onPageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getPointBalance, checkIn, getPointTransactions } from '@/api/points'
import { useUserStore } from '@/store/user'

const loading = ref(false)
const checkInLoading = ref(false)
const typeFilter = ref('')
const list = ref([])
const checkedIn = ref(false)
const checkInFlipping = ref(false)
const checkInPoints = ref(null)
const POINT_STATUS_UPDATED_EVENT = 'campushub:point-status-updated'
const LAST_BALANCE_STORAGE_PREFIX = 'campushub:last-point-balance'
const digitWheel = Array.from({ length: 10 }, (_, index) => index)
const userStore = useUserStore()

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const typeLabelMap = {
  EARN_VERIFICATION: '实名认证赠送',
  EARN_CHECK_IN: '每日签到',
  SPEND_PUBLISH: '发布扣减',
  REFUND_CANCEL: '取消退回',
  INCOME_COMPLETE: '完成入账'
}

function balanceStorageKey() {
  const userId = userStore.userInfo?.userId
  return userId ? `${LAST_BALANCE_STORAGE_PREFIX}:${userId}` : LAST_BALANCE_STORAGE_PREFIX
}

function normalizeBalance(value, fallback = 0) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric < 0) return fallback
  return Math.trunc(numeric)
}

function readStoredBalance() {
  try {
    return normalizeBalance(localStorage.getItem(balanceStorageKey()))
  } catch {
    return 0
  }
}

function saveStoredBalance(balance) {
  try {
    localStorage.setItem(balanceStorageKey(), String(balance))
  } catch {
    // localStorage can be unavailable in private or restricted browser modes.
  }
}

const pointBalance = ref(readStoredBalance())
const displayedBalance = ref(pointBalance.value)
const balanceDigits = computed(() => String(displayedBalance.value).split('').map(digit => Number(digit)))
const shouldRollBalance = computed(() => balanceDigits.value.length <= 4)
const balanceFontSize = computed(() => {
  const digitCount = balanceDigits.value.length
  if (digitCount <= 3) return 52
  if (digitCount === 4) return 42
  return Math.max(22, Math.min(36, Math.floor(118 / (digitCount * 0.58))))
})

function updatePointBalance(nextBalance) {
  const normalized = normalizeBalance(nextBalance, pointBalance.value)
  pointBalance.value = normalized
  displayedBalance.value = normalized
  saveStoredBalance(normalized)
}

function getBalanceDigitStyle(digit, index) {
  return {
    transform: `translateY(-${digit}em)`,
    transitionDelay: `${index * 35}ms`
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
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

function normalizePointAmount(amount) {
  if (amount == null) return null
  const n = Number(amount)
  if (Number.isNaN(n)) return null
  return Math.abs(n)
}

function checkInRewardText(points = checkInPoints.value) {
  return points == null ? '签到领取积分' : `积分+${points}`
}

function getTypeLabel(type) {
  return typeLabelMap[type] || type
}

function getTypeTag(type) {
  if (type === 'SPEND_PUBLISH') return 'danger'
  if (type === 'REFUND_CANCEL') return 'warning'
  return 'success'
}

function onFilterChange() {
  pagination.page = 1
  loadList()
}

function onPageSizeChange() {
  pagination.page = 1
  loadList()
}

async function loadBalance() {
  try {
    const [balanceRes, txRes] = await Promise.all([
      getPointBalance(),
      getPointTransactions({
        type: 'EARN_CHECK_IN',
        page: 1,
        pageSize: 1
      })
    ])
    updatePointBalance(balanceRes?.pointBalance ?? 0)
    const latestCheckInTx = txRes?.list?.[0]
    checkInPoints.value = normalizePointAmount(latestCheckInTx?.amount)
    checkedIn.value = isToday(latestCheckInTx?.createdAt)
  } catch {
    // error handled by interceptor
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (typeFilter.value) {
      params.type = typeFilter.value
    }
    const res = await getPointTransactions(params)
    list.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function emitPointStatusUpdated() {
  window.dispatchEvent(new CustomEvent(POINT_STATUS_UPDATED_EVENT, {
    detail: {
      source: 'points',
      earnedPoints: checkInPoints.value
    }
  }))
}

function handlePointStatusUpdated(event) {
  if (event.detail?.source === 'points') return
  loadBalance()
  loadList()
}

async function playCheckInFlip() {
  checkInFlipping.value = true
  await new Promise(resolve => setTimeout(resolve, 280))
  checkedIn.value = true
  await new Promise(resolve => setTimeout(resolve, 320))
  checkInFlipping.value = false
}

async function handleCheckIn() {
  checkInLoading.value = true
  try {
    const res = await checkIn()
    updatePointBalance(res?.pointBalance ?? pointBalance.value)
    checkInPoints.value = normalizePointAmount(res?.earnedPoints)
    pagination.page = 1
    await loadList()
    await playCheckInFlip()
    emitPointStatusUpdated()
    await ElMessageBox.alert(checkInRewardText(checkInPoints.value), '签到成功', {
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

onMounted(() => {
  window.addEventListener(POINT_STATUS_UPDATED_EVENT, handlePointStatusUpdated)
  loadBalance()
  loadList()
})

onUnmounted(() => {
  window.removeEventListener(POINT_STATUS_UPDATED_EVENT, handlePointStatusUpdated)
})
</script>

<style scoped>
.points-page {
  max-width: 1120px;
  height: calc(100vh - 115px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  overflow: hidden;
}

.points-sidebar {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.balance-card {
  min-height: 180px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #ff6f9f 0%, #ff8fbd 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(255, 111, 159, 0.22);
}

.balance-card :deep(.el-card__body) {
  min-height: 180px;
  padding: 28px 24px;
  position: relative;
}

.balance-icon-main {
  position: absolute;
  left: 24px;
  top: 40%;
  width: 76px;
  height: 76px;
  border-radius: 20px;
  font-size: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff6f9f;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 18px rgba(191, 53, 98, 0.16);
  transform: translateY(-50%);
}

.balance-info {
  position: absolute;
  left: 124px;
  top: 54px;
  width: 118px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.balance-trend-line {
  position: absolute;
  right: -14px;
  bottom: 7px;
  width: 148px;
  height: 82px;
  opacity: 0.42;
  pointer-events: none;
}

.balance-trend-line svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.trend-area {
  fill: rgba(255, 255, 255, 0.12);
}

.trend-line {
  fill: none;
  stroke: rgba(255, 255, 255, 0.88);
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-line-main {
  stroke-width: 5;
  filter: drop-shadow(0 7px 12px rgba(191, 53, 98, 0.14));
}

.trend-line-soft {
  stroke-width: 3;
  opacity: 0.28;
}

.trend-line-faint {
  stroke-width: 2.5;
  opacity: 0.18;
}

.trend-dot {
  fill: rgba(255, 255, 255, 0.9);
  filter: drop-shadow(0 5px 9px rgba(191, 53, 98, 0.14));
}

.trend-dot-end {
  fill: #fff;
}

.summary-label {
  width: 118px;
  font-size: 18px;
  line-height: 22px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.86);
  margin-bottom: 8px;
}

.summary-value {
  width: 118px;
  height: 58px;
  display: flex;
  align-items: flex-start;
  overflow: hidden;
  font-size: var(--balance-font-size);
  line-height: 1;
  font-weight: 700;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.summary-value.is-static {
  align-items: center;
  white-space: nowrap;
}

.balance-digit-window {
  width: 0.66em;
  height: 1em;
  flex: 0 0 auto;
  overflow: hidden;
}

.balance-static-value {
  display: block;
  width: 100%;
  overflow: hidden;
  text-overflow: clip;
  line-height: 1;
}

.balance-digit-track {
  display: flex;
  flex-direction: column;
  transition: transform 680ms cubic-bezier(0.18, 0.89, 0.32, 1.18);
  will-change: transform;
}

.balance-digit {
  width: 100%;
  height: 1em;
  line-height: 1;
  text-align: center;
}

@media (prefers-reduced-motion: reduce) {
  .balance-digit-track {
    transition: none;
  }
}

.checkin-card {
  border-radius: 8px;
}

.checkin-card :deep(.el-card__body) {
  padding: 22px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.checkin-icon-shell {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff0f6;
  color: #ff6f9f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  transform-style: preserve-3d;
}

.checkin-icon-shell.checked {
  background: #f0f9eb;
  color: #67c23a;
}

.checkin-icon-shell.flipping {
  animation: checkin-flip 0.6s ease-in-out;
}

@keyframes checkin-flip {
  from {
    transform: rotateY(0deg);
  }

  to {
    transform: rotateY(360deg);
  }
}

.checkin-title {
  margin-top: 12px;
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.checkin-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.checkin-button {
  width: 100%;
  margin-top: 18px;
}

.transactions-card {
  height: 100%;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.transactions-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 18px;
  font-weight: 600;
}

.transactions-table {
  flex: 1;
  min-height: 0;
}

.amount-income {
  font-weight: 600;
  color: #67c23a;
}

.amount-spend {
  font-weight: 600;
  color: #f56c6c;
}

.text-muted {
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  flex: 0 0 auto;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .points-page {
    height: auto;
    overflow: visible;
    grid-template-columns: 1fr;
  }

  .points-sidebar {
    height: auto;
  }

  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
