<template>
  <div v-loading="loading" class="user-public-profile">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>用户主页</span>
      </template>
    </el-page-header>

    <template v-if="user">
      <el-card class="profile-card" shadow="never">
        <div class="user-header">
          <!-- [修改] 使用异步加载的头像 Object URL -->
          <el-avatar :size="64" :src="avatarSrc" icon="UserFilled" />
          <div class="user-basic">
            <h2>{{ user.nickname || '匿名用户' }}</h2>
            <p v-if="user.college" class="college">{{ user.college }}</p>
            <p v-if="user.contact" class="contact">联系方式：{{ user.contact }}</p>
          </div>
        </div>
      </el-card>

      <!-- Rating Summary -->
      <el-card v-if="ratingSummary" class="rating-card" shadow="never">
        <template #header>
          <span>评价概览</span>
        </template>
        <div class="rating-summary">
          <div v-if="ratingSummary.publisherRoleSummary" class="rating-role">
            <span class="role-label">作为发布者：</span>
            <span class="rating-count">共 {{ ratingSummary.publisherRoleSummary.totalCount || 0 }} 条评价</span>
            <!-- [修改] 好评率乘以 100 显示百分比 -->
            <span class="positive-rate">好评率 {{ formatRate(ratingSummary.publisherRoleSummary.positiveRate) }}</span>
          </div>
          <div v-if="ratingSummary.acceptorRoleSummary" class="rating-role">
            <span class="role-label">作为接单者：</span>
            <span class="rating-count">共 {{ ratingSummary.acceptorRoleSummary.totalCount || 0 }} 条评价</span>
            <span class="positive-rate">好评率 {{ formatRate(ratingSummary.acceptorRoleSummary.positiveRate) }}</span>
          </div>
        </div>
      </el-card>

      <!-- Evaluation History -->
      <el-card class="eval-card" shadow="never">
        <template #header>
          <span>评价历史</span>
        </template>
        <div v-loading="evalLoading">
          <el-empty v-if="!evalLoading && evaluations.length === 0" description="暂无评价" :image-size="60" />
          <div v-for="evalItem in evaluations" :key="evalItem.evaluationId" class="eval-item">
            <div class="eval-header">
              <el-tag :type="getRatingTag(evalItem.ratingLevel)" size="small">
                {{ getRatingLabel(evalItem.ratingLevel) }}
              </el-tag>
              <span class="eval-time">{{ evalItem.createdAt }}</span>
            </div>
            <p class="eval-content">{{ evalItem.contentPreview }}</p>
            <p v-if="evalItem.revieweeRoleInBusiness" class="eval-from">
              角色：{{ evalItem.revieweeRoleInBusiness === 'PUBLISHER' ? '发布者' : '接单者' }}
            </p>
          </div>
        </div>
        <div v-if="evalTotal > evalPageSize" class="pagination-wrapper">
          <el-pagination
              v-model:current-page="evalPage"
              :page-size="evalPageSize"
              :total="evalTotal"
              layout="prev, pager, next"
              @current-change="loadEvaluations"
          />
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getUserProfile, getUserAvatar, getUserEvaluations } from '@/api/user'

const route = useRoute()
const loading = ref(true)
const evalLoading = ref(false)
const user = ref(null)
const ratingSummary = ref(null)
const evaluations = ref([])
const evalPage = ref(1)
const evalPageSize = 10
const evalTotal = ref(0)

// [新增] 头像 Object URL
const avatarSrc = ref('')

// [新增] 格式化好评率：将小数转为百分比字符串
function formatRate(rate) {
  if (rate == null) return '暂无'
  return (rate * 100).toFixed(1) + '%'
}

// [新增] 异步加载头像
async function loadAvatar() {
  const userId = route.params.id
  if (!userId) return
  try {
    const blob = await getUserAvatar(userId)
    if (avatarSrc.value) {
      URL.revokeObjectURL(avatarSrc.value)
    }
    avatarSrc.value = URL.createObjectURL(blob)
  } catch {
    avatarSrc.value = ''
  }
}

function getRatingTag(level) {
  const map = { GOOD: 'success', NEUTRAL: 'info', BAD: 'danger' }
  return map[level] || 'info'
}

function getRatingLabel(level) {
  const map = { GOOD: '好评', NEUTRAL: '中评', BAD: '差评' }
  return map[level] || level
}

async function loadUser() {
  loading.value = true
  try {
    const userId = route.params.id
    // [修复] getUserProfile(includeRating=true) 已包含完整 ratingSummary，无需再请求 getRatingSummary
    const profileRes = await getUserProfile(userId, true)
    user.value = profileRes
    ratingSummary.value = profileRes?.ratingSummary || null
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadEvaluations() {
  evalLoading.value = true
  try {
    const userId = route.params.id
    // [修改] 拦截器已返回 ApiResponse.data
    const res = await getUserEvaluations(userId, {
      page: evalPage.value,
      pageSize: evalPageSize
    })
    evaluations.value = res?.list || []
    evalTotal.value = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    evalLoading.value = false
  }
}

onMounted(() => {
  loadUser()
  loadEvaluations()
  loadAvatar()
})

watch(() => route.params.id, () => {
  if (route.params.id) {
    // [优化] 切换用户时重置评价分页到第一页，避免显示空白页
    evalPage.value = 1
    loadUser()
    loadEvaluations()
    loadAvatar()
  }
})
</script>

<style scoped>
.user-public-profile {
  max-width: 700px;
  margin: 0 auto;
}

.profile-card {
  margin-top: 20px;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-basic h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  color: #303133;
}

.college {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.contact {
  margin: 4px 0 0 0;
  color: #909399;
  font-size: 13px;
}

.rating-card {
  margin-top: 16px;
}

.rating-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rating-role {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-label {
  font-weight: 500;
  color: #303133;
}

.rating-count {
  color: #606266;
  font-size: 14px;
}

.positive-rate {
  color: #67c23a;
  font-weight: 500;
}

.eval-card {
  margin-top: 16px;
}

.eval-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.eval-item:last-child {
  border-bottom: none;
}

.eval-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.eval-time {
  color: #909399;
  font-size: 13px;
}

.eval-content {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.eval-from {
  margin: 4px 0 0 0;
  color: #909399;
  font-size: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>