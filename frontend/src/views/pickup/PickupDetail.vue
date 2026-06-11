<template>
  <div v-loading="loading" class="pickup-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>代取详情</span>
      </template>
    </el-page-header>

    <template v-if="detail">
      <div class="detail-body">
        <div class="timeline-panel">
          <el-card class="reward-card" shadow="never">
            <div class="reward-card-shell">
              <span class="reward-card-bg-icons" aria-hidden="true">
                <el-icon><Coin /></el-icon>
                <el-icon><Coin /></el-icon>
                <el-icon><Coin /></el-icon>
              </span>
              <div class="reward-card-label">报酬</div>
              <div v-if="detail.rewardType === 'PAID'" class="reward-card-value">
                {{ detail.rewardAmount }}
              </div>
              <div v-else class="reward-card-value reward-card-value--free">无</div>
            </div>
          </el-card>

          <div class="status-banner timeline-status" :class="'status-' + detail.status">
            <div class="banner-left">
              <el-icon class="status-icon">
                <CircleCloseFilled v-if="detail.status === 'CANCELLED'" />
                <CircleCheckFilled v-else-if="detail.status === 'COMPLETED'" />
                <Promotion v-else-if="detail.status === 'IN_PROGRESS'" />
                <Clock v-else />
              </el-icon>
              <span class="status-text">{{ statusLabel }}</span>
            </div>
          </div>

          <!-- 时间线 -->
          <el-card class="timeline-card" shadow="never">
            <div class="timeline" :class="['timeline-status-' + detail.status, { 'is-cancelled': detail.status === 'CANCELLED' }]">
              <div class="timeline-item cancelled reverse" :class="{ active: detail.status === 'CANCELLED' }">
                <div class="timeline-dot"></div>
                <div class="timeline-line timeline-line--reverse timeline-line--dashed"></div>
                <div class="timeline-content">
                  <span class="timeline-title">已取消</span>
                  <span v-if="detail.status === 'WAITING_ACCEPT' && detail.acceptDeadline" class="timeline-time timeline-time--danger">
                    {{ formatDateTime(detail.acceptDeadline) }}<br>
                    自动取消
                  </span>
                  <span v-if="detail.cancelledAt" class="timeline-time">{{ formatDateTime(detail.cancelledAt) }}</span>
                </div>
              </div>
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
                  <span class="timeline-title">进行中</span>
                  <span class="timeline-time">{{ detail.acceptedAt ? formatDateTime(detail.acceptedAt) : '未开始' }}</span>
                </div>
              </div>
              <div class="timeline-item" :class="{ active: detail.completedAt }">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <span class="timeline-title">已完成</span>
                  <span class="timeline-time">{{ detail.completedAt ? formatDateTime(detail.completedAt) : '未完成' }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <el-card class="campus-card" shadow="never">
          <div class="campus-card-content">
            <el-icon class="campus-icon">
              <OfficeBuilding v-if="detail.campus === 'GULOU'" />
              <School v-else-if="detail.campus === 'XIANLIN'" />
              <Guide v-else-if="detail.campus === 'SUZHOU'" />
              <Place v-else-if="detail.campus === 'PUKOU'" />
              <Location v-else />
            </el-icon>
            <div class="campus-info">
              <span class="campus-label">校区</span>
              <span class="campus-value">{{ campusLabel(detail.campus) }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="route-card" shadow="never">
          <div class="route-card-content">
            <div
                class="route-stop route-stop--pickup"
                :class="{ 'route-stop--expanded': expandedRouteStops.delivery }"
                role="button"
                tabindex="0"
                :aria-expanded="expandedRouteStops.delivery"
                @click.stop="toggleRouteStop('delivery')"
                @keydown.enter.prevent="toggleRouteStop('delivery')"
                @keydown.space.prevent="toggleRouteStop('delivery')"
            >
              <el-icon class="route-icon"><MapLocation /></el-icon>
              <div class="route-info">
                <span class="route-label">送达地点</span>
                <span class="route-value">{{ detail.deliveryLocation }}</span>
              </div>
              <el-icon class="route-toggle-icon"><ArrowDown /></el-icon>
            </div>
            <svg class="route-curve" viewBox="0 0 128 320" preserveAspectRatio="none" aria-hidden="true">
              <path
                  d="M 21 0
                     C 96 34, 118 76, 52 104
                     C -18 134, 10 38, 76 70
                     C 150 106, 72 178, 24 166
                     C -18 156, 12 236, 82 216
                     C 128 202, 94 292, 21 320"
              />
            </svg>
            <div
                class="route-stop route-stop--delivery"
                :class="{ 'route-stop--expanded': expandedRouteStops.pickup }"
                role="button"
                tabindex="0"
                :aria-expanded="expandedRouteStops.pickup"
                @click.stop="toggleRouteStop('pickup')"
                @keydown.enter.prevent="toggleRouteStop('pickup')"
                @keydown.space.prevent="toggleRouteStop('pickup')"
            >
              <el-icon class="route-icon"><Promotion /></el-icon>
              <div class="route-info">
                <span class="route-label">取件地点</span>
                <span class="route-value">{{ detail.pickupLocation }}</span>
              </div>
              <el-icon class="route-toggle-icon"><ArrowDown /></el-icon>
            </div>
          </div>
        </el-card>

        <div class="detail-main">
          <div class="summary-cards">
            <!-- 物品描述 -->
            <el-card class="desc-card" shadow="never">
              <div class="desc-label">物品描述</div>
              <div class="desc-text">{{ detail.itemDescription || '无' }}</div>
            </el-card>
          </div>

          <!-- 参与者 -->
          <div class="participants">
            <div class="participant-column">
              <el-card class="participant-card participant-card--publisher" shadow="never">
                <div class="participant-main">
                  <div class="participant-identity participant-identity--clickable" @click="goUserPublicProfile(detail.publisher?.userId)">
                    <el-avatar :size="48" :src="publisherAvatarUrl" icon="UserFilled" />
                    <div class="participant-info">
                      <span class="participant-name clickable">{{ detail.publisher?.nickname || '匿名用户' }}</span>
                      <span class="role-pill role-pill--publisher">发布者</span>
                    </div>
                  </div>
                  <div class="participant-side-actions">
                    <el-button
                        v-if="isPublisher && detail.status === 'WAITING_ACCEPT'"
                        type="danger"
                        size="small"
                        :loading="actionLoading"
                        @click.stop="handleCancel"
                    >
                      取消订单
                    </el-button>
                    <RatingTag
                        v-if="getEvaluationByRevieweeRole('PUBLISHER')"
                        :level="getEvaluationByRevieweeRole('PUBLISHER').ratingLevel"
                        size="large"
                        class="eval-rating-tag"
                    />
                  </div>
                </div>
                <div v-if="detail.status === 'COMPLETED'" class="participant-evaluation">
                  <template v-if="getEvaluationByRevieweeRole('PUBLISHER')">
                    <p class="eval-content">{{ getEvaluationByRevieweeRole('PUBLISHER').content || '未填写评价内容' }}</p>
                    <p class="eval-time">{{ formatDateTime(getEvaluationByRevieweeRole('PUBLISHER').createdAt) }}</p>
                  </template>
                  <template v-else-if="shouldShowEvalFormForRole('PUBLISHER')">
                    <div class="eval-form-title">评价发布者</div>
                    <el-form :model="evalForm" label-width="48px" class="inline-eval-form">
                      <el-form-item label="评分">
                        <el-radio-group v-model="evalForm.ratingLevel">
                          <el-radio value="GOOD" class="eval-radio eval-radio--good"><RatingLevelLabel level="GOOD" /></el-radio>
                          <el-radio value="NEUTRAL" class="eval-radio eval-radio--neutral"><RatingLevelLabel level="NEUTRAL" /></el-radio>
                          <el-radio value="BAD" class="eval-radio eval-radio--bad"><RatingLevelLabel level="BAD" /></el-radio>
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
                      <div class="eval-form-actions">
                        <el-button type="primary" :loading="evalLoading" @click="submitEval">提交评价</el-button>
                      </div>
                    </el-form>
                  </template>
                  <div v-else class="eval-placeholder">{{ getNoEvaluationText('PUBLISHER') }}</div>
                </div>
              </el-card>
              <el-card class="proof-card proof-card--publisher" shadow="never">
                <div class="proof-title">取件凭证</div>
                <el-image
                    v-if="credentialUrl"
                    :src="credentialUrl"
                    :preview-src-list="[credentialUrl]"
                    hide-on-click-modal
                    fit="contain"
                    class="proof-image"
                />
                <el-empty v-else :description="credentialEmptyText" :image-size="70" />
              </el-card>
            </div>
            <div class="participant-column">
              <el-card class="participant-card participant-card--acceptor" shadow="never">
                <div
                    class="participant-main"
                    :class="{ 'participant-main--placeholder': !detail.acceptor }"
                >
                  <div class="participant-identity" :class="{ 'participant-identity--clickable': detail.acceptor }" @click="goUserPublicProfile(detail.acceptor?.userId)">
                    <el-avatar :size="48" :src="detail.acceptor ? acceptorAvatarUrl : ''" icon="UserFilled" />
                    <div class="participant-info">
                      <span class="participant-name" :class="{ clickable: detail.acceptor, 'participant-name--placeholder': !detail.acceptor }">
                        {{ detail.acceptor?.nickname || '等待接单' }}
                      </span>
                      <span class="role-pill role-pill--acceptor">接单者</span>
                    </div>
                  </div>
                  <div class="participant-side-actions">
                    <el-button
                        v-if="!isPublisher && userStore.isApproved && detail.status === 'WAITING_ACCEPT'"
                        type="primary"
                        size="small"
                        :loading="actionLoading"
                        @click="handleAccept"
                    >
                      接单
                    </el-button>
                    <RatingTag
                        v-if="detail.acceptor && getEvaluationByRevieweeRole('ACCEPTOR')"
                        :level="getEvaluationByRevieweeRole('ACCEPTOR').ratingLevel"
                        size="large"
                        class="eval-rating-tag"
                    />
                  </div>
                </div>
                <div v-if="detail.acceptor && detail.status === 'COMPLETED'" class="participant-evaluation">
                  <template v-if="getEvaluationByRevieweeRole('ACCEPTOR')">
                    <p class="eval-content">{{ getEvaluationByRevieweeRole('ACCEPTOR').content || '未填写评价内容' }}</p>
                    <p class="eval-time">{{ formatDateTime(getEvaluationByRevieweeRole('ACCEPTOR').createdAt) }}</p>
                  </template>
                  <template v-else-if="shouldShowEvalFormForRole('ACCEPTOR')">
                    <div class="eval-form-title">评价接单者</div>
                    <el-form :model="evalForm" label-width="48px" class="inline-eval-form">
                      <el-form-item label="评分">
                        <el-radio-group v-model="evalForm.ratingLevel">
                          <el-radio value="GOOD" class="eval-radio eval-radio--good"><RatingLevelLabel level="GOOD" /></el-radio>
                          <el-radio value="NEUTRAL" class="eval-radio eval-radio--neutral"><RatingLevelLabel level="NEUTRAL" /></el-radio>
                          <el-radio value="BAD" class="eval-radio eval-radio--bad"><RatingLevelLabel level="BAD" /></el-radio>
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
                      <div class="eval-form-actions">
                        <el-button type="primary" :loading="evalLoading" @click="submitEval">提交评价</el-button>
                      </div>
                    </el-form>
                  </template>
                  <div v-else class="eval-placeholder">{{ getNoEvaluationText('ACCEPTOR') }}</div>
                </div>
              </el-card>
              <el-card class="proof-card proof-card--acceptor" shadow="never">
                <div class="proof-title">完成凭证</div>
                <el-image
                    v-if="completionProofUrl"
                    :src="completionProofUrl"
                    :preview-src-list="[completionProofUrl]"
                    hide-on-click-modal
                    fit="contain"
                    class="proof-image"
                />
                <div
                    v-else-if="isAcceptor && detail.status === 'IN_PROGRESS'"
                    v-loading="actionLoading"
                    class="proof-upload-panel"
                >
                  <el-upload
                      ref="proofUploadRef"
                      class="completion-proof-upload"
                      :class="{ 'completion-proof-upload--filled': proofFileList.length > 0 }"
                      :auto-upload="false"
                      :limit="1"
                      accept="image/jpeg,image/png"
                      list-type="picture-card"
                      :disabled="actionLoading"
                      :file-list="proofFileList"
                      :on-change="onProofChange"
                      :on-remove="onProofRemove"
                  >
                    <el-icon v-if="proofFileList.length === 0"><Plus /></el-icon>
                  </el-upload>
                  <div class="upload-tip">JPG/PNG，不超过5MB，最多一张</div>
                  <el-button
                      class="proof-upload-confirm"
                      type="primary"
                      :loading="actionLoading"
                      :disabled="!proofFile"
                      @click="handleUploadProof"
                  >
                    确定上传
                  </el-button>
                </div>
                <el-empty
                    v-else
                    :description="completionProofLoadFailed ? '无法加载完成凭证' : '等待上传完成凭证'"
                    :image-size="70"
                />
              </el-card>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div v-if="shouldShowBottomActions" class="actions">
            <template v-if="isPublisher && detail.status === 'IN_PROGRESS' && completionProofUrl">
              <el-button type="primary" size="large" :loading="actionLoading" @click="handleConfirm">确认完成</el-button>
            </template>
          </div>
        </div>
      </div>

    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
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
import RatingLevelLabel from '@/components/RatingLevelLabel.vue'
import RatingTag from '@/components/RatingTag.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const actionLoading = ref(false)
const evalLoading = ref(false)
const detail = ref(null)
const proofFile = ref(null)
const proofFileList = ref([])
const evaluations = ref([])
const canEvaluate = ref(false)
const evalForm = reactive({ ratingLevel: 'GOOD', content: '' })
const expandedRouteStops = reactive({ delivery: false, pickup: false })
const proofUploadRef = ref(null)

const credentialUrl = ref('')
const completionProofUrl = ref('')
const publisherAvatarUrl = ref('')
const acceptorAvatarUrl = ref('')
const credentialLoadFailed = ref(false)
const completionProofLoadFailed = ref(false)
const myPickupsReturnStateKey = 'campushub:my-pickups:return-state'

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

const isPublisher = computed(() => {
  return userStore.userInfo?.userId && detail.value && userStore.userInfo.userId === detail.value.publisher?.userId
})

const isAcceptor = computed(() => {
  return userStore.userInfo?.userId && detail.value && userStore.userInfo.userId === detail.value.acceptor?.userId
})

const shouldShowBottomActions = computed(() => {
  if (!detail.value) return false
  return isPublisher.value && detail.value.status === 'IN_PROGRESS' && completionProofUrl.value
})

const credentialEmptyText = computed(() => {
  if (credentialLoadFailed.value) return '无法加载取件凭证'
  if (isPublisher.value || isAcceptor.value) return '等待加载取件凭证'
  return '接单后可查看取件凭证'
})

function goUserPublicProfile(userId) {
  if (userId) router.push(`/user/${userId}`)
}

function toggleRouteStop(type) {
  const shouldExpand = !expandedRouteStops[type]
  expandedRouteStops.delivery = false
  expandedRouteStops.pickup = false
  expandedRouteStops[type] = shouldExpand
}

function closeRouteStops() {
  expandedRouteStops.delivery = false
  expandedRouteStops.pickup = false
}

function handleDocumentClick() {
  if (expandedRouteStops.delivery || expandedRouteStops.pickup) {
    closeRouteStops()
  }
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

function isMissingResourceError(error) {
  return error?.parsedResponse?.errors?.reason === 'RESOURCE_NOT_FOUND'
      || error?.response?.data?.errors?.reason === 'RESOURCE_NOT_FOUND'
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
    credentialLoadFailed.value = false
    completionProofLoadFailed.value = false
    proofFile.value = null
    proofFileList.value = []
    expandedRouteStops.delivery = false
    expandedRouteStops.pickup = false
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
        } catch (error) {
          credentialLoadFailed.value = isMissingResourceError(error)
        }
        if (detail.value.status === 'IN_PROGRESS' || detail.value.status === 'COMPLETED') {
          try {
            const proofConfig = detail.value.status === 'IN_PROGRESS'
                ? { silentReasons: ['COMPLETION_PROOF_NOT_AVAILABLE'] }
                : {}
            const proofBlob = await getCompletionProof(detail.value.pickupId, proofConfig)
            completionProofUrl.value = URL.createObjectURL(proofBlob)
          } catch (error) {
            completionProofLoadFailed.value = isMissingResourceError(error)
          }
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
    proofFileList.value = []
    return
  }
  proofFile.value = file.raw
  proofFileList.value = [file]
}

function onProofRemove() {
  proofFile.value = null
  proofFileList.value = []
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
    proofFileList.value = []
    proofUploadRef.value?.clearFiles()
    loadDetail()
  } catch {
    proofFile.value = null
    proofFileList.value = []
    proofUploadRef.value?.clearFiles()
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
  document.removeEventListener('click', handleDocumentClick)
  revokeBlobURL(credentialUrl.value)
  revokeBlobURL(completionProofUrl.value)
  revokeBlobURL(publisherAvatarUrl.value)
  revokeBlobURL(acceptorAvatarUrl.value)
})

onBeforeRouteLeave((to) => {
  if (to.path !== '/my-pickups') {
    try {
      sessionStorage.removeItem(myPickupsReturnStateKey)
    } catch {
      // Storage failure should not block navigation.
    }
  }
})

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  loadDetail()
})
</script>

