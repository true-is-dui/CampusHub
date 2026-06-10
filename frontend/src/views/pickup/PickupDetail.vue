<template>
  <div v-loading="loading" class="pickup-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>代取详情</span>
      </template>
    </el-page-header>

    <template v-if="detail">
      <!-- 状态横幅 -->
      <div class="status-banner" :class="'status-' + detail.status">
        <div class="banner-left">
          <span class="status-icon">{{ statusIcon }}</span>
          <span class="status-text">{{ statusLabel }}</span>
          <span v-if="detail.status === 'CANCELLED' && detail.cancelReason" class="cancel-reason">
            — {{ cancelReasonLabel(detail.cancelReason) }}
          </span>
        </div>
        <div v-if="detail.rewardType === 'PAID'" class="banner-reward">
          {{ detail.rewardAmount }} <span class="reward-unit">积分</span>
        </div>
      </div>

      <!-- 物品描述 -->
      <el-card class="desc-card" shadow="never">
        <div class="desc-label">物品描述</div>
        <div class="desc-text">{{ detail.itemDescription || '无' }}</div>
      </el-card>

      <!-- 信息网格 -->
      <div class="info-grid">
        <div class="info-item">
          <el-icon class="info-icon"><Location /></el-icon>
          <div class="info-content">
            <span class="info-label">校区</span>
            <span class="info-value">{{ campusLabel(detail.campus) }}</span>
          </div>
        </div>
        <div class="info-item">
          <el-icon class="info-icon"><MapLocation /></el-icon>
          <div class="info-content">
            <span class="info-label">取件地点</span>
            <span class="info-value">{{ detail.pickupLocation }}</span>
          </div>
        </div>
        <div class="info-item">
          <el-icon class="info-icon"><Promotion /></el-icon>
          <div class="info-content">
            <span class="info-label">送达地点</span>
            <span class="info-value">{{ detail.deliveryLocation }}</span>
          </div>
        </div>
        <div class="info-item">
          <el-icon class="info-icon"><Clock /></el-icon>
          <div class="info-content">
            <span class="info-label">接单截止</span>
            <span class="info-value">{{ formatDateTime(detail.acceptDeadline) || '无' }}</span>
          </div>
        </div>
      </div>

      <!-- 时间线 -->
      <el-card class="timeline-card" shadow="never">
        <div class="timeline">
          <div class="timeline-item" :class="{ active: detail.createdAt }">
            <div class="timeline-dot"></div>
            <div class="timeline-line"></div>
            <div class="timeline-content">
              <span class="timeline-title">发布代取</span>
              <span class="timeline-time">{{ formatDateTime(detail.createdAt) }}</span>
            </div>
          </div>
          <div class="timeline-item" :class="{ active: detail.acceptedAt }">
            <div class="timeline-dot"></div>
            <div class="timeline-line"></div>
            <div class="timeline-content">
              <span class="timeline-title">有人接单</span>
              <span class="timeline-time">{{ detail.acceptedAt ? formatDateTime(detail.acceptedAt) : '等待中...' }}</span>
            </div>
          </div>
          <div v-if="detail.status !== 'CANCELLED'" class="timeline-item" :class="{ active: detail.completedAt }">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <span class="timeline-title">代取完成</span>
              <span class="timeline-time">{{ detail.completedAt ? formatDateTime(detail.completedAt) : '进行中...' }}</span>
            </div>
          </div>
          <div v-else class="timeline-item active cancelled">
            <div class="timeline-dot"></div>
            <div class="timeline-content">
              <span class="timeline-title">已取消</span>
              <span class="timeline-time">{{ formatDateTime(detail.updatedAt || detail.createdAt) }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 参与者 -->
      <div class="participants">
        <el-card class="participant-card" shadow="never">
          <div class="participant-main" @click="goUserPublicProfile(detail.publisher?.userId)">
            <el-avatar :size="48" :src="publisherAvatarUrl" icon="UserFilled" />
            <div class="participant-info">
              <el-tag size="small" type="primary">发布者</el-tag>
              <span class="participant-name clickable">{{ detail.publisher?.nickname || '匿名用户' }}</span>
            </div>
          </div>
          <div v-if="detail.status === 'COMPLETED'" class="participant-evaluation">
            <template v-if="getEvaluationByRevieweeRole('PUBLISHER')">
              <div class="eval-header compact">
                <el-tag :type="getRatingTag(getEvaluationByRevieweeRole('PUBLISHER').ratingLevel)" size="small">
                  {{ getRatingLabel(getEvaluationByRevieweeRole('PUBLISHER').ratingLevel) }}
                </el-tag>
                <span class="eval-time">{{ formatDateTime(getEvaluationByRevieweeRole('PUBLISHER').createdAt) }}</span>
              </div>
              <p class="eval-content">{{ getEvaluationByRevieweeRole('PUBLISHER').content || '未填写评价内容' }}</p>
            </template>
            <template v-else-if="shouldShowEvalFormForRole('PUBLISHER')">
              <div class="eval-form-title">评价发布者</div>
              <el-form :model="evalForm" label-width="48px" class="inline-eval-form">
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
                <el-button type="primary" :loading="evalLoading" @click="submitEval">提交评价</el-button>
              </el-form>
            </template>
            <div v-else class="eval-placeholder">{{ getNoEvaluationText('PUBLISHER') }}</div>
          </div>
        </el-card>
        <el-card v-if="detail.acceptor" class="participant-card" shadow="never">
          <div class="participant-main" @click="goUserPublicProfile(detail.acceptor?.userId)">
            <el-avatar :size="48" :src="acceptorAvatarUrl" icon="UserFilled" />
            <div class="participant-info">
              <el-tag size="small" type="success">接单者</el-tag>
              <span class="participant-name clickable">{{ detail.acceptor?.nickname || '匿名用户' }}</span>
            </div>
          </div>
          <div v-if="detail.status === 'COMPLETED'" class="participant-evaluation">
            <template v-if="getEvaluationByRevieweeRole('ACCEPTOR')">
              <div class="eval-header compact">
                <el-tag :type="getRatingTag(getEvaluationByRevieweeRole('ACCEPTOR').ratingLevel)" size="small">
                  {{ getRatingLabel(getEvaluationByRevieweeRole('ACCEPTOR').ratingLevel) }}
                </el-tag>
                <span class="eval-time">{{ formatDateTime(getEvaluationByRevieweeRole('ACCEPTOR').createdAt) }}</span>
              </div>
              <p class="eval-content">{{ getEvaluationByRevieweeRole('ACCEPTOR').content || '未填写评价内容' }}</p>
            </template>
            <template v-else-if="shouldShowEvalFormForRole('ACCEPTOR')">
              <div class="eval-form-title">评价接单者</div>
              <el-form :model="evalForm" label-width="48px" class="inline-eval-form">
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
                <el-button type="primary" :loading="evalLoading" @click="submitEval">提交评价</el-button>
              </el-form>
            </template>
            <div v-else class="eval-placeholder">{{ getNoEvaluationText('ACCEPTOR') }}</div>
          </div>
        </el-card>
      </div>

      <!-- 取件凭证 -->
      <el-card v-if="credentialUrl" class="image-card" shadow="never">
        <template #header><span>取件凭证</span></template>
        <el-image :src="credentialUrl" :preview-src-list="[credentialUrl]" fit="contain" class="proof-image" />
      </el-card>

      <!-- 完成凭证 -->
      <el-card v-if="completionProofUrl" class="image-card" shadow="never">
        <template #header><span>完成凭证</span></template>
        <el-image :src="completionProofUrl" :preview-src-list="[completionProofUrl]" fit="contain" class="proof-image" />
      </el-card>

      <!-- 操作按钮 -->
      <div class="actions">
        <template v-if="isPublisher && detail.status === 'WAITING_ACCEPT'">
          <el-button size="large" @click="handleCancel">取消订单</el-button>
        </template>
        <template v-if="!isPublisher && userStore.isApproved && detail.status === 'WAITING_ACCEPT'">
          <el-button type="primary" size="large" :loading="actionLoading" @click="handleAccept">接单</el-button>
        </template>
        <template v-if="isAcceptor && detail.status === 'IN_PROGRESS' && !completionProofUrl">
          <div class="upload-area">
            <el-upload ref="proofUploadRef" :auto-upload="false" :limit="1" accept="image/jpeg,image/png" :on-change="onProofChange">
              <el-button size="large">选择完成凭证</el-button>
            </el-upload>
            <div class="upload-tip">JPG/PNG，不超过5MB</div>
          </div>
          <el-button type="primary" size="large" :loading="actionLoading" :disabled="!proofFile" @click="handleUploadProof">
            上传完成凭证
          </el-button>
        </template>
        <template v-if="isPublisher && detail.status === 'IN_PROGRESS' && completionProofUrl">
          <el-button type="primary" size="large" :loading="actionLoading" @click="handleConfirm">确认完成</el-button>
        </template>
      </div>
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
  submitEvaluation,
  getPickupEvaluations,
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
const evaluations = ref([])
const canEvaluate = ref(false)
const evalForm = reactive({ ratingLevel: 'GOOD', content: '' })
const proofUploadRef = ref(null)

const credentialUrl = ref('')
const completionProofUrl = ref('')
const publisherAvatarUrl = ref('')
const acceptorAvatarUrl = ref('')

const campusMap = {
  GULOU: '鼓楼校区',
  XIANLIN: '仙林校区',
  SUZHOU: '苏州校区',
  PUKOU: '浦口校区'
}
const campusLabel = (code) => campusMap[code] || code

const cancelReasonMap = {
  USER_CANCELLED: '用户取消',
  ACCEPT_DEADLINE_EXPIRED: '接单截止超时',
  SYSTEM_CANCELLED: '系统取消'
}
const cancelReasonLabel = (reason) => cancelReasonMap[reason] || reason

const statusMap = {
  WAITING_ACCEPT: '待接单',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusTagType = computed(() => {
  const map = {
    WAITING_ACCEPT: 'info',
    IN_PROGRESS: '',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[detail.value?.status] || 'info'
})

const statusLabel = computed(() => statusMap[detail.value?.status] || detail.value?.status)

const statusIcon = computed(() => {
  const map = { WAITING_ACCEPT: '⏳', IN_PROGRESS: '🚀', COMPLETED: '✅', CANCELLED: '❌' }
  return map[detail.value?.status] || '📦'
})

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

function getCurrentUserRole() {
  if (isPublisher.value) return 'PUBLISHER'
  if (isAcceptor.value) return 'ACCEPTOR'
  return null
}

function getEvaluationTargetRole() {
  if (isPublisher.value) return 'ACCEPTOR'
  if (isAcceptor.value) return 'PUBLISHER'
  return null
}

function getEvaluationByRevieweeRole(role) {
  return evaluations.value.find(item => item.revieweeRoleInBusiness === role) || null
}

function shouldShowEvalFormForRole(role) {
  return canEvaluate.value && getEvaluationTargetRole() === role
}

function getNoEvaluationText(role) {
  if (getCurrentUserRole() === role) {
    return '对方暂未评价'
  }
  return '暂未评价'
}

// 格式化日期时间为 YYYY-MM-DD HH:mm
function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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
    evaluations.value = []

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
        if (detail.value.status === 'IN_PROGRESS' || detail.value.status === 'COMPLETED') {
          try {
            const proofConfig = detail.value.status === 'IN_PROGRESS'
                ? { silentReasons: ['COMPLETION_PROOF_NOT_AVAILABLE'] }
                : {}
            const proofBlob = await getCompletionProof(detail.value.pickupId, proofConfig)
            completionProofUrl.value = URL.createObjectURL(proofBlob)
          } catch { /* 无凭证时按状态由拦截器决定是否提示 */ }
        }
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

    if (detail.value.status === 'COMPLETED' && userStore.isLoggedIn) {
      try {
        evaluations.value = await getPickupEvaluations(route.params.id)
      } catch { /* ignore */ }
      try {
        const evalRes = await getEvaluationEligibility(route.params.id)
        canEvaluate.value = evalRes?.canEvaluate || false
      } catch { /* ignore */ }
    } else if (detail.value.status === 'COMPLETED') {
      try {
        evaluations.value = await getPickupEvaluations(route.params.id)
      } catch { /* ignore */ }
      canEvaluate.value = false
    } else {
      canEvaluate.value = false
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
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
    await cancelPickup(detail.value.pickupId, { reason: reason?.trim() })
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
  if (evalForm.ratingLevel === 'BAD' && !evalForm.content?.trim()) {
    ElMessage.warning('差评必须填写评价内容')
    return
  }
  evalLoading.value = true
  try {
    await submitEvaluation(route.params.id, evalForm)
    ElMessage.success('评价成功')
    canEvaluate.value = false
    evalForm.ratingLevel = 'GOOD'
    evalForm.content = ''
    await loadDetail()
  } catch {
    // error handled by interceptor
  } finally {
    evalLoading.value = false
  }
}

onUnmounted(() => {
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
  padding-bottom: 24px;
}

/* 状态横幅 */
.status-banner {
  margin-top: 16px;
  padding: 20px 24px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}

.status-WAITING_ACCEPT { background: linear-gradient(135deg, #409eff, #66b1ff); }
.status-IN_PROGRESS { background: linear-gradient(135deg, #e6a23c, #f0c78a); }
.status-COMPLETED { background: linear-gradient(135deg, #67c23a, #95d475); }
.status-CANCELLED { background: linear-gradient(135deg, #909399, #b1b3b8); }

.banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-icon {
  font-size: 24px;
}

.status-text {
  font-size: 20px;
  font-weight: 600;
}

.cancel-reason {
  font-size: 14px;
  opacity: 0.9;
}

.banner-reward {
  font-size: 32px;
  font-weight: 700;
}

.reward-unit {
  font-size: 14px;
  font-weight: 400;
  opacity: 0.8;
}

/* 物品描述 */
.desc-card {
  margin-top: 16px;
}

.desc-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.desc-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.info-icon {
  font-size: 20px;
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}

/* 时间线 */
.timeline-card {
  margin-top: 16px;
}

.timeline {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  position: relative;
  padding-bottom: 24px;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #dcdfe6;
  flex-shrink: 0;
  margin-top: 4px;
  z-index: 1;
}

.timeline-item.active .timeline-dot {
  background: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}

.timeline-item.cancelled .timeline-dot {
  background: #f56c6c;
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.2);
}

.timeline-line {
  position: absolute;
  left: 5px;
  top: 16px;
  width: 2px;
  height: calc(100% - 10px);
  background: #e4e7ed;
}

.timeline-item.active .timeline-line {
  background: #409eff;
}

.timeline-item.cancelled .timeline-line {
  background: #f56c6c;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.timeline-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.timeline-item:not(.active) .timeline-title {
  color: #c0c4cc;
}

.timeline-time {
  font-size: 13px;
  color: #909399;
}

/* 参与者 */
.participants {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.participant-card {
  transition: box-shadow 0.2s;
}

.participant-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.participant-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 14px;
  padding: 16px;
}

.participant-main {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}

.participant-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.participant-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.participant-name.clickable {
  color: #409eff;
}

.participant-name.clickable:hover {
  text-decoration: underline;
}

.participant-evaluation {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

/* 图片卡片 */
.image-card {
  margin-top: 16px;
}

.proof-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.eval-header.compact {
  justify-content: space-between;
}

.eval-content {
  color: #606266;
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
}

.eval-time {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.eval-placeholder {
  color: #909399;
  font-size: 14px;
}

.eval-form-title {
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
}

.inline-eval-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}
</style>
