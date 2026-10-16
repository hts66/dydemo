<template>
  <div v-if="visible" class="captcha-modal-overlay" @click="handleClose">
    <div class="captcha-modal" @click.stop>
      <div class="captcha-modal-header">
        <h3>安全验证</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      <div class="captcha-modal-body">
        <p class="captcha-tip">请先完成图形验证，我们将发送验证码到您的邮箱</p>
        <div class="captcha-input-group">
          <input
            v-model="captchaCode"
            type="text"
            placeholder="请输入图形验证码"
            class="captcha-input"
            maxlength="4"
            @keyup.enter="handleConfirm"
            @input="errorMessage = ''"
          />
          <img
            :src="captchaImage"
            alt="图形验证码"
            class="captcha-img"
            @click="refreshCaptcha"
          />
        </div>
        <span v-if="errorMessage" class="error-message">{{ errorMessage }}</span>
      </div>
      <div class="captcha-modal-footer">
        <button class="cancel-btn" @click="handleClose">取消</button>
        <button class="confirm-btn" :disabled="!captchaCode || loading" @click="handleConfirm">
          {{ loading ? '验证中...' : '确定' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import request from '../utils/request'

const props = defineProps<{
  visible: boolean
  externalError?: string
  refreshKey?: number
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success', captchaKey: string, captcha: string): void
}>()

const captchaCode = ref('')
const captchaKey = ref('')
const captchaImage = ref('')
const errorMessage = ref('')
const loading = ref(false)

const refreshCaptcha = async (keepError = false) => {
  try {
    const response = await request.get('/captcha')
    if (response && response.data) {
      captchaKey.value = response.data.key
      captchaImage.value = response.data.image
      captchaCode.value = ''
      if (!keepError) {
        errorMessage.value = ''
      }
    }
  } catch (err: any) {
    console.error('获取验证码失败', err?.message || err)
    errorMessage.value = '获取验证码失败，请重试'
  }
}

// 弹窗打开时刷新验证码
watch(() => props.visible, (newVal) => {
  if (newVal) {
    refreshCaptcha()
  }
})

// 外部传入的错误消息
watch(() => props.externalError, (newVal) => {
  if (newVal) {
    errorMessage.value = newVal
  }
})

// 外部触发刷新验证码（保留错误消息）
watch(() => props.refreshKey, () => {
  refreshCaptcha(true)
})

const handleConfirm = () => {
  if (!captchaCode.value) {
    errorMessage.value = '请输入图形验证码'
    return
  }

  loading.value = true
  errorMessage.value = ''

  // 直接 emit 给父组件，让父组件调用发送验证码接口
  emit('success', captchaKey.value, captchaCode.value)
  loading.value = false
}

const handleClose = () => {
  captchaCode.value = ''
  errorMessage.value = ''
  emit('close')
}
</script>

<style scoped>
.captcha-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.captcha-modal {
  background: #1a1a1a;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.captcha-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #333;
}

.captcha-modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #fff;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #fff;
}

.captcha-modal-body {
  padding: 24px;
}

.captcha-tip {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #999;
  line-height: 1.5;
}

.captcha-input-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
  padding: 12px 16px;
  background: #2a2a2a;
  border: 1px solid #444;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.captcha-input:focus {
  border-color: #fe2c55;
}

.captcha-img {
  width: 120px;
  height: 44px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #444;
}

.error-message {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #fe2c55;
}

.captcha-modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #333;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #2a2a2a;
  color: #999;
}

.cancel-btn:hover {
  background: #333;
  color: #fff;
}

.confirm-btn {
  background: #fe2c55;
  color: #fff;
}

.confirm-btn:hover:not(:disabled) {
  background: #ff4d6f;
}

.confirm-btn:disabled {
  background: #666;
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
