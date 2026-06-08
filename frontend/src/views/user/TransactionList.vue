<template>
  <div class="transaction-list">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>积分流水</span>
      </template>
    </el-page-header>

    <div class="balance-bar">
      <span class="balance-label">当前积分余额：</span>
      <span class="balance-value">{{ pointBalance }}</span>
    </div>

    <div class="filter-bar">
      <el-select
          v-model="typeFilter"
          placeholder="流水类型"
          clearable
          style="width: 180px"
          @change="onFilterChange"
      >
        <el-option label="全部" value="" />
        <el-option label="实名认证赠送" value="EARN_VERIFICATION" />
        <el-option label="每日签到" value="EARN_CHECK_IN" />
        <el-option label="发布扣减" value="SPEND_PUBLISH" />
        <el-option label="取消退回" value="REFUND_CANCEL" />
        <el-option label="完成入账" value="INCOME_COMPLETE" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
      <el-table-column prop="transactionId" label="ID" width="80" />
      <el-table-column label="流水类型" width="140">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="积分变动" width="120">
        <template #default="{ row }">
          <span :class="row.amount > 0 ? 'amount-income' : 'amount-spend'">
            {{ row.amount > 0 ? '+' : '' }}{{ row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="balanceAfter" label="变动后余额" width="120" />
      <el-table-column label="关联代取" width="100">
        <template #default="{ row }">
          <el-link
              v-if="row.relatedPickupId"
              type="primary"
              @click.stop="$router.push(`/pickup/${row.relatedPickupId}`)"
          >
            #{{ row.relatedPickupId }}
          </el-link>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
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
import { getPointBalance, getPointTransactions } from '@/api/points'

const loading = ref(false)
const typeFilter = ref('')
const list = ref([])
const pointBalance = ref(0)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const typeLabelMap = {
  EARN_VERIFICATION: '实名认证赠送',
  EARN_CHECK_IN: '每日签到',
  SPEND_PUBLISH: '发布扣减',
  REFUND_CANCEL: '取消退回',
  INCOME_COMPLETE: '完成入账'
}

function getTypeLabel(type) {
  return typeLabelMap[type] || type
}

function getTypeTag(type) {
  if (type === 'SPEND_PUBLISH') return 'danger'
  return 'success'
}

function onFilterChange() {
  pagination.page = 1
  loadList()
}

function onPageSizeChange() {
  pagination.page = 1
  loadList()
}

async function loadBalance() {
  try {
    const res = await getPointBalance()
    pointBalance.value = res?.pointBalance ?? 0
  } catch {
    // error handled by interceptor
  }
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
    const res = await getPointTransactions(params)
    list.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadBalance()
  loadList()
})
</script>

<style scoped>
.transaction-list {
  max-width: 900px;
  margin: 0 auto;
}

.balance-bar {
  margin: 20px 0 0;
  padding: 16px 20px;
  background: #f0f9eb;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.balance-label {
  font-size: 14px;
  color: #606266;
}

.balance-value {
  font-size: 24px;
  font-weight: bold;
  color: #67c23a;
}

.filter-bar {
  margin: 20px 0;
}

.amount-income {
  font-weight: 500;
  color: #67c23a;
}

.amount-spend {
  font-weight: 500;
  color: #f56c6c;
}

.text-muted {
  color: #c0c4cc;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
