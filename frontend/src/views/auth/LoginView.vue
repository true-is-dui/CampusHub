<template>
  <div class="login-container">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <h2 class="card-title">用户登录</h2>
      </template>
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              prefix-icon="User"
              placeholder="请输入用户名"
              size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              type="primary"
              size="large"
              style="width: 100%"
              :loading="loading"
              @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度为 3-30 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度为 8-32 位', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d_@#$%^&*.-]+$/,
      message: '密码必须包含字母和数字，允许字母、数字、下划线及常见符号',
      trigger: 'blur'
    }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.token)
    // 获取用户信息，失败则中止后续跳转（store 内部已清除 token）
    const user = await userStore.fetchUserInfo()
    if (!user) return
    ElMessage.success('登录成功')
    router.push('/hall')
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  border-radius: 12px;
}

.card-title {
  text-align: center;
  margin: 0;
  color: #303133;
}

.login-footer {
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.login-footer a {
  color: #409eff;
  text-decoration: none;
}

.login-footer a:hover {
  text-decoration: underline;
}
</style>