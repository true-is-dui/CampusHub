<template>
  <div class="verification-review">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>认证审核</span>
      </template>
    </el-page-header>

    <div class="filter-bar">
      <el-select
        v-model="statusFilter"
        placeholder="审核状态"
        clearable
        style="width: 160px"
        @change="loadList"
      >
        <el-option label="全部" value="" />
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="studentIdMasked" label="学号" width="150" />
      <el-table-column label="认证图片" width="120">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="showImage(row)">
            查看
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="拒绝原因" min-width="150">
        <template #default="{ row }">
          {{ row.rejectReason || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button type="success" size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(row)">
              拒绝
            </el-button>
          </template>
          <span v-else>-</span>
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
        @size-change="loadList"
      />
    </div>

    <!-- Image Preview Dialog -->
    <el-dialog v-model="imageDialogVisible" title="认证图片" width="600px">
      <el-image
        v-if="currentReviewId"
        :src="getImageUrl(currentReviewId)"
        fit="contain"
        style="width: 100%; max-height: 500px"
      />
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝认证" width="400px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="handling" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVerificationReviews, handleReview, getReviewImage } from '@/api/admin'

const loading = ref(false)
const handling = ref(false)
const statusFilter = ref('')
const list = ref([])
const imageDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentReviewId = ref(null)
const rejectForm = reactive({ reason: '' })

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const statusLabelMap = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

function getStatusLabel(status) {
  return statusLabelMap[status] || status
}

function getStatusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getImageUrl(reviewId) {
  return getReviewImage(reviewId)
}

function showImage(row) {
  currentReviewId.value = row.id
  imageDialogVisible.value = true
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getVerificationReviews(params)
    list.value = res.data?.records || res.data?.list || []
    pagination.total = res.data?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(`确认通过用户 ${row.username} 的认证？`, '审核通过', { type: 'info' })
    handling.value = true
    await handleReview(row.id, { result: 'APPROVE' })
    ElMessage.success('已通过')
    loadList()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  } finally {
    handling.value = false
  }
}

function showRejectDialog(row) {
  currentReviewId.value = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  handling.value = true
  try {
    await handleReview(currentReviewId.value, {
      result: 'REJECT',
      reason: rejectForm.reason
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadList()
  } catch {
    // error handled by interceptor
  } finally {
    handling.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.verification-review {
  max-width: 1100px;
  margin: 0 auto;
}

.filter-bar {
  margin: 20px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
