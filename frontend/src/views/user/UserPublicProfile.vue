<template>
  <div class="user-public-profile">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>用户主页</span>
      </template>
    </el-page-header>

    <UserPublicProfilePanel :user-id="route.params.id" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import UserPublicProfilePanel from './UserPublicProfilePanel.vue'

const route = useRoute()
</script>

<style scoped>
.user-public-profile {
  width: min(calc(100% - 32px), 1120px);
  margin-left: max(16px, calc((100% - 1120px) / 2 - 16px));
  margin-right: auto;
}
</style>
