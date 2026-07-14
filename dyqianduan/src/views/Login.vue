<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo-area">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="#fe2c55">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
          </svg>
        </div>
        <h2 class="login-title">短视频平台</h2>
        <p class="login-subtitle">登录你的账号</p>
      </div>
      
      <form @submit.prevent="handleSubmit" class="login-form">
        <div class="form-group">
          <input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            class="form-input"
            :class="{ error: errors.email }"
          />
          <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
        </div>

        <div class="form-group">
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            class="form-input"
            :class="{ error: errors.password }"
          />
          <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
        </div>

        <div class="form-group">
          <div class="code-input-group">
            <input
              v-model="form.code"
              type="text"
              placeholder="请输入邮箱验证码"
              class="form-input code-input"
              maxlength="6"
            />
            <button
              type="button"
              class="send-code-btn"
              :disabled="codeBtnDisabled"
              @click="sendCode"
            >
              <span v-if="codeBtnDisabled">{{ codeCountdown }}s</span>
              <span v-else>发送验证码</span>
            </button>
          </div>
          <span v-if="errors.code" class="error-message">{{ errors.code }}</span>
        </div>

        <div class="form-group">
          <div class="captcha-group">
            <input
              v-model="form.captcha"
              type="text"
              placeholder="请输入图形验证码"
              class="form-input captcha-input"
              maxlength="4"
            />
            <img
              :src="captchaImage"
              alt="图形验证码"
              class="captcha-img"
              @click="refreshCaptcha"
            />
          </div>
          <span v-if="errors.captcha" class="error-message">{{ errors.captcha }}</span>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading">登录中...</span>
          <span v-else>登录</span>
        </button>

        <span v-if="errorMessage" class="form-error">{{ errorMessage }}</span>
      </form>

      <div class="login-options">
        <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
      </div>

      <p class="register-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  email: '',
  password: '',
  code: '',
  captcha: '',
  captchaKey: '',
})

const errors = reactive({
  email: '',
  password: '',
  code: '',
  captcha: '',
})

const errorMessage = ref('')
const loading = ref(false)
const codeBtnDisabled = ref(false)
const codeCountdown = ref(60)
const captchaImage = ref('')

const refreshCaptcha = async () => {
  try {
    const response = await request.get('/captcha')
    if (response && response.data) {
      form.captchaKey = response.data.key
      captchaImage.value = response.data.image
    }
  } catch (err: any) {
    console.error('获取验证码失败', err?.message || err)
  }
}

onMounted(() => {
  refreshCaptcha()
})

const validateForm = () => {
  let isValid = true
  errors.email = ''
  errors.password = ''
  errors.code = ''
  errors.captcha = ''
  errorMessage.value = ''

  if (!form.email) {
    errors.email = '请输入邮箱'
    isValid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '邮箱格式不正确'
    isValid = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    isValid = false
  } else if (form.password.length < 6) {
    errors.password = '密码长度至少6位'
    isValid = false
  }

  if (!form.code) {
    errors.code = '请输入邮箱验证码'
    isValid = false
  }

  if (!form.captcha) {
    errors.captcha = '请输入图形验证码'
    isValid = false
  }

  return isValid
}

const sendCode = async () => {
  if (!form.email) {
    errors.email = '请输入邮箱'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '邮箱格式不正确'
    return
  }

  try {
    await request.post('/auth/send-code', {
      email: form.email,
    })

    codeBtnDisabled.value = true
    codeCountdown.value = 60

    const timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        clearInterval(timer)
        codeBtnDisabled.value = false
        codeCountdown.value = 60
      }
    }, 1000)
  } catch (err: any) {
    errorMessage.value = err?.message || '发送验证码失败'
  }
}

const handleSubmit = async () => {
  if (!validateForm()) return

  loading.value = true

  try {
    const response = await request.post('/auth/login/code', {
      email: form.email,
      password: form.password,
      code: form.code,
      captcha: form.captcha,
      captchaKey: form.captchaKey,
    })

    console.log('Login response full:', response)
    
    const loginData = response.data || response
    console.log('Login data:', loginData)
    
    if (loginData.token) {
      console.log('Setting token:', loginData.token)
      localStorage.setItem('token', loginData.token)
      localStorage.setItem('refreshToken', loginData.refreshToken || '')
      localStorage.setItem('user', JSON.stringify(loginData.user || {}))
      
      userStore.token = loginData.token
      userStore.refreshToken = loginData.refreshToken || ''
      userStore.user = loginData.user || {}
    } else {
      console.error('No token in response:', loginData)
      errorMessage.value = '登录失败：未获取到令牌'
      loading.value = false
      return
    }

    console.log('Navigating to /featured')
    setTimeout(() => {
      window.location.href = '/featured'
    }, 100)
  } catch (err: any) {
    errorMessage.value = err?.message || '登录失败'
    await refreshCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.login-card {
  background: white;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.logo-area {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  margin-bottom: 12px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.login-subtitle {
  font-size: 14px;
  color: #999;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-input {
  padding: 14px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.2s;
  background: #fafafa;
}

.form-input:focus {
  outline: none;
  border-color: #fe2c55;
  background: white;
  box-shadow: 0 0 0 3px rgba(254, 44, 85, 0.1);
}

.form-input.error {
  border-color: #ff4757;
}

.code-input-group {
  display: flex;
  gap: 12px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  padding: 14px 20px;
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.send-code-btn:hover:not(:disabled) {
  background: #f0f0f0;
  border-color: #d0d0d0;
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.captcha-group {
  display: flex;
  gap: 12px;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  width: 100px;
  height: 44px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #e0e0e0;
}

.error-message {
  font-size: 12px;
  color: #ff4757;
}

.submit-btn {
  padding: 14px;
  background: #fe2c55;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  background: #e81a46;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-error {
  text-align: center;
  color: #ff4757;
  font-size: 13px;
  margin-top: 8px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.option-btn {
  background: none;
  border: none;
  color: #fe2c55;
  font-size: 14px;
  cursor: pointer;
  padding: 4px 8px;
}

.option-btn:hover {
  text-decoration: underline;
}

.forgot-link {
  color: #999;
  font-size: 14px;
  text-decoration: none;
}

.forgot-link:hover {
  color: #fe2c55;
}

.register-link {
  text-align: center;
  margin-top: 24px;
  color: #999;
  font-size: 14px;
}

.register-link a {
  color: #fe2c55;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>