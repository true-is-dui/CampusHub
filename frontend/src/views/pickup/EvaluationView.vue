<template>
  <div v-loading="loading" class="evaluation-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>评价</span>
      </template>
    </el-page-header>

    <template v-if="eligibility">
      <!-- 可以评价 -->
      <el-card v-if="eligibility.canEvaluate" class="evaluation-card" shadow="never">
        <div class="reviewee-info">
          <el-avatar :size="48" :src="revieweeAvatarUrl" icon="UserFilled" />
          <div class="reviewee-text">
            <span class="reviewee-label">评价对象</span>
            <span class="reviewee-name">{{ eligibility.reviewee?.nickname || '匿名用户' }}</span>
          </div>
        </div>

        <el-divider />

        <el-form :model="evalForm" label-width="80px">
          <el-form-item label="评分" required>
            <el-radio-group v-model="evalForm.ratingLevel">
              <el-radio value="GOOD">好评</el-radio>
              <el-radio value="NEUTRAL">中评</el-radio>
              <el-radio value="BAD">差评</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
                v-model="evalForm.content"
                type="textarea"
                :rows="4"
                placeholder="请输入评价内容（差评必填）"
                :maxlength="300"
                show-word-limit
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              提交评价
            </el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 不可评价 -->
      <el-card v-else class="evaluation-card" shadow="never">
        <el-result
            icon="info"
            title="暂不可评价"
            :sub-title="reasonLabel"
        />
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getEvaluationEligibility, submitEvaluation } from '@/api/pickup'
import { getUserAvatar } from '@/api/user'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const eligibility = ref(null)
const revieweeAvatarUrl = ref('')

const evalForm = reactive({
  ratingLevel: 'GOOD',
  content: ''
})

// [新增] 不可评价原因映射
const reasonMap = {
  NOT_COMPLETED: '代取服务尚未完成，完成后才能评价',
  NOT_PARTICIPANT: '您不是该代取服务的参与者，无法评价',
  ALREADY_EVALUATED: '您已经评价过该服务',
  ACCOUNT_NOT_ALLOWED: '当前账号状态不允许评价'
}
const reasonLabel = computed(() => {
  return reasonMap[eligibility.value?.reason] || eligibility.value?.reason || '无法评价'
})

async function loadEligibility() {
  loading.value = true
  try {
    const pickupId = route.params.pickupId
    const res = await getEvaluationEligibility(pickupId)
    eligibility.value = res

    // 加载被评价人头像
    if (res?.canEvaluate && res?.reviewee?.userId) {
      try {
        const blob = await getUserAvatar(res.reviewee.userId)
        revieweeAvatarUrl.value = URL.createObjectURL(blob)
      } catch { /* ignore */ }
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!evalForm.ratingLevel) {
    ElMessage.warning('请选择评分')
    return
  }
  // [修复] 差评必须填写内容
  if (evalForm.ratingLevel === 'BAD' && !evalForm.content?.trim()) {
    ElMessage.warning('差评必须填写评价内容')
    return
  }

  submitting.value = true
  try {
    await submitEvaluation(route.params.pickupId, evalForm)
    ElMessage.success('评价成功')
    router.push(`/pickup/${route.params.pickupId}`)
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadEligibility()
})
</script>

<style scoped>
.evaluation-page {
  max-width: 600px;
  margin: 0 auto;
}

.evaluation-card {
  margin-top: 20px;
}

.reviewee-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reviewee-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reviewee-label {
  font-size: 13px;
  color: #909399;
}

.reviewee-name {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
</style>