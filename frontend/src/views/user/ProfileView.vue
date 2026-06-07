<template>
  <div class="profile-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>个人中心</span>
      </template>
    </el-page-header>

    <el-card class="profile-card" shadow="never">
      <template #header>
        <div class="card-header-row">
          <span>个人信息</span>
          <el-button text type="primary" @click="toggleEdit">
            {{ isEditing ? '取消' : '编辑' }}
          </el-button>
        </div>
      </template>

      <!-- View Mode -->
      <template v-if="!isEditing">
        <div class="profile-display">
          <div class="avatar-section">
            <el-avatar :size="80" :src="avatarSrc" icon="UserFilled" />
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户名">{{ userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ userInfo?.nickname || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="学院">{{ userInfo?.college || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ userInfo?.contact || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="认证状态">
              <el-tag :type="authTagType">{{ authStatusLabel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag v-if="userInfo?.role === 'ADMIN'" type="danger">管理员</el-tag>
              <el-tag v-else>普通用户</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="quick-links">
            <el-button type="primary" text @click="$router.push('/verification')">
              实名认证
            </el-button>
            <el-button type="primary" text @click="$router.push('/transactions')">
              交易记录
            </el-button>
          </div>
        </div>
      </template>

      <!-- Edit Mode -->
      <template v-else>
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
              <!-- 没有文件时显示加号，有文件后自动隐藏 -->
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
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            <el-button @click="isEditing = false">取消</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile, getUserAvatar } from '@/api/user'

const userStore = useUserStore()
const isEditing = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const avatarUploadRef = ref(null)
const avatarFile = ref(null)
const avatarFileList = ref([])

const avatarSrc = ref('')

const userInfo = computed(() => userStore.userInfo)

async function loadAvatar() {
  if (!userInfo.value?.userId) return
  try {
    const blob = await getUserAvatar(userInfo.value.userId)
    if (avatarSrc.value) {
      URL.revokeObjectURL(avatarSrc.value)
    }
    avatarSrc.value = URL.createObjectURL(blob)
  } catch {
    avatarSrc.value = ''
  }
}

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
    await loadAvatar()
    ElMessage.success('保存成功')
    isEditing.value = false
  } catch {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!userInfo.value) {
    await userStore.fetchUserInfo()
  }
  await loadAvatar()
})
</script>

<style scoped>
.profile-view {
  max-width: 700px;
  margin: 0 auto;
}

.profile-card {
  margin-top: 20px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.quick-links {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>