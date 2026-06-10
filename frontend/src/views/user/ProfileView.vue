<template>
  <div class="profile-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>个人中心</span>
      </template>
    </el-page-header>

    <template v-if="profilePerspective === 'visitor'">
      <UserPublicProfilePanel
          v-if="userInfo?.userId"
          :user-id="userInfo.userId"
          :auth-status="userInfo.authStatus"
      />
    </template>

    <template v-else>
      <template v-if="!isEditing">
        <div class="profile-dashboard-grid">
          <el-card class="profile-section-card identity-card" shadow="never">
            <div class="identity-shell">
              <div class="identity-content">
                <el-avatar :size="104" :src="userStore.avatarUrl" icon="UserFilled" class="profile-avatar" />
                <div class="identity-text">
                  <div class="identity-name">{{ userInfo?.nickname || '未设置' }}</div>
                  <div class="identity-username">
                    <span>用户名</span>
                    <strong>{{ userInfo?.username }}</strong>
                  </div>
                  <el-tag v-if="userInfo?.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
                </div>
              </div>
              <div class="identity-tip">{{ identityGreeting }}</div>
            </div>
          </el-card>

          <el-card class="profile-section-card points-card" shadow="never" @click="$router.push('/points')">
            <div class="points-shell">
              <div class="points-content">
                <span class="points-icon" aria-hidden="true">
                  <el-icon><Money /></el-icon>
                </span>
                <div class="points-copy">
                  <span class="points-title">积分余额</span>
                  <div class="point-value">{{ pointBalance }}</div>
                </div>
              </div>
              <div class="points-tip">{{ pointsTip }}</div>
            </div>
          </el-card>

          <el-card class="profile-section-card public-info-card" shadow="never">
            <button class="public-edit-badge" type="button" aria-label="编辑公开资料" @click="toggleEdit">
              <el-icon><EditPen /></el-icon>
              <span>编辑</span>
            </button>
            <div class="public-info-shell">
              <div class="public-info-content">
                <span class="public-info-icon" aria-hidden="true">
                  <el-icon><Postcard /></el-icon>
                </span>
                <div class="public-info-copy">
                  <div class="public-info-list">
                    <div class="info-item public-info-row">
                      <span class="info-label">昵称</span>
                      <span class="info-value">{{ userInfo?.nickname || '未设置' }}</span>
                    </div>
                    <div class="info-item public-info-row">
                      <span class="info-label">学院</span>
                      <span class="info-value">{{ userInfo?.college || '未设置' }}</span>
                    </div>
                    <div class="info-item public-info-row">
                      <span class="info-label">联系方式</span>
                      <span class="info-value">{{ userInfo?.contact || '未设置' }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="public-tip">这些信息会在访客视角和公开主页中展示。</div>
            </div>
          </el-card>

          <el-card class="profile-section-card auth-card" shadow="never">
            <button
                class="auth-verify-badge"
                type="button"
                :disabled="!canStartVerification"
                aria-label="去认证"
                @click="$router.push('/verification')"
            >
              <el-icon><Stamp /></el-icon>
              <span>去认证</span>
            </button>
            <div class="auth-info-shell">
              <div class="auth-info-content">
                <span class="auth-info-icon" aria-hidden="true">
                  <el-icon><UserFilled /></el-icon>
                </span>
                <div class="auth-info-copy">
                  <div class="auth-info-list">
                    <div class="info-item auth-info-row">
                      <span class="info-label">状态</span>
                      <span :class="['auth-status-pill', authStatusClass]">{{ authStatusLabel }}</span>
                    </div>
                    <div class="info-item auth-info-row">
                      <span class="info-label">姓名</span>
                      <span class="info-value">{{ authRealNameDisplay }}</span>
                    </div>
                    <div class="info-item auth-info-row">
                      <span class="info-label">学号</span>
                      <span class="info-value">{{ authStudentIdDisplay }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="auth-tip">认证通过才可参与互助，这些信息不会对外展示。</div>
            </div>
          </el-card>

          <div class="overview-item overview-item-publisher" @click="$router.push('/my-evaluations')">
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

          <div class="overview-item overview-item-acceptor" @click="$router.push('/my-evaluations')">
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
      </template>

      <template v-else>
        <el-card class="info-card" shadow="never">
          <template #header><span>编辑资料</span></template>
          <el-form
              ref="editFormRef"
              :model="editForm"
              :rules="editRules"
              label-width="80px"
              style="max-width: 500px"
          >
            <el-form-item label="头像">
              <el-upload
                  ref="avatarUploadRef"
                  :auto-upload="false"
                  :limit="1"
                  accept="image/jpeg,image/png"
                  list-type="picture-card"
                  :on-change="onAvatarChange"
                  :on-remove="onAvatarRemove"
                  :file-list="avatarFileList"
              >
                <el-icon v-if="avatarFileList.length === 0"><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">JPG/PNG，不超过5MB，最多上传一张</div>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称（1-20字符）" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="学院" prop="college">
              <el-input v-model="editForm.college" placeholder="请输入学院（最多50字符）" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="联系方式" prop="contact">
              <el-input v-model="editForm.contact" placeholder="请输入联系方式（最多100字符）" maxlength="100" show-word-limit />
            </el-form-item>
          </el-form>
        </el-card>
        <div class="edit-actions">
          <el-button size="large" @click="isEditing = false">取消</el-button>
          <el-button type="primary" size="large" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getUserProfile, updateProfile } from '@/api/user'
import { getPointBalance } from '@/api/points'
import UserPublicProfilePanel from './UserPublicProfilePanel.vue'

const route = useRoute()
const userStore = useUserStore()
const isEditing = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const avatarUploadRef = ref(null)
const avatarFile = ref(null)
const avatarFileList = ref([])
const pointBalance = ref(0)
const ratingSummary = ref(null)

const userInfo = computed(() => userStore.userInfo)
const profilePerspective = computed(() => route.query.perspective === 'visitor' ? 'visitor' : 'self')
const emptyRoleSummary = {
  totalCount: 0,
  positiveRate: null
}
const publisherSummary = computed(() => ratingSummary.value?.publisherRoleSummary || emptyRoleSummary)
const acceptorSummary = computed(() => ratingSummary.value?.acceptorRoleSummary || emptyRoleSummary)
const pointsTips = [
  '通过实名认证可获得100积分。',
  '每日签到可获得5积分。',
  '完成接单可获取积分。',
  '给帮助你的同学赏点积分吧。'
]
const identityGreetingTemplates = [
  (name) => `${name}，今天也欢迎回到 CampusHub。`,
  (name) => `${name}，愿你的校园互助一路顺手。`,
  (name) => `${name}，看看今天有什么可以帮上忙的。`,
  (name) => `${name}，你的个人资料已准备就绪。`
]
const pointsTipIndex = ref(Math.floor(Math.random() * pointsTips.length))
const identityGreetingIndex = ref(Math.floor(Math.random() * identityGreetingTemplates.length))
const pointsTip = computed(() => pointsTips[pointsTipIndex.value])
const identityGreeting = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || '同学'
  return identityGreetingTemplates[identityGreetingIndex.value](name)
})

