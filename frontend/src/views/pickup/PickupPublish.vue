<template>
  <div class="pickup-publish">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>发布代取</span>
      </template>
    </el-page-header>

    <el-card class="publish-card" shadow="never">
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          label-position="right"
      >
        <el-form-item label="校区" prop="campus">
          <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
            <el-option label="鼓楼校区" value="GULOU" />
            <el-option label="仙林校区" value="XIANLIN" />
            <el-option label="苏州校区" value="SUZHOU" />
            <el-option label="浦口校区" value="PUKOU" />
          </el-select>
        </el-form-item>

        <el-form-item label="取件地点" prop="pickupLocation">
          <el-input v-model="form.pickupLocation" placeholder="请输入取件地点" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="送达地点" prop="deliveryLocation">
          <el-input v-model="form.deliveryLocation" placeholder="请输入送达地点" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="物品描述" prop="itemDescription">
          <el-input
              v-model="form.itemDescription"
              type="textarea"
              :rows="3"
              placeholder="请描述需要代取的物品"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="报酬类型" prop="rewardType">
          <el-radio-group v-model="form.rewardType">
            <el-radio value="PAID">有报酬</el-radio>
            <el-radio value="UNPAID">无报酬</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
            v-if="form.rewardType === 'PAID'"
            label="报酬金额"
            prop="rewardAmount"
        >
          <el-input-number
              v-model="form.rewardAmount"
              :min="1"
              :max="200"
              :precision="0"
              :step="1"
          />
          <span class="unit-text">元</span>
        </el-form-item>

        <el-form-item label="接单截止" prop="acceptDeadline">
          <el-date-picker
              v-model="form.acceptDeadline"
              type="datetime"
              placeholder="请选择接单截止时间（必须晚于当前时间）"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :disabled-date="disabledDate"
              :disabled-hours="disabledHours"
              :disabled-minutes="disabledMinutes"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="取件凭证" prop="pickupCredential">
          <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              accept="image/jpeg,image/png"
              list-type="picture-card"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :before-upload="beforeUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">请上传取件凭证图片（如快递截图），JPG/PNG，不超过5MB</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            发布
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Pay Dialog -->
    <el-dialog v-model="showPayDialog" title="支付提示" width="400px" :close-on-click-modal="false" @close="clearCountdown">
      <p>代取请求已发布，请完成支付。</p>
      <div v-if="countdownText && !isPayExpired" class="pay-countdown">
        <el-icon><Clock /></el-icon>
        <span>剩余支付时间：<strong>{{ countdownText }}</strong></span>
      </div>
      <div v-if="isPayExpired" class="pay-expired-tip">
        <el-alert type="warning" :closable="false" show-icon>
          支付已超时，该代取请求已自动取消
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="goDetail">稍后支付</el-button>
        <el-button
            type="primary"
            :disabled="isPayExpired"
            @click="goPay"
        >
          去支付
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPickup } from '@/api/pickup'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const showPayDialog = ref(false)
const createdId = ref(null)
const payUrl = ref('')
const credentialFile = ref(null)

// 支付倒计时相关
const expireAt = ref(null)
const countdownText = ref('')
const isPayExpired = ref(false)
let countdownTimer = null

const form = reactive({
  campus: '',
  pickupLocation: '',
  deliveryLocation: '',
  itemDescription: '',
  rewardType: 'UNPAID',
  rewardAmount: 5,
  acceptDeadline: ''
})

const validateRewardAmount = (rule, value, callback) => {
  if (form.rewardType === 'PAID' && (!value || value < 1 || value > 200)) {
    callback(new Error('报酬金额为1-200元'))
  } else {
    callback()
  }
}

const validateCredential = (rule, value, callback) => {
  if (!credentialFile.value) {
    callback(new Error('请上传取件凭证图片'))
  } else {
    callback()
  }
}

const validateDeadline = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择接单截止时间'))
  } else if (new Date(value) <= new Date()) {
    callback(new Error('接单截止时间必须晚于当前时间'))
  } else {
    callback()
  }
}

