<template>
  <div class="transaction-list">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>交易记录</span>
      </template>
    </el-page-header>

    <div class="filter-bar">
      <!-- [修改] 选项值改为 PAYMENT / SETTLEMENT / REFUND -->
      <el-select
          v-model="typeFilter"
          placeholder="交易类型"
          clearable
          style="width: 160px"
          @change="onFilterChange"
      >
        <el-option label="全部" value="" />
        <el-option label="支付" value="PAYMENT" />
        <el-option label="结算" value="SETTLEMENT" />
        <el-option label="退款" value="REFUND" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe style="width: 100%" @row-click="goDetail">
      <el-table-column prop="paymentId" label="ID" width="80" />
      <!-- [新增] 交易类型列 -->
      <el-table-column label="交易类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务类型" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ getBusinessLabel(row.businessType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="{ row }">
          <span class="amount">¥{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadList"
          @size-change="onPageSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTransactions } from '@/api/payment'

const router = useRouter()

const loading = ref(false)
const typeFilter = ref('')  // [修改] 存储 PAYMENT/SETTLEMENT/REFUND 或 ''
const list = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// [新增] 交易类型映射
const typeLabelMap = {
  PAYMENT: '支付',
  SETTLEMENT: '结算',
  REFUND: '退款'
}
function getTypeLabel(type) {
  return typeLabelMap[type] || type
}

const businessLabelMap = {
  PICKUP_REQUEST: '代取服务'
}
function getBusinessLabel(type) {
  return businessLabelMap[type] || type
}

const statusLabelMap = {
  WAITING_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
  SETTLED: '已结算'
}
function getStatusLabel(status) {
  return statusLabelMap[status] || status
}

function getStatusTag(status) {
  const map = {
    WAITING_PAY: 'warning',
    PAID: 'success',
    CLOSED: 'info',
    REFUNDED: 'warning',
    SETTLED: 'success'
  }
  return map[status] || 'info'
}

// [新增] 筛选变化时重置页码
function onFilterChange() {
  pagination.page = 1
  loadList()
}

// [新增] pageSize 变化时重置页码
function onPageSizeChange() {
  pagination.page = 1
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (typeFilter.value) {
      params.type = typeFilter.value
    }
    // [修改] 拦截器已返回 data，直接使用 res
    const res = await getTransactions(params)
    list.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})

function goDetail(row) {
  if (row?.paymentId) {
    router.push(`/payment/${row.paymentId}`)
  }
}
</script>

<style scoped>
.transaction-list {
  max-width: 900px;
  margin: 0 auto;
}

.filter-bar {
  margin: 20px 0;
}

.amount {
  font-weight: 500;
  color: #303133;
}

:deep(.el-table__row) {
  cursor: pointer;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>