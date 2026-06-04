<template>
  <div v-loading="loading" class="pickup-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>代取详情</span>
      </template>
    </el-page-header>

    <template v-if="detail">
      <el-card class="detail-card" shadow="never">
        <div class="status-row">
          <el-tag :type="statusTagType" size="large">{{ statusLabel }}</el-tag>
          <span v-if="detail.rewardType === 'PAID'" class="reward">¥{{ detail.rewardAmount }}</span>
        </div>
        <div v-if="detail.status === 'WAITING_PAYMENT' && countdownText" class="countdown-bar">
          支付剩余时间：<span class="countdown" :class="{ 'countdown-expired': countdownText === '已过期' }">{{ countdownText }}</span>
        </div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="校区">{{ detail.campus }}</el-descriptions-item>
          <el-descriptions-item label="报酬类型">
            {{ detail.rewardType === 'PAID' ? '有报酬' : '无报酬' }}
          </el-descriptions-item>
          <el-descriptions-item label="取件地点">{{ detail.pickupLocation }}</el-descriptions-item>
          <el-descriptions-item label="送达地点">{{ detail.deliveryLocation }}</el-descriptions-item>
          <el-descriptions-item label="物品描述" :span="2">
            {{ detail.itemDescription || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="接单截止时间" :span="2">
            {{ detail.acceptDeadline || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- Publisher Info -->
        <div class="section">
          <h3>发布者信息</h3>
          <div class="user-info" @click="goUserPublicProfile(detail.publisher?.userId)">
            <el-avatar :size="40" :src="getAvatarUrl(detail.publisher?.userId)" icon="UserFilled" />
            <div class="user-text">
              <span class="user-name clickable">{{ detail.publisher?.nickname || '匿名用户' }}</span>
            </div>
          </div>
        </div>

        <!-- Acceptor Info -->
        <div v-if="detail.acceptor" class="section">
          <h3>接单者信息</h3>
          <div class="user-info" @click="goUserPublicProfile(detail.acceptor?.userId)">
            <el-avatar :size="40" :src="getAvatarUrl(detail.acceptor?.userId)" icon="UserFilled" />
            <div class="user-text">
              <span class="user-name clickable">{{ detail.acceptor?.nickname || '匿名用户' }}</span>
            </div>
          </div>
        </div>

        <!-- Credential Image -->
        <div v-if="detail.pickupCredentialFileId" class="section">
          <h3>取件凭证</h3>
          <el-image
            :src="credentialUrl"
            :preview-src-list="[credentialUrl]"
            fit="contain"
            class="proof-image"
          />
        </div>

        <!-- Completion Proof -->
        <div v-if="detail.completionProofFileId" class="section">
          <h3>完成凭证</h3>
          <el-image
            :src="completionProofUrl"
            :preview-src-list="[completionProofUrl]"
            fit="contain"
            class="proof-image"
          />
        </div>

        <!-- Evaluation Section -->
        <div v-if="detail.status === 'COMPLETED'" class="section">
          <h3>评价</h3>
          <template v-if="evaluation">
            <div class="eval-display">
              <el-tag :type="getRatingTag(evaluation.ratingLevel)" size="large">
                {{ getRatingLabel(evaluation.ratingLevel) }}
              </el-tag>
              <p class="eval-content">{{ evaluation.content }}</p>
            </div>
          </template>
          <template v-else-if="canEvaluate">
            <el-form :model="evalForm" label-width="60px">
              <el-form-item label="评分">
                <el-radio-group v-model="evalForm.ratingLevel">
                  <el-radio value="GOOD">好评</el-radio>
                  <el-radio value="NEUTRAL">中评</el-radio>
                  <el-radio value="BAD">差评</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="评价">
                <el-input
                  v-model="evalForm.content"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入评价内容"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="evalLoading" @click="submitEval">
                  提交评价
                </el-button>
              </el-form-item>
            </el-form>
          </template>
          <el-empty v-else description="暂无评价" :image-size="80" />
        </div>

        <!-- Action Buttons -->
        <div class="actions">
          <!-- Publisher + WAITING_PAYMENT -->
          <template v-if="isPublisher && detail.status === 'WAITING_PAYMENT'">
            <el-button type="primary" @click="handlePay">继续支付</el-button>
            <el-button type="danger" @click="handleCancel">取消订单</el-button>
          </template>

          <!-- Publisher + WAITING_ACCEPT -->
          <template v-if="isPublisher && detail.status === 'WAITING_ACCEPT'">
            <el-button type="danger" @click="handleCancel">取消订单</el-button>
          </template>

          <!-- Approved user + WAITING_ACCEPT -->
          <template v-if="!isPublisher && userStore.isApproved() && detail.status === 'WAITING_ACCEPT'">
            <el-button type="success" :loading="actionLoading" @click="handleAccept">
              接单
            </el-button>
          </template>

          <!-- Acceptor + IN_PROGRESS -->
          <template v-if="isAcceptor && detail.status === 'IN_PROGRESS' && !detail.completionProofFileId">
            <el-upload
              ref="proofUploadRef"
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              :on-change="onProofChange"
            >
              <el-button type="primary">选择完成凭证</el-button>
            </el-upload>
            <el-button
              type="success"
              :loading="actionLoading"
              :disabled="!proofFile"
              @click="handleUploadProof"
            >
              上传完成凭证
            </el-button>
          </template>

          <!-- Publisher + IN_PROGRESS + proof uploaded -->
          <template v-if="isPublisher && detail.status === 'IN_PROGRESS' && detail.completionProofFileId">
            <el-button type="success" :loading="actionLoading" @click="handleConfirm">
              确认完成
            </el-button>
          </template>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getPickupDetail,
  getPickupCredential,
  getCompletionProof,
  acceptPickup,
  uploadCompletionProof,
  confirmComplete,
  cancelPickup,
  getPaymentEntry,
  submitEvaluation,
  getEvaluationEligibility
} from '@/api/pickup'
import { getUserAvatar } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const actionLoading = ref(false)
const evalLoading = ref(false)
const detail = ref(null)
const proofFile = ref(null)
const evaluation = ref(null)
const canEvaluate = ref(false)
const evalForm = reactive({ ratingLevel: 'GOOD', content: '' })
const proofUploadRef = ref(null)
const countdownText = ref('')
let countdownTimer = null

function startCountdown(expireAtStr) {
  stopCountdown()
  if (!expireAtStr) return
  const expireAt = new Date(expireAtStr.replace(' ', 'T')).getTime()
  function tick() {
    const remaining = Math.max(0, Math.floor((expireAt - Date.now()) / 1000))
    if (remaining <= 0) {
      countdownText.value = '已过期'
      stopCountdown()
      return
    }
    const m = String(Math.floor(remaining / 60)).padStart(2, '0')
    const s = String(remaining % 60).padStart(2, '0')
    countdownText.value = `${m}:${s}`
  }
  tick()
  countdownTimer = setInterval(tick, 1000)
}

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const statusMap = {
  WAITING_PAYMENT: '待支付',
  WAITING_ACCEPT: '待接单',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusTagType = computed(() => {
  const map = {
    WAITING_PAYMENT: 'warning',
    WAITING_ACCEPT: 'info',
    IN_PROGRESS: '',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[detail.value?.status] || 'info'
})

const statusLabel = computed(() => statusMap[detail.value?.status] || detail.value?.status)

const isPublisher = computed(() => {
  return userStore.userInfo && detail.value && userStore.userInfo.userId === detail.value.publisher?.userId
})

const isAcceptor = computed(() => {
  return userStore.userInfo && detail.value && userStore.userInfo.userId === detail.value.acceptor?.userId
})

const credentialUrl = computed(() => {
  if (!detail.value?.pickupCredentialFileId) return ''
  return getPickupCredential(detail.value.pickupId)
})

const completionProofUrl = computed(() => {
  if (!detail.value?.completionProofFileId) return ''
  return getCompletionProof(detail.value.pickupId)
})

function getAvatarUrl(userId) {
  return userId ? getUserAvatar(userId) : ''
}

function goUserPublicProfile(userId) {
  if (userId) router.push(`/user/${userId}`)
}

function getRatingTag(level) {
  const map = { GOOD: 'success', NEUTRAL: 'info', BAD: 'danger' }
  return map[level] || 'info'
}

function getRatingLabel(level) {
  const map = { GOOD: '好评', NEUTRAL: '中评', BAD: '差评' }
  return map[level] || level
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getPickupDetail(route.params.id)
    detail.value = res.data
    if (detail.value.status === 'WAITING_PAYMENT' && detail.value.paymentExpireAt) {
      startCountdown(detail.value.paymentExpireAt)
    }
    // Check evaluation eligibility if completed
    if (detail.value.status === 'COMPLETED' && userStore.isLoggedIn()) {
      try {
        const evalRes = await getEvaluationEligibility(route.params.id)
        if (evalRes.data?.canEvaluate) {
          canEvaluate.value = true
        }
      } catch {
        // ignore
      }
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handlePay() {
  try {
    const res = await getPaymentEntry(detail.value.pickupId)
    const url = res.data?.payEntry || res.data?.payUrl || res.data
    if (url) {
      window.location.href = url
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleAccept() {
  try {
    await ElMessageBox.confirm('确定要接单吗？', '确认接单', { type: 'info' })
    actionLoading.value = true
    await acceptPickup(detail.value.pickupId)
    ElMessage.success('接单成功')
    loadDetail()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  } finally {
    actionLoading.value = false
  }
}

function onProofChange(file) {
  proofFile.value = file.raw
}

async function handleUploadProof() {
  if (!proofFile.value) {
    ElMessage.warning('请选择完成凭证图片')
    return
  }
  const fd = new FormData()
  fd.append('proofImage', proofFile.value)
  actionLoading.value = true
  try {
    await uploadCompletionProof(detail.value.pickupId, fd)
    ElMessage.success('上传成功')
    proofFile.value = null
    loadDetail()
  } catch {
    // error handled by interceptor
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认代取已完成？', '确认完成', { type: 'info' })
    actionLoading.value = true
    await confirmComplete(detail.value.pickupId)
    ElMessage.success('已确认完成')
    loadDetail()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  } finally {
    actionLoading.value = false
  }
}

async function handleCancel() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因（可选）', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因',
      type: 'warning'
    })
    actionLoading.value = true
    await cancelPickup(detail.value.pickupId, { reason: reason || '' })
    ElMessage.success('订单已取消')
    loadDetail()
  } catch (e) {
    if (e !== 'cancel' && e?.action !== 'cancel') {
      // error handled by interceptor
    }
  } finally {
    actionLoading.value = false
  }
}

async function submitEval() {
  if (!evalForm.ratingLevel) {
    ElMessage.warning('请选择评分')
    return
  }
  evalLoading.value = true
  try {
    await submitEvaluation(route.params.id, evalForm)
    ElMessage.success('评价成功')
    loadDetail()
  } catch {
    // error handled by interceptor
  } finally {
    evalLoading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
.pickup-detail {
  max-width: 800px;
  margin: 0 auto;
}

.detail-card {
  margin-top: 20px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.reward {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
}

.countdown-bar {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 4px;
  padding: 8px 16px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #e6a23c;
}

.countdown {
  font-weight: bold;
  font-size: 18px;
  font-family: monospace;
}

.countdown-expired {
  color: #f56c6c;
}

.detail-info {
  margin-bottom: 24px;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  font-size: 16px;
  color: #303133;
  margin-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 0;
}

.user-name {
  font-size: 15px;
  color: #303133;
}

.user-name.clickable {
  color: #409eff;
}

.user-name.clickable:hover {
  text-decoration: underline;
}

.proof-image {
  max-width: 400px;
  max-height: 300px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.eval-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.eval-content {
  color: #606266;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
