<template>
  <div class="profile-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>个人中心</span>
      </template>
    </el-page-header>

    <!-- 用户头部卡片 -->
    <div class="profile-header">
      <div class="header-left">
        <el-avatar :size="72" :src="userStore.avatarUrl" icon="UserFilled" class="profile-avatar" />
        <div class="header-info">
          <div class="header-name">{{ userInfo?.nickname || userInfo?.username }}</div>
          <div class="header-meta">
            <span class="meta-id">学号 {{ userInfo?.studentIdMasked }}</span>
            <el-tag :type="authTagType" size="small">{{ authStatusLabel }}</el-tag>
            <el-tag v-if="userInfo?.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <div class="point-panel">
          <span class="point-label">积分余额</span>
          <span class="point-value">{{ pointBalance }}</span>
        </div>
        <el-button type="primary" plain @click="toggleEdit">编辑资料</el-button>
      </div>
    </div>

    <!-- 查看模式 -->
    <template v-if="!isEditing">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header><span>基本信息</span></template>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userInfo?.username }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userInfo?.nickname || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">学院</span>
            <span class="info-value">{{ userInfo?.college || '未设置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">联系方式</span>
            <span class="info-value">{{ userInfo?.contact || '未设置' }}</span>
          </div>
        </div>
      </el-card>

      <!-- 快捷入口 -->
      <div class="quick-cards">
        <div class="quick-card" @click="$router.push('/verification')">
          <span class="quick-icon">🪪</span>
          <span class="quick-label">实名认证</span>
        </div>
        <div class="quick-card" @click="$router.push('/transactions')">
          <span class="quick-icon">💰</span>
          <span class="quick-label">积分流水</span>
        </div>
      </div>
    </template>

    <!-- 编辑模式 -->
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile } from '@/api/user'
import { getPointBalance } from '@/api/points'

const userStore = useUserStore()
const isEditing = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const avatarUploadRef = ref(null)
const avatarFile = ref(null)
const avatarFileList = ref([])
const pointBalance = ref(0)

const userInfo = computed(() => userStore.userInfo)

const authStatusMap = {
  UNVERIFIED: '未认证',
  REVIEWING: '审核中',
  APPROVED: '已认证',
  REJECTED: '认证失败'
}

const authStatusLabel = computed(() => authStatusMap[userInfo.value?.authStatus] || '未知')

const authTagType = computed(() => {
  const map = {
    UNVERIFIED: 'info',
    REVIEWING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[userInfo.value?.authStatus] || 'info'
})

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
})
</script>

<style scoped>
.profile-view {
  max-width: 700px;
  margin: 0 auto;
  padding-bottom: 24px;
}

.profile-header {
  margin-top: 16px;
  padding: 24px;
  border-radius: 12px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar :deep(.el-icon) {
  font-size: 34px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.point-panel {
  min-width: 118px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
}

.point-label {
  font-size: 12px;
  opacity: 0.86;
}

.point-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.header-name {
  font-size: 22px;
  font-weight: 600;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-id {
  font-size: 13px;
  opacity: 0.85;
}

.profile-header :deep(.el-button) {
  border-color: rgba(255, 255, 255, 0.6);
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
}

.profile-header :deep(.el-button:hover) {
  background: rgba(255, 255, 255, 0.25);
  border-color: #fff;
  color: #fff;
}

.info-card {
  margin-top: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #909399;
}

.info-value {
  font-size: 15px;
  color: #303133;
}

.quick-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.quick-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.quick-icon {
  font-size: 28px;
}

.quick-label {
  font-size: 14px;
  color: #303133;
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
</style>