watch(profilePerspective, (value) => {
  if (value === 'visitor') {
    isEditing.value = false
  }
})

const authStatusMap = {
  UNVERIFIED: '未认证',
  REVIEWING: '审核中',
  APPROVED: '已认证',
  REJECTED: '认证失败'
}

const authStatusLabel = computed(() => authStatusMap[userInfo.value?.authStatus] || '未知')
const canStartVerification = computed(() => userInfo.value?.authStatus === 'UNVERIFIED')
const isVerificationReviewing = computed(() => userInfo.value?.authStatus === 'REVIEWING')
const authRealNameDisplay = computed(() => isVerificationReviewing.value ? '**' : (userInfo.value?.realName || '未认证'))
const authStudentIdDisplay = computed(() => isVerificationReviewing.value ? '********' : (userInfo.value?.studentIdMasked || '未认证'))

const authStatusClass = computed(() => {
  const map = {
    UNVERIFIED: 'auth-status-pill--unverified',
    REVIEWING: 'auth-status-pill--reviewing',
    APPROVED: 'auth-status-pill--approved',
    REJECTED: 'auth-status-pill--unverified'
  }
  return map[userInfo.value?.authStatus] || 'auth-status-pill--unverified'
})

function formatRateNumber(rate) {
  if (rate == null) return '暂无'
  return (rate * 100).toFixed(1)
}