<style scoped>
.pickup-detail {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 24px;
}

.detail-body {
  display: block;
  margin-top: 16px;
}

.detail-main {
  min-width: 0;
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
.status-CANCELLED { background: linear-gradient(135deg, #f56c6c, #f89898); }

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

.timeline-status {
  align-items: flex-start;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
  margin: 0 0 10px;
  padding: 10px 12px;
  border-radius: 8px;
  min-height: 40px;
  overflow: hidden;
}

.timeline-status .banner-left {
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.timeline-status .status-icon {
  font-size: 22px;
  line-height: 1;
}

.timeline-status .status-text {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
  max-width: 132px;
  opacity: 1;
  overflow: hidden;
  white-space: nowrap;
}

.timeline-status .cancel-reason {
  flex-basis: 100%;
  font-size: 12px;
  line-height: 1.4;
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transition: max-height 0.2s ease, opacity 0.15s ease;
}

.summary-cards {
  display: block;
}

.summary-cards > .el-card {
  min-width: 0;
}

.summary-cards :deep(.el-card__body) {
  height: 100%;
}

/* 物品描述 */
.desc-card {
  margin-top: 0;
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

.timeline-panel {
  position: fixed;
  top: 96px;
  bottom: 32px;
  left: max(16px, calc((100vw - 800px) / 2 - 204px));
  width: 180px;
  z-index: 8;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.reward-card {
  flex: 0 0 88px;
  height: 88px;
  width: 100%;
  margin-bottom: 10px;
  border-color: #f4dfad;
  background: linear-gradient(135deg, #fff8e6 0%, #fffdf5 100%);
  overflow: hidden;
}

.reward-card :deep(.el-card__body) {
  height: 100%;
  box-sizing: border-box;
  padding: 12px 16px;
}

.reward-card-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 100%;
  min-height: 0;
  min-width: 0;
}

.reward-card-bg-icons {
  position: absolute;
  left: 50%;
  bottom: -12px;
  display: inline-flex;
  align-items: center;
  transform: translateX(-50%);
  color: #c78912;
  opacity: 0.13;
  pointer-events: none;
}

.reward-card-bg-icons .el-icon {
  margin-left: -18px;
  font-size: 70px;
}

.reward-card-bg-icons .el-icon:first-child {
  margin-left: 0;
}

.reward-card-label {
  position: relative;
  z-index: 1;
  color: #9a6a12;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  text-align: center;
}

.reward-card-value {
  position: relative;
  z-index: 1;
  max-width: 100%;
  overflow: hidden;
  color: #bd8214;
  font-size: 40px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reward-card-value--free {
  font-size: 34px;
  color: #bd8214;
}

.campus-card {
  position: fixed;
  top: 96px;
  right: max(16px, calc((100vw - 800px) / 2 - 216px));
  width: 192px;
  z-index: 8;
}

.campus-card :deep(.el-card__body) {
  padding: 20px;
}

.campus-card-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.campus-icon {
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 28px;
}

.campus-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.campus-label {
  font-size: 13px;
  color: #909399;
}

.campus-value {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.route-card {
  position: fixed;
  right: max(16px, calc((100vw - 800px) / 2 - 216px));
  top: 204px;
  bottom: 32px;
  width: 192px;
  z-index: 8;
  overflow: visible;
}

.route-card :deep(.el-card__body) {
  height: 100%;
  padding: 20px;
  box-sizing: border-box;
  overflow: visible;
}

.route-card-content {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 260px;
  height: 100%;
}

.route-stop {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
  width: 100%;
  max-height: 82px;
  margin: -8px;
  padding: 8px;
  border-radius: 8px;
  background: transparent;
  box-sizing: border-box;
  cursor: pointer;
  overflow: hidden;
  transition:
      width 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      max-height 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      min-height 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      padding 0.78s cubic-bezier(0.22, 1, 0.36, 1),
      background-color 0.22s ease,
      box-shadow 0.22s ease,
      transform 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.route-stop:not(.route-stop--expanded):hover,
.route-stop:not(.route-stop--expanded):focus-visible {
  background: transparent;
}

.route-stop:focus-visible {
  outline: 2px solid rgba(64, 158, 255, 0.45);
  outline-offset: 2px;
}

.route-stop--expanded {
  z-index: 3;
  width: 304px;
  min-height: 132px;
  max-height: 320px;
  padding: 14px 34px 16px 16px;
  background: #fff;
  box-shadow: 0 16px 30px rgba(37, 48, 78, 0.16);
  transform: translateX(-112px);
}

.route-icon {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #fff0f6;
  color: #ff6f9f;
  font-size: 24px;
}

.route-stop--delivery .route-icon {
  background: #ecf5ff;
  color: #409eff;
}

.route-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1 1 auto;
  min-width: 0;
}

.route-label {
  font-size: 12px;
  color: #909399;
}

.route-value {
  display: -webkit-box;
  max-height: 43.5px;
  overflow: hidden;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.45;
  text-overflow: ellipsis;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  transition: max-height 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.route-stop--expanded .route-value {
  max-height: 232px;
  -webkit-line-clamp: unset;
}

.route-toggle-icon {
  position: absolute;
  top: 10px;
  right: 8px;
  color: #c0c4cc;
  font-size: 14px;
  transition:
      color 0.22s ease,
      transform 0.78s cubic-bezier(0.22, 1, 0.36, 1);
}

.route-stop:hover .route-toggle-icon,
.route-stop:focus-visible .route-toggle-icon,
.route-stop--expanded .route-toggle-icon {
  color: #409eff;
}

.route-stop--expanded .route-toggle-icon {
  transform: rotate(180deg);
}

.route-curve {
  position: absolute;
  left: 0;
  top: 42px;
  width: 128px;
  height: calc(100% - 84px);
  pointer-events: none;
}

.route-curve path {
  fill: none;
  stroke: #c0c4cc;
  stroke-width: 2;
  stroke-dasharray: 6 6;
  stroke-linecap: round;
}

/* 时间线 */
.timeline-card {
  width: 100%;
  flex: 1 1 auto;
  min-height: 260px;
  overflow: hidden;
}

.timeline-card :deep(.el-card__body) {
  height: 100%;
  box-sizing: border-box;
  padding: 14px 14px;
}

.timeline {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 0;
  height: 100%;
}

.timeline-item {
  flex: 1 1 0;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  min-height: 64px;
  padding-bottom: 0;
}

.timeline-item:last-child {
  flex: 0 0 auto;
  min-height: 0;
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

.timeline.timeline-status-IN_PROGRESS .timeline-item.active:not(.cancelled) .timeline-dot {
  background: #e6a23c;
  box-shadow: 0 0 0 3px rgba(230, 162, 60, 0.2);
}

.timeline.timeline-status-COMPLETED .timeline-item.active:not(.cancelled) .timeline-dot {
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.2);
}

.timeline-item.cancelled.active .timeline-dot {
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

.timeline.timeline-status-IN_PROGRESS .timeline-item.active:not(.cancelled) .timeline-line {
  background: #e6a23c;
}

.timeline.timeline-status-COMPLETED .timeline-item.active:not(.cancelled) .timeline-line {
  background: #67c23a;
}

.timeline.is-cancelled .timeline-item:not(.cancelled) .timeline-line {
  background: #e4e7ed;
}

.timeline.is-cancelled .timeline-item:not(.cancelled).active .timeline-dot {
  background: #f56c6c;
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.2);
}

.timeline-item.cancelled .timeline-line {
  background: #f56c6c;
}

.timeline-item.cancelled.active .timeline-line--dashed {
  width: 2px;
  border-left: 0;
  background: #f56c6c !important;
}

.timeline-line--reverse {
  background: #e4e7ed !important;
}

.timeline-line--dashed {
  width: 0;
  border-left: 2px dashed #dcdfe6;
  background: transparent !important;
}

.timeline-item.reverse {
  padding-bottom: 0;
}

.timeline-item.reverse .timeline-line {
  top: 16px;
  height: calc(100% - 12px);
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 0 0 130px;
  max-width: 130px;
  overflow: hidden;
}

.timeline-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
}

.timeline-item:not(.active) .timeline-title {
  color: #c0c4cc;
}

.timeline-time {
  font-size: 13px;
  color: #909399;
  line-height: 1.35;
}

.timeline-time--danger {
  color: #f56c6c;
}

/* 参与者 */
.participants {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    "publisher acceptor"
    "publisher-proof acceptor-proof";
  align-items: stretch;
  gap: 12px;
  margin-top: 16px;
}

.participant-column {
  display: contents;
}

.participant-card--publisher { grid-area: publisher; }
.participant-card--acceptor { grid-area: acceptor; }
.proof-card--publisher { grid-area: publisher-proof; }
.proof-card--acceptor { grid-area: acceptor-proof; }

.participant-card {
  transition: box-shadow 0.2s;
  min-width: 0;
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
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.participant-main--placeholder {
  cursor: default;
}

.participant-identity {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.participant-identity--clickable {
  cursor: pointer;
}

.participant-side-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
}

.participant-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.participant-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.participant-name.clickable {
  color: #409eff;
}

.participant-name.clickable:hover {
  text-decoration: underline;
}

.participant-name--placeholder {
  color: #909399;
}

.participant-evaluation {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.role-pill {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
  white-space: nowrap;
}

.role-pill--publisher {
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 58%, #ffc0cf 100%);
}

.role-pill--acceptor {
  background: linear-gradient(135deg, #4f8cff 0%, #6db7ff 58%, #a5dcff 100%);
}

.eval-rating-tag {
  flex: 0 0 auto;
}

.eval-rating-tag :deep(.rating-level-icon) {
  width: 18px;
  height: 18px;
}

.proof-card {
  min-width: 0;
  overflow-x: hidden;
}

.proof-card :deep(.el-card__body) {
  min-width: 0;
  overflow-x: hidden;
  box-sizing: border-box;
  padding: 16px;
}

.proof-title {
  margin-bottom: 10px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.proof-image {
  display: block;
  width: 100%;
  max-width: 100%;
  max-height: 360px;
  box-sizing: border-box;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.proof-image :deep(.el-image__inner) {
  max-width: 100%;
}

.proof-upload-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 176px;
}

.completion-proof-upload {
  display: flex;
  justify-content: center;
  min-height: 112px;
  line-height: 0;
}

.completion-proof-upload :deep(.el-upload--picture-card),
.completion-proof-upload :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 112px;
  height: 112px;
}

.completion-proof-upload :deep(.el-upload-list--picture-card) {
  display: grid;
  grid-template-columns: 112px;
  justify-content: center;
  margin: 0;
  line-height: 0;
}

.completion-proof-upload :deep(.el-upload-list--picture-card .el-upload-list__item) {
  margin: 0;
}

.completion-proof-upload--filled :deep(.el-upload--picture-card) {
  display: none;
}

.proof-upload-confirm {
  margin-top: 12px;
}

.eval-content {
  color: #606266;
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.eval-time {
  color: #909399;
  font-size: 13px;
  margin: 8px 0 0;
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

.inline-eval-form :deep(.eval-radio) {
  margin-right: 16px;
}

.inline-eval-form :deep(.eval-radio .el-radio__label) {
  display: inline-flex;
  align-items: center;
  color: inherit;
  font-weight: 600;
}

.inline-eval-form :deep(.eval-radio--good) {
  color: #67c23a;
}

.inline-eval-form :deep(.eval-radio--neutral) {
  color: #e6a23c;
}

.inline-eval-form :deep(.eval-radio--bad) {
  color: #f56c6c;
}

.inline-eval-form :deep(.eval-radio--good .el-radio__input.is-checked .el-radio__inner) {
  border-color: #67c23a;
  background: #67c23a;
}

.inline-eval-form :deep(.eval-radio--neutral .el-radio__input.is-checked .el-radio__inner) {
  border-color: #e6a23c;
  background: #e6a23c;
}

.inline-eval-form :deep(.eval-radio--bad .el-radio__input.is-checked .el-radio__inner) {
  border-color: #f56c6c;
  background: #f56c6c;
}

.inline-eval-form :deep(.eval-radio--good .el-radio__input.is-checked + .el-radio__label) {
  color: #67c23a;
}

.inline-eval-form :deep(.eval-radio--neutral .el-radio__input.is-checked + .el-radio__label) {
  color: #e6a23c;
}

.inline-eval-form :deep(.eval-radio--bad .el-radio__input.is-checked + .el-radio__label) {
  color: #f56c6c;
}

.eval-form-actions {
  display: flex;
  justify-content: flex-end;
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

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  text-align: center;
}

@media (max-width: 1180px) {
  .timeline-panel,
  .campus-card,
  .route-card {
    position: static;
    width: 100%;
    margin-bottom: 16px;
  }

  .timeline-panel {
    bottom: auto;
    display: block;
  }

  .timeline-status {
    align-items: flex-start;
    gap: 6px;
    margin-bottom: 10px;
  }

  .reward-card :deep(.el-card__body) {
    padding: 12px 16px;
  }

  .reward-card-value {
    max-width: none;
  }

  .timeline-status .banner-left {
    align-items: flex-start;
    gap: 8px;
  }

  .timeline-status .status-text {
    max-width: none;
    opacity: 1;
  }

  .timeline-status .cancel-reason {
    max-height: 48px;
    opacity: 1;
  }

  .route-stop--expanded {
    width: 100%;
    transform: none;
  }

  .timeline-card :deep(.el-card__body) {
    height: auto;
    padding: 14px 14px;
  }

  .timeline-card {
    height: auto;
  }

  .timeline {
    min-height: 260px;
    height: auto;
  }

  .timeline-item {
    flex: 0 0 auto;
    gap: 12px;
    min-height: 0;
    padding-bottom: 24px;
  }

  .timeline-item:last-child {
    padding-bottom: 0;
  }

  .timeline-item.reverse {
    padding-bottom: 28px;
  }

  .timeline-item.reverse .timeline-line {
    height: calc(100% - 6px);
  }

  .timeline-content {
    flex: 1 1 auto;
    max-width: none;
  }

}

@media (max-width: 640px) {
  .summary-cards,
  .participants {
    grid-template-columns: 1fr;
  }

  .participants {
    grid-template-areas:
      "publisher"
      "publisher-proof"
      "acceptor"
      "acceptor-proof";
  }

  .status-banner {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }
}
</style>
