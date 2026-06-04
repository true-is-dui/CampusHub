<template>
  <div class="pickup-hall">
    <div class="filter-bar">
      <el-select
        v-model="filters.campus"
        placeholder="选择校区"
        clearable
        style="width: 160px"
        @change="loadList"
      >
        <el-option label="全部校区" value="" />
        <el-option label="鼓楼校区" value="鼓楼校区" />
        <el-option label="仙林校区" value="仙林校区" />
        <el-option label="苏州校区" value="苏州校区" />
        <el-option label="浦口校区" value="浦口校区" />
      </el-select>
      <el-select
        v-model="filters.rewardType"
        placeholder="报酬类型"
        clearable
        style="width: 150px"
        @change="loadList"
      >
        <el-option label="全部" value="" />
        <el-option label="有报酬" value="PAID" />
        <el-option label="无报酬" value="UNPAID" />
      </el-select>
      <el-button type="primary" @click="loadList">搜索</el-button>
    </div>

    <div v-loading="loading" class="pickup-list">
      <el-empty v-if="!loading && list.length === 0" description="暂无代取请求" />
      <el-card
        v-for="item in list"
        :key="item.pickupId"
        class="pickup-card"
        shadow="hover"
        @click="goDetail(item.pickupId)"
      >
        <div class="card-header">
          <el-tag :type="item.rewardType === 'PAID' ? 'success' : 'info'" size="small">
            {{ item.rewardType === 'PAID' ? '有报酬' : '无报酬' }}
          </el-tag>
          <span v-if="item.rewardType === 'PAID' && item.rewardAmount" class="reward-amount">
            ¥{{ item.rewardAmount }}
          </span>
        </div>
        <div class="card-body">
          <div class="location-row">
            <el-icon><Location /></el-icon>
            <span>{{ item.campus }} - {{ item.pickupLocation }}</span>
          </div>
          <div class="location-row">
            <el-icon><Promotion /></el-icon>
            <span>{{ item.deliveryLocation }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="publisher">
            <el-icon><User /></el-icon>
            {{ item.publisher?.nickname || '匿名用户' }}
          </span>
          <span class="time">{{ formatTime(item.createdAt) }}</span>
        </div>
      </el-card>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList"
        @size-change="loadList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPickupList } from '@/api/pickup'

const router = useRouter()
const loading = ref(false)
const list = ref([])

const filters = reactive({
  campus: '',
  rewardType: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filters.campus) params.campus = filters.campus
    if (filters.rewardType) params.rewardType = filters.rewardType
    const res = await getPickupList(params)
    list.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/pickup/${id}`)
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.pickup-hall {
  padding: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.pickup-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.pickup-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.pickup-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.reward-amount {
  font-size: 18px;
  font-weight: bold;
  color: #e6a23c;
}

.card-body {
  margin-bottom: 12px;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  color: #606266;
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 13px;
}

.publisher {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
