<template>
  <div v-loading="loading" class="point-detail">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>积分详情</span>
      </template>
    </el-page-header>

    <el-card class="detail-card" shadow="never">
      <div class="balance-section">
        <div class="balance-label">当前积分余额</div>
        <div class="balance-value">{{ pointBalance }}</div>
        <el-button
            type="primary"
            :disabled="checkedIn"
            :loading="checkInLoading"
            @click="handleCheckIn"
        >
          {{ checkedIn ? '今日已签到' : '每日签到 +5' }}
        </el-button>
      </div>

      <el-divider />

      <el-descriptions :column="2" border class="detail-info">
        <el-descriptions-item label="积分性质">平台虚拟资产，不可充值提现</el-descriptions-item>
        <el-descriptions-item label="签到奖励">每日签到获得 5 积分</el-descriptions-item>
        <el-descriptions-item label="发布代取">有报酬代取发布时扣减对应积分</el-descriptions-item>
        <el-descriptions-item label="完成代取">有报酬代取完成后积分转入接单方</el-descriptions-item>
        <el-descriptions-item label="取消代取">有报酬代取取消时积分退回发布方</el-descriptions-item>
        <el-descriptions-item label="实名认证">认证通过后赠送积分</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPointBalance, checkIn } from '@/api/points'

const loading = ref(true)
const checkInLoading = ref(false)
const pointBalance = ref(0)
const checkedIn = ref(false)

async function loadBalance() {
  loading.value = true
  try {
    const res = await getPointBalance()
    pointBalance.value = res?.pointBalance ?? 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleCheckIn() {
  checkInLoading.value = true
  try {
    const res = await checkIn()
    pointBalance.value = res?.pointBalance ?? pointBalance.value
    checkedIn.value = true
    ElMessage.success(`签到成功，获得 ${res?.earnedPoints ?? 5} 积分`)
  } catch (err) {
    if (err?.errors?.reason === 'ALREADY_CHECKED_IN_TODAY') {
      checkedIn.value = true
    }
  } finally {
    checkInLoading.value = false
  }
}

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.point-detail {
  max-width: 700px;
  margin: 0 auto;
}

.detail-card {
  margin-top: 20px;
}

.balance-section {
  text-align: center;
  padding: 20px 0;
}

.balance-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.balance-value {
  font-size: 48px;
  font-weight: bold;
  color: #67c23a;
  margin-bottom: 16px;
}

.detail-info {
  margin-top: 8px;
}
</style>
