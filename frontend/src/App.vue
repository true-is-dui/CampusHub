<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

.el-avatar:not(.profile-avatar):not(.nav-avatar):not(.card-avatar) .el-icon {
  width: 58%;
  height: 58%;
  font-size: 28px;
}

.el-avatar:not(.profile-avatar):not(.nav-avatar):not(.card-avatar) .el-icon svg {
  width: 100%;
  height: 100%;
}

</style>
