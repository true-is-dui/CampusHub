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
          <div class="status-left">
            <el-tag :type="statusTagType" size="large">{{ statusLabel }}</el-tag>
            <span v-if="detail.status === 'CANCELLED' && detail.cancelReason" class="cancel-reason">
              取消原因：{{ cancelReasonLabel(detail.cancelReason) }}
            </span>
          </div>
          <span v-if="detail.rewardType === 'PAID'" class="reward">¥{{ detail.rewardAmount }}</span>
        </div>

        <el-descriptions :column="2" border class="detail-info">
          <el-descriptions-item label="校区">{{ campusLabel(detail.campus) }}</el-descriptions-item>
          <el-descriptions-item label="报酬类型">
            {{ detail.rewardType === 'PAID' ? '有报酬' : '无报酬' }}
          </el-descriptions-item>
          <el-descriptions-item label="取件地点">{{ detail.pickupLocation }}</el-descriptions-item>
          <el-descriptions-item label="送达地点">{{ detail.deliveryLocation }}</el-descriptions-item>
          <el-descriptions-item label="物品描述" :span="2">
            {{ detail.itemDescription || '无' }}
          </el-descriptions-item>
          <!-- 接单截止时间使用格式化函数展示 -->
          <el-descriptions-item label="接单截止时间" :span="2">
            {{ formatDateTime(detail.acceptDeadline) || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- Publisher Info -->
        <div class="section">
          <h3>发布者信息</h3>
          <div class="user-info" @click="goUserPublicProfile(detail.publisher?.userId)">
            <el-avatar :size="40" :src="publisherAvatarUrl" icon="UserFilled" />
            <div class="user-text">
              <span class="user-name clickable">{{ detail.publisher?.nickname || '匿名用户' }}</span>
            </div>
          </div>
        </div>

        <!-- Acceptor Info -->
        <div v-if="detail.acceptor" class="section">
          <h3>接单者信息</h3>
          <div class="user-info" @click="goUserPublicProfile(detail.acceptor?.userId)">
            <el-avatar :size="40" :src="acceptorAvatarUrl" icon="UserFilled" />
            <div class="user-text">
              <span class="user-name clickable">{{ detail.acceptor?.nickname || '匿名用户' }}</span>
            </div>
          </div>
        </div>

        <!-- Credential Image -->
        <div v-if="credentialUrl" class="section">
          <h3>取件凭证</h3>
          <el-image
              :src="credentialUrl"
              :preview-src-list="[credentialUrl]"
              fit="contain"
              class="proof-image"
          />
        </div>

        <!-- Completion Proof -->
        <div v-if="completionProofUrl" class="section">
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
                    placeholder="请输入评价内容（差评必填）"
                    :maxlength="300"
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
            <div v-if="countdown" class="countdown-section">
              <span v-if="countdown !== '已过期'" class="countdown-text">
                支付剩余：<strong>{{ countdown }}</strong>
              </span>
              <el-tag v-else type="danger">支付已超时</el-tag>
            </div>
            <el-button
                type="primary"
                @click="handlePay"
                :disabled="countdown === '已过期' || !countdown"
            >
              继续支付
            </el-button>
            <el-button type="danger" @click="handleCancel">取消订单</el-button>
          </template>

          <!-- Publisher + WAITING_ACCEPT -->
          <template v-if="isPublisher && detail.status === 'WAITING_ACCEPT'">
            <el-button type="danger" @click="handleCancel">取消订单</el-button>
          </template>

          <!-- Approved user + WAITING_ACCEPT -->
          <template v-if="!isPublisher && userStore.isApproved && detail.status === 'WAITING_ACCEPT'">
            <el-button type="success" :loading="actionLoading" @click="handleAccept">
              接单
            </el-button>
          </template>

          <!-- Acceptor + IN_PROGRESS -->
          <template v-if="isAcceptor && detail.status === 'IN_PROGRESS' && !completionProofUrl">
            <div>
              <el-upload
                  ref="proofUploadRef"
                  :auto-upload="false"
                  :limit="1"
                  accept="image/jpeg,image/png"
                  :on-change="onProofChange"
              >
                <el-button type="primary">选择完成凭证</el-button>
              </el-upload>
              <div class="upload-tip">JPG/PNG，不超过5MB，最多上传一张</div>
            </div>
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
          <template v-if="isPublisher && detail.status === 'IN_PROGRESS' && completionProofUrl">
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
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

const credentialUrl = ref('')
const completionProofUrl = ref('')
const publisherAvatarUrl = ref('')
const acceptorAvatarUrl = ref('')

const expireAt = ref(null)
const countdown = ref('')
let countdownTimer = null

const campusMap = {
  GULOU: '鼓楼校区',
  XIANLIN: '仙林校区',
  SUZHOU: '苏州校区',
  PUKOU: '浦口校区'
}
const campusLabel = (code) => campusMap[code] || code

const cancelReasonMap = {
  USER_CANCELLED: '用户取消',
  PAYMENT_EXPIRED: '支付超时',
  ACCEPT_DEADLINE_EXPIRED: '接单截止超时',
  SYSTEM_CANCELLED: '系统取消'
}
const cancelReasonLabel = (reason) => cancelReasonMap[reason] || reason

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
  return userStore.userInfo?.userId && detail.value && userStore.userInfo.userId === detail.value.publisher?.userId
})

