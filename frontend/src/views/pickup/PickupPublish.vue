<template>
  <div class="pickup-publish">
    <div class="publish-topbar">
      <el-page-header @back="$router.back()">
        <template #content>
          <span>发布代取</span>
        </template>
      </el-page-header>
      <el-button
          class="publish-submit-button"
          type="primary"
          :loading="loading"
          @click="handleSubmit"
      >
        发布代取
      </el-button>
    </div>

    <div class="publish-grid">
      <div class="publish-main">
        <!-- 基本信息 -->
        <el-card class="form-card basic-card" shadow="never">
          <template #header><span>基本信息</span></template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" label-position="left">
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
                  :rows="6"
                  placeholder="请描述需要代取的物品，越详细越好"
                  maxlength="500"
                  show-word-limit
              />
            </el-form-item>
          </el-form>
        </el-card>

      </div>

      <div class="deadline-column">
        <!-- 接单截止 -->
        <el-card class="form-card compact-card deadline-card" shadow="never">
          <template #header><span><span class="required-mark">*</span>接单截止</span></template>
          <el-form :model="form">
            <el-form-item prop="acceptDeadline">
              <el-calendar
                  v-model="deadlineDate"
                  class="deadline-calendar"
                  @update:model-value="onDeadlineDateChange"
              >
                <template #header="{ date }">
                  <div class="calendar-header">
                    <el-button class="calendar-nav-button" circle text @click.stop="shiftDeadlineMonth(-1)">
                      <el-icon><ArrowLeft /></el-icon>
                    </el-button>
                    <span class="calendar-title">{{ date }}</span>
                    <el-button class="calendar-nav-button" circle text @click.stop="shiftDeadlineMonth(1)">
                      <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </template>
                <template #date-cell="{ data }">
                  <div
                      class="calendar-day"
                      :class="{
                        'is-past': isBeforeToday(data.date),
                        'is-current': isSameDate(data.date, deadlineDate)
                      }"
                  >
                    {{ data.date.getDate() }}
                  </div>
                </template>
              </el-calendar>
              <div class="deadline-time-inputs">
                <div class="time-unit">
                  <el-input-number
                      v-model="deadlineTime.hour"
                      class="time-number"
                      controls-position="right"
                      :min="0"
                      :max="23"
                      :step="1"
                      :precision="0"
                      @change="syncDeadlineField"
                  />
                  <span class="time-label">时</span>
                </div>
                <div class="time-unit">
                  <el-input-number
                      v-model="deadlineTime.minute"
                      class="time-number"
                      controls-position="right"
                      :min="0"
                      :max="59"
                      :step="1"
                      :precision="0"
                      @change="syncDeadlineField"
                  />
                  <span class="time-label">分</span>
                </div>
                <div class="time-unit">
                  <el-input-number
                      v-model="deadlineTime.second"
                      class="time-number"
                      controls-position="right"
                      :min="0"
                      :max="59"
                      :step="1"
                      :precision="0"
                      @change="syncDeadlineField"
                  />
                  <span class="time-label">秒</span>
                </div>
              </div>
              <div class="deadline-preview">{{ deadlinePreview }}</div>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div class="publish-side">
        <!-- 报酬 -->
        <el-card class="form-card compact-card reward-card" shadow="never">
          <template #header><span><span class="required-mark">*</span>报酬</span></template>
          <div class="reward-compact">
            <div class="reward-row">
              <span class="reward-label">报酬类型</span>
              <el-radio-group v-model="form.rewardType" class="reward-switch">
                <el-radio-button value="PAID">有报酬</el-radio-button>
                <el-radio-button value="UNPAID">无报酬</el-radio-button>
              </el-radio-group>
            </div>
            <div class="reward-row">
              <span class="reward-label">报酬金额</span>
              <div class="reward-amount">
                <el-input-number
                    :model-value="form.rewardType === 'PAID' ? form.rewardAmount : 0"
                    :disabled="form.rewardType === 'UNPAID'"
                    :min="form.rewardType === 'PAID' ? 1 : 0"
                    :max="200"
                    :precision="0"
                    :step="1"
                    @update:model-value="form.rewardAmount = $event"
                />
                <span class="unit-text">积分</span>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 取件凭证 -->
        <el-card class="form-card compact-card" shadow="never">
          <template #header><span><span class="required-mark">*</span>取件凭证</span></template>
          <el-form ref="credentialFormRef" :model="form" :rules="rules" class="credential-form">
            <el-form-item prop="pickupCredential">
              <el-upload
                  ref="uploadRef"
                  class="credential-upload"
                  :class="{ 'credential-upload--filled': credentialFileList.length > 0 }"
                  :auto-upload="false"
                  :limit="1"
                  accept="image/jpeg,image/png"
                  list-type="picture-card"
                  :on-change="onFileChange"
                  :on-remove="onFileRemove"
                  :before-upload="beforeUpload"
                  :file-list="credentialFileList"
              >
                <el-icon v-if="credentialFileList.length === 0"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-form>
          <div class="upload-tip">JPG/PNG，不超过5MB，最多一张</div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createPickup } from '@/api/pickup'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const formRef = ref(null)
const credentialFormRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const createdId = ref(null)
const credentialFile = ref(null)
// 用于控制上传按钮显示的文件列表
const credentialFileList = ref([])

const form = reactive({
  campus: '',
  pickupLocation: '',
  deliveryLocation: '',
  itemDescription: '',
  rewardType: 'UNPAID',
  rewardAmount: 5,
  acceptDeadline: '',
  pickupCredential: null
})
const deadlineDate = ref(new Date())
const deadlineTime = reactive({
  hour: 18,
  minute: 0,
  second: 0
})
const deadlinePreview = computed(() => {
  if (!form.acceptDeadline) return '未选择'
  return form.acceptDeadline.replace('T', ' ').slice(0, 16)
})

const validateRewardAmount = (rule, value, callback) => {
  if (form.rewardType === 'PAID' && (!value || value < 1 || value > 200)) {
    callback(new Error('报酬金额为1-200积分'))
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
  acceptDeadline: [
    { required: true, message: '请选择接单截止时间', trigger: 'change' },
    { validator: validateDeadline, trigger: 'change' }
  ],
  pickupCredential: [
    { required: true, message: '请上传取件凭证图片', trigger: 'change' },
    { validator: validateCredential, trigger: 'change' }
  ]
}

function padDatePart(value) {
  return String(value).padStart(2, '0')
}

function buildDeadline(date) {
  const deadline = new Date(date)
  deadline.setHours(Number(deadlineTime.hour), Number(deadlineTime.minute), Number(deadlineTime.second), 0)
  return deadline
}

function formatDeadline(date) {
  const year = date.getFullYear()
  const month = padDatePart(date.getMonth() + 1)
  const day = padDatePart(date.getDate())
  const hour = padDatePart(date.getHours())
  const minute = padDatePart(date.getMinutes())
  const second = padDatePart(date.getSeconds())
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function isSameDate(a, b) {
  return a.getFullYear() === b.getFullYear()
      && a.getMonth() === b.getMonth()
      && a.getDate() === b.getDate()
}

function isBeforeToday(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(date)
  target.setHours(0, 0, 0, 0)
  return target < today
}

function hasCompleteDeadlineTime() {
  return deadlineTime.hour !== undefined
      && deadlineTime.hour !== null
      && deadlineTime.minute !== undefined
      && deadlineTime.minute !== null
      && deadlineTime.second !== undefined
      && deadlineTime.second !== null
}

function isDeadlineExpired() {
  return hasCompleteDeadlineTime() && isSameDate(deadlineDate.value, new Date()) && buildDeadline(deadlineDate.value) <= new Date()
}

function syncDeadlineField() {
  if (!hasCompleteDeadlineTime() || isDeadlineExpired()) {
    form.acceptDeadline = ''
  } else {
    form.acceptDeadline = formatDeadline(buildDeadline(deadlineDate.value))
  }
}

function setDeadlineDate(value) {
  deadlineDate.value = isBeforeToday(value) ? new Date() : new Date(value)
  syncDeadlineField()
}

function onDeadlineDateChange(value) {
  setDeadlineDate(value)
}

function shiftDeadlineMonth(offset) {
  const current = deadlineDate.value
  setDeadlineDate(new Date(current.getFullYear(), current.getMonth() + offset, 1))
}

function initDeadline() {
  const initialDeadline = new Date(Date.now() + 60 * 60 * 1000)
  deadlineDate.value = initialDeadline
  deadlineTime.hour = initialDeadline.getHours()
  deadlineTime.minute = initialDeadline.getMinutes()
  deadlineTime.second = 0
  syncDeadlineField()
}

initDeadline()

function beforeUpload(file) {
  if (file.size > 5 * 1024 * 1024) {
    showErrorMessage('取件凭证图片不能超过5MB')
    return false
  }
  return true
}

function onFileChange(file) {
  // 二次校验文件大小（处理某些绕过 beforeUpload 的场景）
  if (file.raw && file.raw.size > 5 * 1024 * 1024) {
    showErrorMessage('取件凭证图片不能超过5MB')
    // 使用公开方法清除文件列表，同时清空文件引用和文件列表
    uploadRef.value?.clearFiles()
    credentialFile.value = null
    credentialFileList.value = []
    form.pickupCredential = null
    credentialFormRef.value?.validateField('pickupCredential')
    return
  }
  credentialFile.value = file.raw
  form.pickupCredential = file.raw || null
  // 更新文件列表，控制按钮显隐（只保留当前文件）
  credentialFileList.value = [file]
  credentialFormRef.value?.validateField('pickupCredential')
}

function onFileRemove() {
  credentialFile.value = null
  credentialFileList.value = []
  form.pickupCredential = null
  credentialFormRef.value?.validateField('pickupCredential')
}

async function handleSubmit(event) {
  event?.currentTarget?.blur?.()
  const [basicValid, credentialValid] = await Promise.all([
    formRef.value.validate().catch(() => false),
    credentialFormRef.value.validate().catch(() => false)
  ])
  if (!basicValid || !credentialValid) return
  if (form.rewardType === 'PAID' && (!form.rewardAmount || form.rewardAmount < 1 || form.rewardAmount > 200)) {
    showErrorMessage('报酬金额为1-200积分')
    return
  }
  if (!form.acceptDeadline || new Date(form.acceptDeadline) <= new Date()) {
    showErrorMessage('请选择晚于当前时间的接单截止时间')
    return
  }
  if (!credentialFile.value) {
    showErrorMessage('请上传取件凭证图片')
    return
  }

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
    showSuccessMessage('发布成功')
    goDetail()
  } catch {
    // error handled by interceptor (409 = 积分不足)
  } finally {
    loading.value = false
  }
}

function goDetail() {
  if (createdId.value) {
    router.push(`/pickup/${createdId.value}`)
  } else {
    router.push('/hall')
  }
}
</script>

<style scoped>
.pickup-publish {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 24px;
}

.publish-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.publish-submit-button {
  height: 36px;
  min-width: 92px;
  border-radius: 7px;
  border: 0;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 58%, #ffc0cf 100%);
  box-shadow: 0 8px 18px rgba(255, 111, 159, 0.18);
}

.publish-submit-button:hover,
.publish-submit-button:focus {
  background: linear-gradient(135deg, #ff5f95 0%, #ff8fb4 58%, #ffb7ca 100%);
  box-shadow: 0 8px 18px rgba(255, 111, 159, 0.2);
}

.publish-submit-button:active,
.publish-submit-button.is-loading {
  background: linear-gradient(135deg, #ff6f9f 0%, #ff9fbd 58%, #ffc0cf 100%);
  box-shadow: 0 6px 14px rgba(255, 111, 159, 0.16);
}

.publish-grid {
  display: grid;
  grid-template-columns: minmax(440px, 1.25fr) minmax(320px, 0.9fr) minmax(230px, 0.6fr);
  gap: 18px;
  align-items: start;
  margin-top: 16px;
}

.publish-main,
.deadline-column,
.publish-side {
  min-width: 0;
}

.publish-main {
  display: flex;
  flex-direction: column;
}

.publish-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.deadline-column {
  display: flex;
  min-width: 0;
}

.basic-card,
.deadline-card {
  width: 100%;
}

.required-mark {
  margin-right: 4px;
  color: #f56c6c;
}

.deadline-card :deep(.el-card__body) {
  padding: 14px 16px 12px;
}

.deadline-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.deadline-card :deep(.el-form-item__content) {
  display: block;
}

.form-card {
  border-radius: 8px;
}

.compact-card :deep(.el-card__body) {
  padding-bottom: 8px;
}

.reward-card :deep(.el-card__header) {
  padding: 14px 16px;
}

.reward-card :deep(.el-card__body) {
  padding: 14px 16px 16px;
}

.reward-compact {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reward-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  align-items: center;
  column-gap: 8px;
}

.reward-label {
  color: #606266;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.reward-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 132px;
  max-width: 100%;
}

.reward-switch :deep(.el-radio-button__inner) {
  width: 66px;
  padding-left: 0;
  padding-right: 0;
  text-align: center;
  font-size: 13px;
  white-space: nowrap;
}

.deadline-calendar {
  --el-calendar-cell-width: 30px;
}

.deadline-calendar :deep(.el-calendar__header) {
  padding: 0 0 8px;
}

.deadline-calendar :deep(.el-calendar__body) {
  padding: 0;
}

.deadline-calendar :deep(.el-calendar-table thead th) {
  padding: 4px 0;
  color: #909399;
  font-size: 11px;
  font-weight: 500;
}

.deadline-calendar :deep(.el-calendar-table td) {
  border: 0;
}

.deadline-calendar :deep(.el-calendar-day) {
  height: 30px;
  padding: 0;
}

.deadline-calendar :deep(.el-calendar-table .el-calendar-day:hover) {
  background: transparent;
}

.deadline-calendar :deep(.el-calendar-table td.is-selected) {
  background: transparent;
}

.deadline-calendar :deep(.el-calendar-table td.is-selected .el-calendar-day) {
  background: transparent;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.calendar-nav-button {
  width: 24px;
  height: 24px;
}

.calendar-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.calendar-day {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin: 1px auto;
  border-radius: 6px;
  font-size: 12px;
  color: #303133;
}

.deadline-calendar :deep(.el-calendar-day:hover) .calendar-day:not(.is-current) {
  color: #409eff;
  background: #ecf5ff;
}

.calendar-day.is-current {
  color: #fff;
  background: #409eff;
}

.calendar-day.is-past {
  color: #c0c4cc;
  background: #f5f7fa;
}

.deadline-time-inputs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: center;
  column-gap: 8px;
  margin-top: 12px;
}

.time-unit {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 4px;
  min-width: 0;
}

.time-number {
  width: 100%;
  min-width: 0;
}

.time-number :deep(.el-input) {
  width: 100%;
}

.time-number :deep(.el-input__wrapper) {
  padding-left: 4px;
  padding-right: 18px;
}

.time-number :deep(.el-input__inner) {
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.time-number :deep(.el-input-number__increase),
.time-number :deep(.el-input-number__decrease) {
  width: 16px;
}

.time-label {
  flex: 0 0 auto;
  color: #606266;
  font-size: 12px;
  margin-left: 3px;
}

.deadline-preview {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
  text-align: right;
}

.reward-amount {
  display: flex;
  align-items: center;
  min-width: 0;
}

.reward-amount :deep(.el-input-number) {
  width: 118px;
}

.reward-amount :deep(.el-input-number .el-input__wrapper) {
  padding-left: 24px;
  padding-right: 24px;
}

.reward-amount :deep(.el-input-number__increase),
.reward-amount :deep(.el-input-number__decrease) {
  width: 24px;
}

.basic-card :deep(.el-card__body) {
  padding-bottom: 18px;
}

.unit-text {
  margin-left: 6px;
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  text-align: center;
}

.publish-side :deep(.el-upload--picture-card),
.publish-side :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 112px;
  height: 112px;
}

.credential-upload {
  display: flex;
  justify-content: center;
  min-height: 112px;
  line-height: 0;
}

.credential-upload :deep(.el-upload-list--picture-card) {
  display: grid;
  grid-template-columns: 112px;
  justify-content: center;
  margin: 0;
  line-height: 0;
}

.credential-upload :deep(.el-upload-list--picture-card .el-upload-list__item) {
  margin: 0;
}

.credential-upload--filled :deep(.el-upload--picture-card) {
  display: none;
}

@media (max-width: 900px) {
  .pickup-publish {
    max-width: 700px;
  }

  .publish-grid {
    grid-template-columns: 1fr;
  }

  .basic-card {
    min-height: auto;
  }

}
</style>