const rules = {
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }],
  pickupLocation: [
    { required: true, message: '请输入取件地点', trigger: 'blur' },
    { min: 1, max: 100, message: '取件地点长度为1-100个字符', trigger: 'blur' }
  ],
  deliveryLocation: [
    { required: true, message: '请输入送达地点', trigger: 'blur' },
    { min: 1, max: 100, message: '送达地点长度为1-100个字符', trigger: 'blur' }
  ],
  itemDescription: [
    { required: true, message: '请输入物品描述', trigger: 'blur' },
    { min: 1, max: 500, message: '物品描述长度为1-500个字符', trigger: 'blur' }
  ],
  rewardType: [{ required: true, message: '请选择报酬类型', trigger: 'change' }],
  rewardAmount: [{ validator: validateRewardAmount, trigger: 'change' }],
  acceptDeadline: [{ validator: validateDeadline, trigger: 'change' }],
  pickupCredential: [{ validator: validateCredential, trigger: 'change' }]
}

function disabledDate(date) {
  return date < new Date(Date.now() - 86400000)
}

function disabledHours() {
  const selectedDate = form.acceptDeadline ? new Date(form.acceptDeadline) : new Date()
  const now = new Date()
  if (selectedDate.toDateString() === now.toDateString()) {
    const currentHour = now.getHours()
    return Array.from({ length: currentHour }, (_, i) => i)
  }
  return []
}

function disabledMinutes(hour) {
  const selectedDate = form.acceptDeadline ? new Date(form.acceptDeadline) : new Date()
  const now = new Date()
  if (selectedDate.toDateString() === now.toDateString() && hour === now.getHours()) {
    const currentMinute = now.getMinutes()
    return Array.from({ length: currentMinute }, (_, i) => i)
  }
  return []
}

function beforeUpload(file) {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('取件凭证图片不能超过5MB')
    return false
  }
  return true
}

function onFileChange(file) {
  // 二次校验文件大小（处理某些绕过 beforeUpload 的场景）
  if (file.raw && file.raw.size > 5 * 1024 * 1024) {
    ElMessage.error('取件凭证图片不能超过5MB')
    // 使用公开方法清除文件列表，同时清空文件引用
    uploadRef.value?.clearFiles()
    credentialFile.value = null
    formRef.value?.validateField('pickupCredential')
    return
  }
  credentialFile.value = file.raw
  formRef.value?.validateField('pickupCredential')
}

function onFileRemove() {
  credentialFile.value = null
  formRef.value?.validateField('pickupCredential')
}

function startCountdown() {
  clearCountdown()
  isPayExpired.value = false
  if (!expireAt.value) return
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}

function updateCountdown() {
  const diff = new Date(expireAt.value) - Date.now()
  if (diff <= 0) {
    countdownText.value = '已超时'
    isPayExpired.value = true
    clearCountdown()
    return
  }
  const min = Math.floor(diff / 60000)
  const sec = Math.floor((diff % 60000) / 1000)
  countdownText.value = `${min}分${sec}秒`
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const fd = new FormData()
  fd.append('campus', form.campus)
  fd.append('pickupLocation', form.pickupLocation)
  fd.append('deliveryLocation', form.deliveryLocation)
  fd.append('itemDescription', form.itemDescription)
  fd.append('rewardType', form.rewardType)
  fd.append('acceptDeadline', form.acceptDeadline)
  if (form.rewardType === 'PAID') {
    fd.append('rewardAmount', String(form.rewardAmount))
  }
  fd.append('pickupCredential', credentialFile.value)

  loading.value = true
  try {
    const res = await createPickup(fd)
    createdId.value = res?.pickupId
    ElMessage.success('发布成功')

    if (form.rewardType === 'PAID' && res?.payEntry) {
      payUrl.value = res.payEntry
      expireAt.value = res?.expireAt || null
      startCountdown()
      showPayDialog.value = true
      return
    }
    goDetail()
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function goPay() {
  if (!isPayExpired.value && payUrl.value) {
    window.location.href = payUrl.value
  } else {
    goDetail()
  }
}

function goDetail() {
  if (createdId.value) {
    router.push(`/pickup/${createdId.value}`)
  } else {
    router.push('/hall')
  }
}

onUnmounted(() => {
  clearCountdown()
})
</script>

<style scoped>
.pickup-publish {
  max-width: 700px;
  margin: 0 auto;
}

.publish-card {
  margin-top: 20px;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.pay-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #e6a23c;
}

.pay-countdown strong {
  font-weight: 600;
}

.pay-expired-tip {
  margin-top: 12px;
}
</style>