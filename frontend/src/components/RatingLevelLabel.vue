<template>
  <span class="rating-level-label">
    <span class="rating-level-icon" aria-hidden="true">
      <svg v-if="level === 'GOOD'" viewBox="0 0 24 24" focusable="false">
        <path d="M7 21H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3v11Zm2 0V10.7l4.4-7.1c.4-.6 1.2-.8 1.8-.5 1.2.6 1.6 2 1.1 3.2L15 10h4.6c1.4 0 2.5 1.2 2.4 2.6l-.7 5.7A3 3 0 0 1 18.4 21H9Z" />
      </svg>
      <svg v-else-if="level === 'BAD'" viewBox="0 0 24 24" focusable="false">
        <path d="M7 3H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h3V3Zm2 0v10.3l4.4 7.1c.4.6 1.2.8 1.8.5 1.2-.6 1.6-2 1.1-3.2L15 14h4.6c1.4 0 2.5-1.2 2.4-2.6l-.7-5.7A3 3 0 0 0 18.4 3H9Z" />
      </svg>
      <svg v-else viewBox="0 0 24 24" focusable="false">
        <path d="M6 10.75h12a1.25 1.25 0 1 1 0 2.5H6a1.25 1.25 0 1 1 0-2.5Z" />
      </svg>
    </span>
    <span>{{ label }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  level: {
    type: String,
    default: ''
  }
})

const label = computed(() => {
  const map = { GOOD: '好评', NEUTRAL: '中评', BAD: '差评' }
  return map[props.level] || props.level
})
</script>

<style scoped>
.rating-level-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rating-level-icon {
  width: 13px;
  height: 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.rating-level-icon svg {
  width: 100%;
  height: 100%;
  display: block;
  fill: currentColor;
}
</style>
