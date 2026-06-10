<template>
  <div class="pickup-hall">
    <div class="hall-toolbar">
      <div class="filter-bar">
        <el-select
            v-model="filters.campus"
            placeholder="选择校区"
            clearable
            style="width: 160px"
        >
          <el-option label="全部校区" value="" />
          <el-option label="鼓楼校区" value="GULOU" />
          <el-option label="仙林校区" value="XIANLIN" />
          <el-option label="苏州校区" value="SUZHOU" />
          <el-option label="浦口校区" value="PUKOU" />
        </el-select>
        <el-select
            v-model="filters.rewardType"
            placeholder="报酬类型"
            clearable
            style="width: 150px"
        >
          <el-option label="全部" value="" />
          <el-option label="有报酬" value="PAID" />
          <el-option label="无报酬" value="UNPAID" />
        </el-select>
        <el-button type="primary" @click="onFilterChange">搜索</el-button>
      </div>
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
          <div class="card-tags">
            <el-tag :type="item.rewardType === 'PAID' ? 'success' : 'info'" size="small">
              {{ item.rewardType === 'PAID' ? '有报酬' : '无报酬' }}
            </el-tag>
          </div>
          <span v-if="item.rewardType === 'PAID' && item.rewardAmount" class="reward-amount">
            {{ item.rewardAmount }} 积分
          </span>
        </div>
        <div class="card-body">
          <div class="location-row">
            <el-icon><Location /></el-icon>
            <span>{{ campusLabel(item.campus) }} - {{ item.pickupLocation }}</span>
          </div>
          <div class="location-row">
            <el-icon><Promotion /></el-icon>
            <span>{{ item.deliveryLocation }}</span>
          </div>
          <!-- 物品描述预览 -->
          <div class="desc-preview" v-if="item.itemDescriptionPreview">
            {{ item.itemDescriptionPreview }}
          </div>
          <!-- 接单截止时间 -->
          <div v-if="item.acceptDeadline" class="deadline-row">
            <el-icon><Clock /></el-icon>
            <span>接单截止：{{ formatTime(item.acceptDeadline) }}</span>
          </div>
        </div>
        <div class="card-footer">
          <span class="publisher" @click.stop="goUserProfile(item.publisher?.userId)">
            <el-icon><User /></el-icon>
            {{ item.publisher?.nickname || '匿名用户' }}
          </span>
          <span class="time">{{ formatTime(item.createdAt) }}</span>
        </div>
      </el-card>
    </div>

    <div ref="loadMoreSentinel" class="load-more-wrapper">
      <span v-if="loading && list.length > 0" class="load-more-text">加载中...</span>
      <span v-else-if="hasMore && list.length > 0" class="load-more-text">继续下滑加载更多</span>
      <span v-else-if="list.length > 0" class="no-more-text">没有更多了</span>
    </div>

    <button
        v-show="showBackTop"
        class="back-top-button"
        type="button"
        aria-label="回到顶部"
        @click="scrollToTop"
    >
      <el-icon class="back-top-icon"><CaretTop /></el-icon>
      <span class="back-top-text">顶部</span>
    </button>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getPickupList } from '@/api/pickup'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const loadMoreSentinel = ref(null)
const showBackTop = ref(false)

const filters = reactive({
  campus: '',
  rewardType: ''
})

const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
let loadMoreObserver = null
const backTopVisibleOffset = () => Math.max(900, window.innerHeight * 1.25)

