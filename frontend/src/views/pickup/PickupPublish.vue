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
            <el-option label="鼓楼校区" value="鼓楼校区" />
            <el-option label="仙林校区" value="仙林校区" />
            <el-option label="苏州校区" value="苏州校区" />
            <el-option label="浦口校区" value="浦口校区" />
          </el-select>
        </el-form-item>

        <el-form-item label="取件地点" prop="pickupLocation">
          <el-input v-model="form.pickupLocation" placeholder="请输入取件地点" maxlength="100" />
        </el-form-item>

        <el-form-item label="送达地点" prop="deliveryLocation">
          <el-input v-model="form.deliveryLocation" placeholder="请输入送达地点" maxlength="100" />
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
            placeholder="请选择接单截止时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="取件凭证">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            list-type="picture-card"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">上传取件凭证图片（如快递截图），可选</div>
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
    <el-dialog v-model="showPayDialog" title="支付提示" width="400px" :close-on-click-modal="false">
      <p>代取请求已发布，请完成支付。</p>
      <template #footer>
        <el-button @click="goDetail">稍后支付</el-button>
        <el-button type="primary" @click="goPay">去支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPickup, getPaymentEntry } from '@/api/pickup'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const showPayDialog = ref(false)
const createdId = ref(null)
const payUrl = ref('')
const credentialFile = ref(null)

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

const rules = {
  campus: [{ required: true, message: '请输入校区', trigger: 'blur' }],
  pickupLocation: [{ required: true, message: '请输入取件地点', trigger: 'blur' }],
  deliveryLocation: [{ required: true, message: '请输入送达地点', trigger: 'blur' }],
  rewardType: [{ required: true, message: '请选择报酬类型', trigger: 'change' }],
  rewardAmount: [{ validator: validateRewardAmount, trigger: 'change' }],
  acceptDeadline: [{ required: true, message: '请选择接单截止时间', trigger: 'change' }]
}

function disabledDate(date) {
  return date < new Date(Date.now() - 86400000)
}

function onFileChange(file) {
  credentialFile.value = file.raw
}

function onFileRemove() {
  credentialFile.value = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const fd = new FormData()
  fd.append('campus', form.campus)
  fd.append('pickupLocation', form.pickupLocation)
  fd.append('deliveryLocation', form.deliveryLocation)
  fd.append('itemDescription', form.itemDescription || '')
  fd.append('rewardType', form.rewardType)
  fd.append('acceptDeadline', form.acceptDeadline)
  if (form.rewardType === 'PAID') {
    fd.append('rewardAmount', String(form.rewardAmount))
  }
  if (credentialFile.value) {
    fd.append('pickupCredential', credentialFile.value)
  }

  loading.value = true
  try {
    const res = await createPickup(fd)
    createdId.value = res.data?.pickupId
    ElMessage.success('发布成功')

    if (form.rewardType === 'PAID' && createdId.value) {
      try {
        const payRes = await getPaymentEntry(createdId.value)
        payUrl.value = payRes.data?.payEntry || payRes.data?.payUrl || ''
        if (payUrl.value) {
          showPayDialog.value = true
          return
        }
      } catch {
        // fallback to detail page
      }
    }
    goDetail()
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function goPay() {
  if (payUrl.value) {
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
</style>
