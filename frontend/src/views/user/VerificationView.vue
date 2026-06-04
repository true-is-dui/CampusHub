<template>
  <div class="verification-view">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>实名认证</span>
      </template>
    </el-page-header>

    <el-card class="verification-card" shadow="never">
      <!-- Current Status -->
      <div class="status-section">
        <h3>认证状态</h3>
        <el-tag :type="statusTagType" size="large">{{ statusLabel }}</el-tag>
      </div>

      <!-- UNVERIFIED: show form -->
      <template v-if="authStatus === 'UNVERIFIED'">
        <el-divider />
        <h3>提交认证信息</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          style="max-width: 500px"
        >
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="30" />
          </el-form-item>
          <el-form-item label="学号" prop="studentId">
            <el-input v-model="form.studentId" placeholder="请输入学号" maxlength="30" />
          </el-form-item>
          <el-form-item label="认证图片" prop="verificationImage">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              list-type="picture-card"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">请上传学生证或校园卡照片</div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              提交认证
            </el-button>
          </el-form-item>
        </el-form>
      </template>

      <!-- REVIEWING: show message -->
      <template v-if="authStatus === 'REVIEWING'">
        <el-result icon="info" title="审核中" sub-title="您的认证信息正在审核中，请耐心等待" />
      </template>

      <!-- APPROVED: show message -->
      <template v-if="authStatus === 'APPROVED'">
        <el-result icon="success" title="已认证" sub-title="您已完成实名认证" />
      </template>

      <!-- REJECTED: show reason and re-submit form -->
      <template v-if="authStatus === 'REJECTED'">
        <el-alert
          :title="'认证未通过：' + (rejectReason || '未说明原因')"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />
        <el-divider />
        <h3>重新提交认证</h3>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          style="max-width: 500px"
        >
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="30" />
          </el-form-item>
          <el-form-item label="学号" prop="studentId">
            <el-input v-model="form.studentId" placeholder="请输入学号" maxlength="30" />
          </el-form-item>
          <el-form-item label="认证图片" prop="verificationImage">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              list-type="picture-card"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">请上传学生证或校园卡照片</div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              重新提交
            </el-button>
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
import { submitVerification } from '@/api/user'

const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const imageFile = ref(null)

const authStatus = computed(() => userStore.userInfo?.authStatus || 'UNVERIFIED')
const rejectReason = computed(() => userStore.userInfo?.rejectReason || '')

const statusMap = {
  UNVERIFIED: '未认证',
  REVIEWING: '审核中',
  APPROVED: '已认证',
  REJECTED: '认证失败'
}

const statusLabel = computed(() => statusMap[authStatus.value] || '未知')

const statusTagType = computed(() => {
  const map = {
    UNVERIFIED: 'info',
    REVIEWING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[authStatus.value] || 'info'
})

const form = reactive({
  realName: '',
  studentId: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  verificationImage: [
    {
      validator: (rule, value, callback) => {
        if (!imageFile.value) {
          callback(new Error('请上传认证图片'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

function onFileChange(file) {
  imageFile.value = file.raw
}

function onFileRemove() {
  imageFile.value = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const fd = new FormData()
  fd.append('realName', form.realName)
  fd.append('studentId', form.studentId)
  fd.append('verificationImage', imageFile.value)

  submitting.value = true
  try {
    await submitVerification(fd)
    await userStore.fetchUserInfo()
    ElMessage.success('认证提交成功，请等待审核')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (userStore.userInfo) {
    form.realName = userStore.userInfo.realName || ''
    form.studentId = userStore.userInfo.studentId || ''
  }
})
</script>

<style scoped>
.verification-view {
  max-width: 700px;
  margin: 0 auto;
}

.verification-card {
  margin-top: 20px;
}

.status-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-section h3 {
  margin: 0;
  font-size: 16px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