const campusMap = {
  GULOU: '鼓楼校区',
  XIANLIN: '仙林校区',
  SUZHOU: '苏州校区',
  PUKOU: '浦口校区'
}
const campusLabel = (code) => campusMap[code] || code

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  // 未来时间（如接单截止时间）显示完整日期
  if (diff < 0) {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function onFilterChange() {
  page.value = 1
  hasMore.value = true
  list.value = []
  loadList()
}

async function loadList(append = false) {
  if (loading.value) return
  loading.value = true
  let loaded = false
  const scrollYBeforeAppend = append && typeof window !== 'undefined' ? window.scrollY : null
  try {
    const params = {
      page: page.value,
      pageSize
    }
    if (filters.campus) params.campus = filters.campus
    if (filters.rewardType) params.rewardType = filters.rewardType
    const res = await getPickupList(params)
    const fetched = res?.list || []
    if (append) {
      list.value.push(...fetched)
      await nextTick()
      if (scrollYBeforeAppend !== null && Math.abs(window.scrollY - scrollYBeforeAppend) > 1) {
        window.scrollTo({
          top: scrollYBeforeAppend,
          behavior: 'auto'
        })
      }
    } else {
      list.value = fetched
    }
    hasMore.value = fetched.length >= pageSize
    loaded = true
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
    if (loaded && !append) {
      await maybeLoadMoreWhenVisible()
    }
  }
}

function loadMore() {
  if (loading.value || !hasMore.value) return
  page.value++
  loadList(true)
}

function isSentinelVisible() {
  if (!loadMoreSentinel.value || typeof window === 'undefined') return false
  const rect = loadMoreSentinel.value.getBoundingClientRect()
  return rect.top <= window.innerHeight + 240
}

async function maybeLoadMoreWhenVisible() {
  await nextTick()
  if (hasMore.value && !loading.value && isSentinelVisible()) {
    loadMore()
  }
}

function setupAutoLoadMore() {
  if (typeof window === 'undefined' || !('IntersectionObserver' in window) || !loadMoreSentinel.value) return
  loadMoreObserver = new IntersectionObserver((entries) => {
    if (entries.some((entry) => entry.isIntersecting)) {
      loadMore()
    }
  }, {
    rootMargin: '240px 0px'
  })
  loadMoreObserver.observe(loadMoreSentinel.value)
}

function goDetail(id) {
  router.push(`/pickup/${id}`)
}

function goUserProfile(userId) {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

function updateBackTopVisible() {
  showBackTop.value = window.scrollY > backTopVisibleOffset()
}

function scrollToTop() {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  setupAutoLoadMore()
  updateBackTopVisible()
  window.addEventListener('scroll', updateBackTopVisible, { passive: true })
  loadList()
})

onUnmounted(() => {
  if (loadMoreObserver) {
    loadMoreObserver.disconnect()
    loadMoreObserver = null
  }
  window.removeEventListener('scroll', updateBackTopVisible)
})
</script>

<style scoped>
.pickup-hall {
  padding: 0;
}

.hall-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-bar {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.pickup-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
  align-items: start;
}

.pickup-card {
  cursor: pointer;
  transition: transform 0.2s;
  min-width: 0;
}

.pickup-card:hover {
  transform: translateY(-2px);
}

@media (max-width: 900px) {
  .pickup-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 600px) {
  .hall-toolbar,
  .filter-bar {
    justify-content: flex-start;
  }

  .pickup-list { grid-template-columns: 1fr; }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  gap: 8px;
  align-items: center;
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

.desc-preview {
  font-size: 14px;
  color: #303133;
  margin-top: 8px;
  margin-bottom: 8px;
  line-height: 1.5;
  word-break: break-word;
}

.deadline-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #e6a23c;
  font-size: 13px;
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
  cursor: pointer;
}

.publisher:hover {
  color: #409eff;
}

.load-more-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px 0;
}

.no-more-text {
  color: #909399;
  font-size: 14px;
}

.load-more-text {
  color: #606266;
  font-size: 14px;
}

.back-top-button {
  position: fixed;
  right: max(12px, calc((100vw - 1200px) / 2 - 52px));
  bottom: 28px;
  z-index: 2500;
  width: 42px;
  min-height: 48px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  color: #606266;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  line-height: 1;
  padding: 3px 2px;
  transition: color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.back-top-button:hover {
  color: #409eff;
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.16);
  transform: translateY(-2px);
}

.back-top-button:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 3px;
}

.back-top-icon {
  width: 30px;
  height: 30px;
  font-size: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-top-text {
  font-size: 12px;
  line-height: 14px;
  color: inherit;
}

@media (max-width: 600px) {
  .back-top-button {
    right: 12px;
    bottom: 20px;
  }
}
</style>
