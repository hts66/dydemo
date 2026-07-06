<template>
  <div class="upload-container">
    <header class="upload-header">
      <div class="header-left">
        <router-link to="/" class="logo">🎵 短视频</router-link>
      </div>
      <div class="header-right">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/profile" class="nav-link">个人资料</router-link>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <main class="upload-main">
      <div class="upload-card">
        <h2 class="card-title">发布视频</h2>

        <div class="upload-section">
          <div v-if="!videoUrl" class="upload-area" @click="triggerVideoUpload">
            <span class="upload-icon">📹</span>
            <p class="upload-text">点击上传视频</p>
            <p class="upload-hint">支持 MP4、MOV、AVI 格式，大小不超过 100MB</p>
          </div>
          <div v-else class="video-preview">
            <video :src="videoUrl" controls class="preview-video"></video>
            <button class="reupload-btn" @click="triggerVideoUpload">重新上传</button>
          </div>
          <input
            type="file"
            ref="videoInput"
            accept="video/*"
            @change="handleVideoChange"
            class="hidden-input"
          />
        </div>

        <div class="form-section">
          <div class="form-group">
            <label>视频标题</label>
            <input
              type="text"
              v-model="form.title"
              class="form-input"
              placeholder="给你的视频起个标题"
            />
          </div>

          <div class="form-group">
            <label>视频描述</label>
            <textarea
              v-model="form.description"
              class="form-textarea"
              placeholder="介绍一下你的视频..."
              rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label>封面图（可选）</label>
            <div v-if="!thumbnailUrl" class="thumbnail-upload" @click="triggerThumbnailUpload">
              <span class="upload-icon">🖼️</span>
              <p class="upload-text">点击上传封面</p>
            </div>
            <div v-else class="thumbnail-preview">
              <img :src="thumbnailUrl" class="preview-img" />
              <button class="reupload-btn" @click="triggerThumbnailUpload">重新上传</button>
            </div>
            <input
              type="file"
              ref="thumbnailInput"
              accept="image/*"
              @change="handleThumbnailChange"
              class="hidden-input"
            />
          </div>

          <button class="publish-btn" @click="handlePublish" :disabled="publishing || !videoUrl">
            {{ publishing ? '发布中...' : '发布视频' }}
          </button>

          <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useUserStore } from '../stores/user'
import { publishWork } from '../api/work'
import { uploadVideo, uploadImage, cleanupFiles } from '../api/upload'

const router = useRouter()
const userStore = useUserStore()

const videoInput = ref(null)
const thumbnailInput = ref(null)
const videoUrl = ref('')
const thumbnailUrl = ref('')
const uploadingVideo = ref(false)
const uploadingThumbnail = ref(false)
const publishing = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const hasManualThumbnail = ref(false)
const isPublished = ref(false)
const oldVideoUrl = ref('')
const oldThumbnailUrl = ref('')

const form = ref({
  title: '',
  description: ''
})

onMounted(() => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
  }
})

onBeforeRouteLeave(async (to, from) => {
  if (isPublished.value) {
    return
  }

  if (videoUrl.value || thumbnailUrl.value) {
    const filesToCleanup = []
    if (videoUrl.value) filesToCleanup.push(videoUrl.value)
    if (thumbnailUrl.value) filesToCleanup.push(thumbnailUrl.value)

    if (filesToCleanup.length > 0) {
      try {
        await cleanupFiles(filesToCleanup)
        console.log('已清理未发布的文件:', filesToCleanup)
      } catch (err) {
        console.error('清理文件失败:', err)
      }
    }
  }
})

const triggerVideoUpload = () => {
  if (!hasManualThumbnail.value) {
    thumbnailUrl.value = ''
  }
  oldVideoUrl.value = videoUrl.value
  oldThumbnailUrl.value = thumbnailUrl.value
  videoInput.value?.click()
}

const triggerThumbnailUpload = () => {
  thumbnailInput.value?.click()
}

const handleVideoChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > 100 * 1024 * 1024) {
    errorMessage.value = '视频大小不能超过100MB'
    return
  }

  uploadingVideo.value = true
  errorMessage.value = ''

  try {
    const result = await uploadVideo(file)
    if (result.code === 200) {
      const newVideoUrl = result.data
      const newThumbnailUrl = thumbnailUrl.value

      if (oldVideoUrl.value || oldThumbnailUrl.value) {
        const filesToCleanup = []
        if (oldVideoUrl.value) filesToCleanup.push(oldVideoUrl.value)
        if (oldThumbnailUrl.value) filesToCleanup.push(oldThumbnailUrl.value)
        try {
          await cleanupFiles(filesToCleanup)
          console.log('已清理重新上传前的旧文件:', filesToCleanup)
        } catch (err) {
          console.error('清理旧文件失败:', err)
        }
      }

      videoUrl.value = newVideoUrl
      if (!hasManualThumbnail.value) {
        thumbnailUrl.value = ''
        await generateThumbnailFromVideo(file)
      }
      oldVideoUrl.value = ''
      oldThumbnailUrl.value = ''
    } else {
      errorMessage.value = result.message || '视频上传失败'
    }
  } catch (err) {
    errorMessage.value = '视频上传失败: ' + err.message
  } finally {
    uploadingVideo.value = false
  }
}

const generateThumbnailFromVideo = (videoFile) => {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')

    video.crossOrigin = 'anonymous'
    video.preload = 'metadata'
    video.src = URL.createObjectURL(videoFile)

    video.onloadedmetadata = () => {
      video.currentTime = 0.1
    }

    video.onseeked = () => {
      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

      canvas.toBlob(async (blob) => {
        if (blob) {
          const thumbnailFile = new File([blob], 'thumbnail.jpg', { type: 'image/jpeg' })
          const formData = new FormData()
          formData.append('file', thumbnailFile)

          try {
            const result = await uploadImage(thumbnailFile)
            if (result.code === 200) {
              thumbnailUrl.value = result.data
            }
          } catch (err) {
            console.warn('自动生成封面失败:', err)
          }
        }
        URL.revokeObjectURL(video.src)
        resolve()
      }, 'image/jpeg', 0.9)
    }

    video.onerror = () => {
      URL.revokeObjectURL(video.src)
      resolve()
    }
  })
}

const handleThumbnailChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '图片大小不能超过5MB'
    return
  }

  uploadingThumbnail.value = true
  errorMessage.value = ''

  try {
    const result = await uploadImage(file)
    if (result.code === 200) {
      thumbnailUrl.value = result.data
      hasManualThumbnail.value = true
    } else {
      errorMessage.value = result.message || '封面上传失败'
    }
  } catch (err) {
    errorMessage.value = '封面上传失败: ' + err.message
  } finally {
    uploadingThumbnail.value = false
  }
}

const handlePublish = async () => {
  if (!videoUrl.value) {
    errorMessage.value = '请先上传视频'
    return
  }

  publishing.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const result = await publishWork({
      url: videoUrl.value,
      thumbnail: thumbnailUrl.value,
      title: form.value.title,
      description: form.value.description
    })
    if (result.code === 200) {
      successMessage.value = '发布成功！'
      isPublished.value = true
      setTimeout(() => {
        router.push('/')
      }, 1500)
    } else {
      errorMessage.value = result.message || '发布失败'
    }
  } catch (err) {
    errorMessage.value = '发布失败: ' + err.message
  } finally {
    publishing.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.upload-container {
  min-height: 100vh;
  background: #f5f5f5;
  overflow-y: auto;
}

.upload-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  text-decoration: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-link {
  padding: 8px 16px;
  color: #333;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.nav-link:hover {
  background: #f0f0f0;
}

.logout-btn {
  padding: 8px 16px;
  background: #ff4757;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.logout-btn:hover {
  background: #ff3344;
}

.upload-main {
  max-width: 600px;
  margin: 40px auto;
  padding: 0 20px;
}

.upload-card {
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}

.upload-section {
  margin-bottom: 24px;
}

.upload-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.upload-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.upload-hint {
  font-size: 12px;
  color: #999;
}

.video-preview {
  text-align: center;
}

.preview-video {
  width: 100%;
  max-height: 300px;
  border-radius: 8px;
  object-fit: cover;
}

.thumbnail-upload {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.thumbnail-upload:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.thumbnail-preview {
  text-align: center;
}

.preview-img {
  width: 100%;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
}

.reupload-btn {
  margin-top: 8px;
  padding: 6px 16px;
  background: #f0f0f0;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.form-input:focus {
  border-color: #667eea;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  resize: vertical;
  transition: border-color 0.3s;
}

.form-textarea:focus {
  border-color: #667eea;
}

.hidden-input {
  display: none;
}

.publish-btn {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.publish-btn:hover:not(:disabled) {
  background: #5a67d8;
}

.publish-btn:disabled {
  background: #a0aec0;
  cursor: not-allowed;
}

.success-message {
  margin-top: 12px;
  color: #48bb78;
  font-size: 14px;
  text-align: center;
}

.error-message {
  margin-top: 12px;
  color: #f56565;
  font-size: 14px;
  text-align: center;
}
</style>
