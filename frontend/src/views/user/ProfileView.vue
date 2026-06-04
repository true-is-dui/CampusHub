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
          label-width="80px"
          style="max-width: 500px"
        >
          <el-form-item label="头像">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              list-type="picture-card"
              :on-change="onAvatarChange"
              :on-remove="onAvatarRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="editForm.nickname" placeholder="请输入昵称" maxlength="30" />
          </el-form-item>
          <el-form-item label="学院" prop="college">
            <el-input v-model="editForm.college" placeholder="请输入学院" maxlength="50" />
          </el-form-item>
          <el-form-item label="联系方式" prop="contact">
            <el-input v-model="editForm.contact" placeholder="请输入联系方式" maxlength="100" />
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
const avatarFile = ref(null)

const userInfo = computed(() => userStore.userInfo)

const avatarSrc = computed(() => {
  if (userInfo.value?.id) {
    return getUserAvatar(userInfo.value.id)
  }
  return ''
})

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

function toggleEdit() {
  if (isEditing.value) {
    isEditing.value = false
    return
  }
  editForm.nickname = userInfo.value?.nickname || ''
  editForm.college = userInfo.value?.college || ''
  editForm.contact = userInfo.value?.contact || ''
  avatarFile.value = null
  isEditing.value = true
}

function onAvatarChange(file) {
  avatarFile.value = file.raw
}

function onAvatarRemove() {
  avatarFile.value = null
}

async function handleSave() {
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('nickname', editForm.nickname || '')
    fd.append('college', editForm.college || '')
    fd.append('contact', editForm.contact || '')
    if (avatarFile.value) {
      fd.append('avatar', avatarFile.value)
    }
    await updateProfile(fd)
    await userStore.fetchUserInfo()
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
</style>