const editForm = reactive({
  nickname: '',
  college: '',
  contact: ''
})

// 保存原始值（可能为 null），用于对比是否修改
const original = reactive({
  nickname: '',
  college: null,
  contact: null
})

const editRules = {
  nickname: [
    { min: 1, max: 20, message: '昵称长度为 1-20 字符', trigger: 'blur' }
  ],
  college: [
    { max: 50, message: '学院名称不能超过 50 字符', trigger: 'blur' }
  ],
  contact: [
    { max: 100, message: '联系方式不能超过 100 字符', trigger: 'blur' }
  ]
}

function toggleEdit() {
  if (isEditing.value) {
    isEditing.value = false
    return
  }
  const cur = userInfo.value || {}
  editForm.nickname = cur.nickname || ''
  editForm.college = cur.college || ''
  editForm.contact = cur.contact || ''
  // 保存原始值（保留 null）
  original.nickname = editForm.nickname
  original.college = cur.college  // 可能为 null
  original.contact = cur.contact  // 可能为 null
  avatarFile.value = null
  avatarFileList.value = []
  isEditing.value = true
}

function onAvatarChange(file) {
  if (file.raw && file.raw.size > 5 * 1024 * 1024) {
    ElMessage.error('头像图片不能超过5MB')
    const index = avatarFileList.value.findIndex(f => f.uid === file.uid)
    if (index !== -1) {
      avatarFileList.value.splice(index, 1)
    }
    return
  }
  avatarFile.value = file.raw
}

function onAvatarRemove() {
  avatarFile.value = null
}

function isValueChanged(field) {
  const current = editForm[field]
  const prev = original[field]
  const normalize = (v) => (v === null || v === '' ? '' : v)
  return normalize(current) !== normalize(prev)
}

async function loadPointStatus() {
  try {
    const balanceRes = await getPointBalance()
    pointBalance.value = balanceRes?.pointBalance ?? 0
  } catch {
    // error handled by interceptor
  }
}

async function loadRatingSummary() {
  const userId = userStore.userInfo?.userId
  if (!userId) return
  try {
    const res = await getUserProfile(userId, true)
    ratingSummary.value = res?.ratingSummary || null
  } catch {
    // error handled by interceptor
  }
}

async function handleSave() {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  // 额外校验：昵称部分更新时不能清空（API 昵称 minLength=1）
  if (isValueChanged('nickname') && !editForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }

  const fd = new FormData()
  let hasChange = false

  // 昵称修改检查
  if (isValueChanged('nickname')) {
    fd.append('nickname', editForm.nickname)
    hasChange = true
  }

  // 学院修改检查
  if (isValueChanged('college')) {
    fd.append('college', editForm.college || '')
    hasChange = true
  }

  // 联系方式修改检查
  if (isValueChanged('contact')) {
    fd.append('contact', editForm.contact || '')
    hasChange = true
  }

  // 头像
  if (avatarFile.value) {
    fd.append('avatar', avatarFile.value)
    hasChange = true
  }

  if (!hasChange) {
    ElMessage.warning('没有修改任何资料')
    return
  }

  saving.value = true
  try {
    await updateProfile(fd)
    await userStore.fetchUserInfo()

    // 如果上传了新头像，立即更新全局头像（从本地文件生成新 URL）
    if (avatarFile.value) {
      const newUrl = URL.createObjectURL(avatarFile.value)
      userStore.updateAvatarUrl(newUrl)
    } else {
      // 没有修改头像，仍从后端同步最新头像（例如后端可能有默认头像或其他场景）
      await userStore.loadAvatar()
    }

    ElMessage.success('保存成功')
    isEditing.value = false
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  // 如果 Store 中还没有头像，则加载
  if (!userStore.avatarUrl && userStore.userInfo?.userId) {
    await userStore.loadAvatar()
  }
  await loadPointStatus()
  await loadRatingSummary()
})
</script>