const isAcceptor = computed(() => {
  return userStore.userInfo?.userId && detail.value && userStore.userInfo.userId === detail.value.acceptor?.userId
})

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

// 格式化日期时间为 YYYY-MM-DD HH:mm
function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function updateCountdown() {
  if (!expireAt.value) {
    countdown.value = ''
    return
  }
  const diff = new Date(expireAt.value) - Date.now()
  if (diff <= 0) {
    countdown.value = '已过期'
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
      // 自动刷新详情，以获取后端已取消的状态
      loadDetail()
    }
    return
  }
  const min = Math.floor(diff / 60000)
  const sec = Math.floor((diff % 60000) / 1000)
  countdown.value = `${min}:${String(sec).padStart(2, '0')}`
}

function startCountdown() {
  if (countdownTimer) clearInterval(countdownTimer)
  if (expireAt.value) {
    updateCountdown()
    if (countdown.value !== '已过期') {
      countdownTimer = setInterval(updateCountdown, 1000)
    }
  }
}

function revokeBlobURL(url) {
  if (url) URL.revokeObjectURL(url)
}

async function loadDetail() {
  loading.value = true
  try {
    // 释放旧的 Blob URL
    revokeBlobURL(credentialUrl.value)
    revokeBlobURL(completionProofUrl.value)
    revokeBlobURL(publisherAvatarUrl.value)
    revokeBlobURL(acceptorAvatarUrl.value)
    credentialUrl.value = ''
    completionProofUrl.value = ''
    publisherAvatarUrl.value = ''
    acceptorAvatarUrl.value = ''

    const res = await getPickupDetail(route.params.id)
    detail.value = res

    const currentUserId = userStore.userInfo?.userId
    const isParticipant = currentUserId && (
        currentUserId === detail.value.publisher?.userId ||
        currentUserId === detail.value.acceptor?.userId
    )

    if (isParticipant) {
      if (detail.value.pickupId) {
        try {
          const credBlob = await getPickupCredential(detail.value.pickupId)
          credentialUrl.value = URL.createObjectURL(credBlob)
        } catch { /* 无权限时忽略 */ }
        try {
          const proofBlob = await getCompletionProof(detail.value.pickupId)
          completionProofUrl.value = URL.createObjectURL(proofBlob)
        } catch { /* 无凭证时忽略 */ }
      }
    }

    if (detail.value.publisher?.userId) {
      try {
        const blob = await getUserAvatar(detail.value.publisher.userId)
        publisherAvatarUrl.value = URL.createObjectURL(blob)
      } catch { /* ignore */ }
    }
    if (detail.value.acceptor?.userId) {
      try {
        const blob = await getUserAvatar(detail.value.acceptor.userId)
        acceptorAvatarUrl.value = URL.createObjectURL(blob)
      } catch { /* ignore */ }
    }

    if (detail.value.status === 'WAITING_PAYMENT' && isPublisher.value && userStore.isLoggedIn) {
      try {
        const payRes = await getPaymentEntry(detail.value.pickupId)
        expireAt.value = payRes?.expireAt || null
      } catch {
        expireAt.value = null
      }
      startCountdown()
    }

    if (detail.value.status === 'COMPLETED' && userStore.isLoggedIn) {
      try {
        const evalRes = await getEvaluationEligibility(route.params.id)
        canEvaluate.value = evalRes?.canEvaluate || false
      } catch { /* ignore */ }
    } else {
      canEvaluate.value = false
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
    if (res?.payEntry) {
      window.location.href = res.payEntry
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
  if (file.raw && file.raw.size > 5 * 1024 * 1024) {
    ElMessage.error('完成凭证图片不能超过5MB')
    proofUploadRef.value?.clearFiles()
    proofFile.value = null
    return
  }
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
    const { value: reason } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因',
      type: 'warning',
      inputValidator: (val) => {
        if (!val || !val.trim()) {
          return '取消原因不能为空'
        }
        return true
      }
    })
    actionLoading.value = true
    const res = await cancelPickup(detail.value.pickupId, { reason: reason?.trim() })
    if (res?.paymentStatus === 'REFUNDED') {
      ElMessage.success('订单已取消，退款已发起')
    } else {
      ElMessage.success('订单已取消')
    }
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
  if (evalForm.ratingLevel === 'BAD' && !evalForm.content?.trim()) {
    ElMessage.warning('差评必须填写评价内容')
    return
  }
  evalLoading.value = true
  try {
    await submitEvaluation(route.params.id, evalForm)
    ElMessage.success('评价成功')
    evalForm.ratingLevel = 'GOOD'
    evalForm.content = ''
    loadDetail()
  } catch {
    // error handled by interceptor
  } finally {
    evalLoading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  // 释放所有 Blob URL
  revokeBlobURL(credentialUrl.value)
  revokeBlobURL(completionProofUrl.value)
  revokeBlobURL(publisherAvatarUrl.value)
  revokeBlobURL(acceptorAvatarUrl.value)
})

onMounted(() => {
  loadDetail()
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
  flex-wrap: wrap;
  gap: 8px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cancel-reason {
  color: #f56c6c;
  font-size: 14px;
  font-weight: 500;
}

.reward {
  font-size: 24px;
  font-weight: bold;
  color: #e6a23c;
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

.countdown-section {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 8px;
}

.countdown-text {
  color: #e6a23c;
  font-size: 14px;
}

.countdown-text strong {
  font-weight: 700;
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>