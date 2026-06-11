<template>
  <el-dialog
      class="auth-dialog"
      :model-value="modelValue"
      width="430px"
      align-center
      @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="auth-header">
        <div class="auth-logo-mark" aria-label="CampusHub">
          <img class="auth-logo-img" :src="brandLogo" alt="CampusHub" />
        </div>
      </div>
    </template>

    <div class="mode-switch" role="tablist" aria-label="登录或注册">
      <button
          class="mode-switch-button"
          :class="{ active: mode === 'login' }"
          type="button"
          role="tab"
          :aria-selected="mode === 'login'"
          @click="setMode('login')"
      >
        登录
      </button>
      <button
          class="mode-switch-button"
          :class="{ active: mode === 'register' }"
          type="button"
          role="tab"
          :aria-selected="mode === 'register'"
          @click="setMode('register')"
      >
        注册
      </button>
    </div>

    <el-form
        v-if="mode === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="0"
        @submit.prevent="handleLogin"
    >
      <el-form-item prop="username">
        <el-input
            v-model="loginForm.username"
            prefix-icon="User"
            placeholder="请输入用户名"
            size="large"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
            v-model="loginForm.password"
            prefix-icon="Lock"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
        />
      </el-form-item>
      <el-button
          type="primary"
          size="large"
          class="submit-button"
          :loading="loginLoading"
          @click="handleLogin"
      >
        登录
      </el-button>
    </el-form>

    <el-form
        v-else
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="0"
        @submit.prevent="handleRegister"
    >
      <el-form-item prop="username">
        <el-input
            v-model="registerForm.username"
            prefix-icon="User"
            placeholder="请输入用户名（3-30位，字母数字下划线）"
            size="large"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
            v-model="registerForm.password"
            prefix-icon="Lock"
            type="password"
            placeholder="请输入密码（8-32位，需包含字母和数字）"
            size="large"
            show-password
        />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
            v-model="registerForm.confirmPassword"
            prefix-icon="Lock"
            type="password"
            placeholder="请确认密码"
            size="large"
            show-password
            @keyup.enter="handleRegister"
        />
      </el-form-item>
      <el-button
          type="primary"
          size="large"
          class="submit-button"
          :loading="registerLoading"
          @click="handleRegister"
      >
        注册
      </el-button>
    </el-form>
  </el-dialog>
</template>

<script setup>
import { nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/store/user'
import brandLogo from '@/assets/brand-logo.png'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  initialMode: {
    type: String,
    default: 'login'
  },
  redirectTo: {
    type: String,
    default: '/hall'
  }
})
const emit = defineEmits(['update:modelValue', 'success'])

const router = useRouter()
const userStore = useUserStore()
const mode = ref(props.initialMode)
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

watch(
  () => props.initialMode,
  (value) => {
    setMode(value)
  }
)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    mode.value = props.initialMode
    resetAuthForms()
  }
)

const usernameRules = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 3, max: 30, message: '用户名长度为3-30个字符', trigger: 'blur' },
  { pattern: /^[A-Za-z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
]

const passwordRules = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 8, max: 32, message: '密码长度为8-32位', trigger: 'blur' },
  {
    pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d_@#$%^&*.-]+$/,
    message: '密码必须包含字母和数字，允许字母、数字、下划线及常见符号',
    trigger: 'blur'
  }
]

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const loginRules = {
  username: usernameRules,
  password: passwordRules
}

const registerRules = {
  username: usernameRules,
  password: passwordRules,
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

function resetLoginForm() {
  loginForm.username = ''
  loginForm.password = ''
  nextTick(() => {
    loginFormRef.value?.clearValidate()
  })
}

function resetRegisterForm() {
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  nextTick(() => {
    registerFormRef.value?.clearValidate()
  })
}

function resetAuthForms() {
  resetLoginForm()
  resetRegisterForm()
}

function setMode(value) {
  if (!['login', 'register'].includes(value)) return
  mode.value = value
  if (value === 'login') {
    resetLoginForm()
  } else {
    resetRegisterForm()
  }
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loginLoading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.token)
    const user = await userStore.fetchUserInfo()
    if (!user) return
    ElMessage.success('登录成功')
    resetAuthForms()
    emit('update:modelValue', false)
    emit('success')
    router.push(props.redirectTo || '/hall')
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  registerLoading.value = true
  try {
    await register({ username: registerForm.username, password: registerForm.password })
    ElMessage.success('注册成功，请登录')
    resetRegisterForm()
    setMode('login')
  } catch {
    // error handled by interceptor
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
:deep(.auth-dialog) {
  --el-color-primary: #ff6f9f;
  --el-color-primary-light-3: #ff8fb4;
  --el-color-primary-light-5: #ffadc8;
  --el-color-primary-light-7: #ffd0dc;
  --el-color-primary-light-9: #fff0f6;
  --el-color-primary-dark-2: #ff5f95;
  border-radius: 8px;
}

:deep(.auth-dialog .el-dialog__header) {
  padding: 26px 28px 18px;
  margin: 0;
}

:deep(.auth-dialog .el-dialog__body) {
  padding: 0 28px 28px;
}

.auth-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.auth-logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 172px;
  height: 50px;
  overflow: hidden;
}

.auth-logo-img {
  display: block;
  width: 202px;
  height: auto;
  flex: 0 0 auto;
  transform: translateY(8px);
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  margin-bottom: 22px;
  overflow: hidden;
  border: 1px solid #ff6f9f;
  border-radius: 7px;
  box-sizing: border-box;
}

.mode-switch-button {
  height: 32px;
  border: 0;
  border-right: 1px solid #ff6f9f;
  color: #ff5f95;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-sizing: border-box;
}

.mode-switch-button:last-child {
  border-right: 0;
}

.mode-switch-button.active {
  color: #fff;
  background: #ff6f9f;
  border-color: #ff6f9f;
}

.submit-button {
  border: 0;
  background: #ff6f9f;
  box-shadow: 0 8px 18px rgba(255, 111, 159, 0.18);
}

.submit-button:hover,
.submit-button:focus {
  background: #ff5f95;
  box-shadow: 0 8px 18px rgba(255, 111, 159, 0.2);
}

.submit-button {
  width: 100%;
  margin-top: 2px;
}

@media (max-width: 520px) {
  :deep(.auth-dialog) {
    width: calc(100vw - 32px) !important;
  }

  :deep(.auth-dialog .el-dialog__header) {
    padding: 22px 20px 16px;
  }

  :deep(.auth-dialog .el-dialog__body) {
    padding: 0 20px 24px;
  }
}
</style>