<style scoped>
.profile-view {
  width: min(calc(100% - 32px), 1120px);
  margin-left: max(16px, calc((100% - 1120px) / 2 - 16px));
  margin-right: auto;
  padding-bottom: 24px;
}

.profile-dashboard-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1.08fr) minmax(270px, 0.95fr);
  grid-template-areas:
    "identity public publisher"
    "points auth acceptor";
  gap: 18px;
  margin-top: 24px;
  align-items: stretch;
}

.profile-visitor-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-areas:
    "identity identity"
    "publisher acceptor";
}

.visitor-identity-card {
  min-height: 232px;
}

.visitor-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.visitor-name-row .auth-status-pill {
  margin-left: 0;
}

.visitor-public-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
}

.visitor-public-row .info-value {
  min-width: 0;
  flex: 1;
  color: #2b3a4a;
}

.profile-section-card {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  min-height: 232px;
}

.profile-section-card :deep(.el-card__body) {
  flex: 1;
  box-sizing: border-box;
  padding: 24px;
}

.profile-section-card :deep(.el-card__header) {
  padding: 16px 24px;
  font-size: 18px;
  font-weight: 700;
}

.identity-card {
  grid-area: identity;
  border-color: #d8ebff;
  background: linear-gradient(135deg, #eef7ff 0%, #f8fbff 100%);
}

.identity-card :deep(.el-card__body),
.points-card :deep(.el-card__body) {
  padding: 22px 24px;
}

.identity-shell,
.points-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
}

