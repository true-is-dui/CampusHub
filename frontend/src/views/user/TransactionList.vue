<template>
  <div class="transaction-list">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>交易记录</span>
      </template>
    </el-page-header>

    <div class="filter-bar">
      <el-select
        v-model="typeFilter"
        placeholder="交易类型"
        clearable
        style="width: 160px"
        @change="loadList"
      >
        <el-option label="全部" value="" />
        <el-option label="我支付的" value="PAYER" />
        <el-option label="我收到的" value="RECEIVER" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
      <el-table-column prop="paymentId" label="ID" width="80" />
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
      <el-table-column prop="closeReason" label="关闭原因" min-width="150">
        <template #default="{ row }">
          {{ row.closeReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column prop="statusChangedAt" label="状态变更时间" width="170" />
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTransactions } from '@/api/payment'

const loading = ref(false)
const typeFilter = ref('')
const list = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const businessLabelMap = {
  PICKUP_REQUEST: '代取服务'
}

const statusLabelMap = {
  WAITING_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
  SETTLED: '已结算'
}

function getBusinessLabel(type) {
  return businessLabelMap[type] || type
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

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (typeFilter.value) params.type = typeFilter.value
    const res = await getTransactions(params)
    list.value = res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
