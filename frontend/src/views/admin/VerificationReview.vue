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
          @change="onFilterChange"
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
      <el-table-column prop="nickname" label="昵称" width="120">
        <template #default="{ row }">
          {{ row.nickname || '-' }}
        </template>
      </el-table-column>
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
      <el-table-column prop="reviewedAt" label="审核时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.reviewedAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="reviewerId" label="审核人ID" width="100">
        <template #default="{ row }">
          {{ row.reviewerId || '-' }}
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
          <template v-else-if="row.status === 'APPROVED'">
            <span class="reviewed-tag approved">已通过</span>
          </template>
          <template v-else-if="row.status === 'REJECTED'">
            <span class="reviewed-tag rejected">已拒绝</span>
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
          @size-change="onPageSizeChange"
      />
    </div>

    <!-- Image Preview Dialog -->
    <el-dialog v-model="imageDialogVisible" title="认证图片" width="600px" @close="onImageDialogClose">
      <div v-loading="imageLoading" class="image-preview-wrapper">
        <el-image
            v-if="previewImageUrl"
            :src="previewImageUrl"
            fit="contain"
            style="width: 100%; max-height: 500px"
            :preview-src-list="[previewImageUrl]"
        />
        <el-empty v-else-if="!imageLoading" description="无法加载认证图片" />
      </div>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝认证" width="400px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input
              v-model="rejectForm.reason"
              type="textarea"
              :rows="3"
              placeholder="请输入拒绝原因（必填，1-200字符）"
              maxlength="200"
              show-word-limit
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
const imageLoading = ref(false)
const rejectDialogVisible = ref(false)
const currentReviewId = ref(null)
const rejectForm = reactive({ reason: '' })
const rejectFormRef = ref(null)

const previewImageUrl = ref('')

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const rejectRules = {
  reason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { min: 1, max: 200, message: '拒绝原因为1-200个字符', trigger: 'blur' }
  ]
}

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

async function showImage(row) {
  currentReviewId.value = row.id
  imageDialogVisible.value = true
  imageLoading.value = true
  // 先释放旧的 Object URL，防止快速切换时内存泄漏
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = ''
  try {
    const blob = await getReviewImage(row.id)
    // 成功后再次检查并设置新 URL
    if (previewImageUrl.value) {
      URL.revokeObjectURL(previewImageUrl.value)
    }
    previewImageUrl.value = URL.createObjectURL(blob)
  } catch {
    previewImageUrl.value = ''
  } finally {
    imageLoading.value = false
  }
}

function onImageDialogClose() {
  if (previewImageUrl.value) {
    URL.revokeObjectURL(previewImageUrl.value)
  }
  previewImageUrl.value = ''
  imageLoading.value = false
}

function onFilterChange() {
  pagination.page = 1
  loadList()
}

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
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getVerificationReviews(params)
    list.value = res?.list || []
    pagination.total = res?.total || 0
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(
        `确定通过用户「${row.username}（${row.realName}）」的实名认证吗？`,
        '审核通过',
        { confirmButtonText: '确定通过', cancelButtonText: '取消', type: 'success' }
    )
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
  rejectFormRef.value?.resetFields()
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectFormRef.value) return
  const valid = await rejectFormRef.value.validate().catch(() => false)
  if (!valid) return

  handling.value = true
  try {
    await handleReview(currentReviewId.value, {
      result: 'REJECT',
      reason: rejectForm.reason.trim()
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

.image-preview-wrapper {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reviewed-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}
.reviewed-tag.approved {
  color: #67c23a;
  background: #f0f9eb;
}
.reviewed-tag.rejected {
  color: #f56c6c;
  background: #fef0f0;
}
</style>