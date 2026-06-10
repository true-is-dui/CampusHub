<template>
  <el-tag :type="tagType" :size="size" :class="['rating-tag', `rating-tag--${size}`]">
    <RatingLevelLabel :level="level" />
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import RatingLevelLabel from './RatingLevelLabel.vue'

const props = defineProps({
  level: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'small'
  }
})

const tagType = computed(() => {
  const map = { GOOD: 'success', NEUTRAL: 'warning', BAD: 'danger' }
  return map[props.level] || 'info'
})

</script>

<style scoped>
.rating-tag :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rating-tag :deep(.rating-level-icon) {
  width: 13px;
  height: 13px;
}

.rating-tag--large :deep(.rating-level-icon) {
  width: 15px;
  height: 15px;
}
</style>