.identity-content {
  flex: 1;
  min-height: 0;
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

.identity-label,
.section-label,
.info-label,
.point-caption {
  color: #909399;
  font-size: 15px;
  font-weight: 700;
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

.identity-username {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5d7f9f;
  font-size: 15px;
  line-height: 22px;
  font-weight: 600;
}

.identity-username strong {
  color: #2b3a4a;
  font-size: 17px;
  font-weight: 800;
}

.identity-tip {
  color: #6d8dad;
  font-size: 13px;
  line-height: 18px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  color: #303133;
  font-size: 18px;
  font-weight: 800;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
}

.info-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.span-all {
  grid-column: 1 / -1;
}

.info-value {
  color: #303133;
  font-size: 18px;
  line-height: 24px;
  font-weight: 700;
  word-break: break-word;
}

.public-tip {
  color: #6f8f80;
  font-size: 13px;
  line-height: 18px;
}

.points-card {
  grid-area: points;
  border-color: #f4dfad;
  background: linear-gradient(135deg, #fff8e6 0%, #fffdf5 100%);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.points-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(190, 140, 32, 0.08);
}

.points-content {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
  text-align: left;
}

.points-icon {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #c78912;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10px 22px rgba(190, 140, 32, 0.14);
}

.points-icon .el-icon {
  font-size: 56px;
}

.points-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.points-title {
  color: #9a6a12;
  font-size: 20px;
  line-height: 26px;
  font-weight: 700;
}

.point-value {
  color: #bd8214;
  font-size: 46px;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.points-tip {
  color: #9a6a12;
  font-size: 13px;
  line-height: 18px;
}

.public-info-card {
  grid-area: public;
  border-color: #cdebd8;
  background: linear-gradient(135deg, #effaf2 0%, #f8fdf9 100%);
}

.public-info-card :deep(.el-card__body) {
  position: relative;
  padding: 22px 24px;
}

.public-info-card--visitor .public-info-shell {
  justify-content: center;
}

.public-edit-badge {
  position: absolute;
  top: 22px;
  right: 24px;
  z-index: 2;
  min-width: 68px;
  height: 30px;
  padding: 0 11px;
  border: 1px solid rgba(47, 157, 99, 0.18);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #2f8d5c;
  background: rgba(255, 255, 255, 0.64);
  box-sizing: border-box;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  box-shadow: none;
  backdrop-filter: blur(6px);
  transition: transform 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.public-edit-badge:hover {
  transform: translateY(-1px);
  border-color: rgba(47, 157, 99, 0.28);
  background: rgba(232, 249, 239, 0.9);
  box-shadow: 0 3px 8px rgba(47, 157, 99, 0.08);
}

.public-edit-badge .el-icon {
  font-size: 15px;
}

.public-info-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
}

.public-info-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
  padding-top: 57px;
}

.public-info-icon {
  width: 108px;
  height: 108px;
  border-radius: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #2f9d63;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 10px 22px rgba(47, 157, 99, 0.13);
}

.public-info-icon .el-icon {
  font-size: 64px;
}

.public-info-copy {
  min-width: 0;
  flex: 1;
}

.public-info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.public-info-row {
  flex-direction: row;
  align-items: baseline;
  gap: 12px;
}

.public-info-row .info-label {
  width: 64px;
  flex: 0 0 auto;
  color: #5f9072;
}

.public-info-row .info-value {
  min-width: 0;
  flex: 1;
  color: #244536;
}

.auth-card {
  grid-area: auth;
  border-color: #e3d8ff;
  background: linear-gradient(135deg, #f5f0ff 0%, #fbf9ff 100%);
}

.auth-card :deep(.el-card__body) {
  position: relative;
  padding: 22px 24px;
}

.auth-verify-badge {
  position: absolute;
  top: 22px;
  right: 24px;
  z-index: 2;
  min-width: 82px;
  height: 30px;
  padding: 0 11px;
  border: 1px solid rgba(130, 96, 214, 0.18);
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: #7352c8;
  background: rgba(255, 255, 255, 0.64);
  box-sizing: border-box;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  box-shadow: none;
  backdrop-filter: blur(6px);
  transition: border-color 0.18s ease, background-color 0.18s ease, color 0.18s ease;
}

.auth-verify-badge:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(130, 96, 214, 0.3);
  background: rgba(242, 236, 255, 0.92);
  box-shadow: 0 3px 8px rgba(130, 96, 214, 0.08);
}

.auth-verify-badge:disabled {
  color: #a9a3b8;
  border-color: rgba(169, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.42);
  cursor: not-allowed;
}

.auth-verify-badge .el-icon {
  font-size: 15px;
}

.auth-info-shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
}

.auth-info-content {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
  padding-top: 57px;
}

.auth-info-icon {
  width: 108px;
  height: 108px;
  border-radius: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: #8260d6;
  background: rgba(255, 255, 255, 0.78);
}

.auth-info-icon .el-icon {
  font-size: 64px;
}

.auth-info-copy {
  min-width: 0;
  flex: 1;
}

.auth-info-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-info-row {
  flex-direction: row;
  align-items: center;
  gap: 12px;
  min-height: 28px;
}

.auth-info-row .info-label {
  width: 72px;
  flex: 0 0 auto;
  color: #7b6f99;
}

.auth-info-row .info-value {
  min-width: 0;
  flex: 1;
  color: #403456;
}

.auth-status-pill {
  margin-left: -7px;
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

.auth-tip {
  color: #85789e;
  font-size: 13px;
  line-height: 18px;
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
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.overview-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 40px rgba(37, 48, 78, 0.18);
}

.profile-visitor-grid .overview-item {
  cursor: default;
}

.profile-visitor-grid .overview-item:hover {
  transform: none;
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

.info-card {
  margin-top: 24px;
  border-radius: 8px;
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

.info-card :deep(.el-card__header) {
  padding: 18px 28px;
  font-size: 18px;
  font-weight: 700;
}

.info-card :deep(.el-card__body) {
  padding: 28px;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
}

@media (max-width: 1080px) {
  .profile-view {
    width: calc(100% - 32px);
    margin-left: 16px;
  }

  .profile-dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-template-areas:
      "identity public"
      "points auth"
      "publisher acceptor";
  }

  .profile-visitor-grid {
    grid-template-areas:
      "identity identity"
      "publisher acceptor";
  }

  .info-grid {
    gap: 24px 48px;
  }
}

@media (max-width: 640px) {
  .profile-dashboard-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "identity"
      "points"
      "public"
      "auth"
      "publisher"
      "acceptor";
  }

  .profile-visitor-grid {
    grid-template-areas:
      "identity"
      "publisher"
      "acceptor";
  }

  .visitor-name-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .eval-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .identity-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .identity-name {
    font-size: 26px;
    line-height: 32px;
  }

  .info-grid,
  .public-info-grid {
    grid-template-columns: 1fr;
  }

  .span-all {
    grid-column: auto;
  }
}
</style>
