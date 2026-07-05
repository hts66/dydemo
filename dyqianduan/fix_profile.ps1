$content = @"
<template>
  <div class="profile-container">
    <header class="profile-header">
      <div class="header-left">
        <h1 class="logo">短视频</h1>
      </div>
      <div class="header-right" v-if="userStore.isLoggedIn">
        <router-link to="/" class="home-link">首页</router-link>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
      <div class="header-right" v-else>
        <router-link to="/login" class="login-btn">登录</router-link>
        <router-link to="/register" class="register-btn">注册</router-link>
      </div>
    </header>

    <main class="profile-main" v-if="userStore.isLoggedIn">
      <div class="profile-card">
        <div 
          class="background-section" 
          :style="{ backgroundColor: currentBackground }"
          @click="toggleBackgroundPicker"
        >
          <div class="background-hint">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
              <path d="M20.71 4.04a1 1 0 0 0-1.42 0l-3.02 3.02-7.19-7.19a1 1 0 0 0-1.42 0L3.29 5.36a1 1 0 0 0 0 1.42l7.19 7.19-3.02 3.02a1 1 0 0 0 0 1.42l2.12 2.12a1 1 0 0 0 1.42 0l3.02-3.02 7.19 7.19a1 1 0 0 0 1.42 0l2.12-2.12a1 1 0 0 0 0-1.42l-7.19-7.19 3.02-3.02a1 1 0 0 0 0-1.42z"/>
            </svg>
            <span>点击更换背景</span>
          </div>
        </div>
        
        <div class="card-content">
          <div class="avatar-section">
            <div class="avatar-wrapper" @click="triggerUpload">
              <img 
                :src="pendingAvatarUrl || userStore.user?.avatar || defaultAvatar" 
                alt="头像" 
                class="avatar-img"
              />
              <div class="avatar-overlay">
                <span class="upload-icon">📷</span>
                <span class="upload-text">{{ pendingAvatarUrl ? '更换头像' : '点击更换头像' }}</span>
              </div>
            </div>
            <input 
              type="file" 
              ref="fileInput" 
              accept="image/*" 
              @change="handleFileChange" 
              class="hidden-input"
            />
            <div v-if="pendingAvatarUrl" class="avatar-actions">
              <button class="cancel-avatar-btn" @click="cancelAvatarChange">取消</button>
            </div>
          </div>

          <div class="stats-section">
            <div class="stat-item" @click="showFollowList('following')">
              <span class="stat-number">{{ followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item" @click="showFollowList('followers')">
              <span class="stat-number">{{ followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
          </div>

          <div class="form-section">
            <div class="form-group">
              <label>邮箱</label>
              <input 
                type="email" 
                v-model="form.email" 
                class="form-input" 
                disabled
              />
            </div>

            <div class="form-group">
              <label>用户名</label>
              <input 
                type="text" 
                v-model="form.username" 
                class="form-input"
                placeholder="请输入用户名"
              />
            </div>

            <div class="form-group">
              <label>性别</label>
              <div class="gender-options">
                <label class="gender-option">
                  <input type="radio" v-model="form.gender" :value="0" />
                  <span>未知</span>
                </label>
                <label class="gender-option">
                  <input type="radio" v-model="form.gender" :value="1" />
                  <span>男</span>
                </label>
                <label class="gender-option">
                  <input type="radio" v-model="form.gender" :value="2" />
                  <span>女</span>
                </label>
              </div>
            </div>

            <div class="form-group">
              <label>个人简介</label>
              <textarea 
                v-model="form.bio" 
                class="form-textarea"
                placeholder="介绍一下自己..."
                rows="4"
              ></textarea>
            </div>

            <button class="save-btn" @click="handleSave" :disabled="saving">
              {{ saving ? '保存中...' : '保存修改' }}
            </button>

            <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
            <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
          </div>
        </div>
      </div>
    </main>

    <main class="profile-main" v-else>
      <div class="not-login">
        <h2>请先登录</h2>
        <router-link to="/login" class="login-link">去登录</router-link>
      </div>
    </main>

    <div v-if="showFollowModal" class="follow-modal" @click.self="closeFollowModal">
      <div class="follow-modal-content">
        <div class="follow-modal-header">
          <h3>{{ followModalTitle }}</h3>
          <button class="close-btn" @click="closeFollowModal">X</button>
        </div>
        <div class="follow-list">
          <div v-for="user in followList" :key="user.id" class="follow-item">
            <img :src="user.avatar || defaultAvatar" class="follow-avatar" />
            <div class="follow-info">
              <span class="follow-name">{{ user.username || '匿名用户' }}</span>
              <span class="follow-bio">{{ user.bio || '这个人很懒，什么都没写~' }}</span>
            </div>
            <button 
              v-if="user.id !== userStore.user?.id"
              class="follow-action-btn"
              :class="{ following: user.isFollowing }"
              @click="handleFollow(user.id, user)"
            >
              {{ user.isFollowing ? '已关注' : '关注' }}
            </button>
          </div>
          <div v-if="followList.length === 0" class="empty-follow">
            <p>暂无{{ followModalTitle === '关注' ? '关注' : '粉丝' }}</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showBackgroundPicker" class="background-modal" @click.self="toggleBackgroundPicker">
      <div class="background-modal-content">
        <div class="background-modal-header">
          <h3>选择背景颜色</h3>
          <button class="close-btn" @click="toggleBackgroundPicker">X</button>
        </div>
        <div class="background-colors">
          <div 
            v-for="color in backgroundColors" 
            :key="color"
            class="color-item"
            :class="{ active: currentBackground === color }"
            :style="{ backgroundColor: color }"
            @click="selectBackground(color)"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { useUserStore } from '../stores/user'
import { uploadAvatar, cleanupFiles } from '../api/upload'
import { getFollowList, toggleFollow, checkIsFollowing } from '../api/follow'
import { updateBackground } from '../api/user'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const fileInput = ref(null)
const saving = ref(false)
const successMessage = ref('')
const errorMessage = ref('')
const followingCount = ref(0)
const followerCount = ref(0)
const showFollowModal = ref(false)
const followModalTitle = ref('')
const followList = ref([])

const pendingAvatarFile = ref(null)
const pendingAvatarUrl = ref('')

const currentBackground = ref('#000000')
const showBackgroundPicker = ref(false)

const backgroundColors = [
  '#000000',
  '#1a1a2e',
  '#16213e',
  '#0f3460',
  '#533483',
  '#e94560',
  '#ff6b6b',
  '#ff8e53',
  '#feca57',
  '#48dbfb',
  '#1dd1a1',
  '#5f27cd',
]

const form = reactive({
  email: '',
  username: '',
  gender: 0,
  bio: '',
})

onMounted(() => {
  if (userStore.user) {
    form.email = userStore.user.email || ''
    form.username = userStore.user.username || ''
    form.gender = userStore.user.gender || 0
    form.bio = userStore.user.bio || ''
    currentBackground.value = userStore.user.background || '#000000'
    loadFollowCounts()
  }
})

onUnmounted(() => {
  if (pendingAvatarUrl.value) {
    URL.revokeObjectURL(pendingAvatarUrl.value)
  }
})

onBeforeRouteLeave(() => {
  if (pendingAvatarUrl.value) {
    URL.revokeObjectURL(pendingAvatarUrl.value)
    pendingAvatarUrl.value = ''
    pendingAvatarFile.value = null
  }
})

const loadFollowCounts = async () => {
  try {
    const result = await getFollowList(userStore.user.id)
    if (result.code === 200) {
      followingCount.value = result.data.following.length
      followerCount.value = result.data.followers.length
    }
  } catch (err) {
    console.error('加载关注数据失败', err)
  }
}

const showFollowList = async (type) => {
  followModalTitle.value = type === 'following' ? '关注' : '粉丝'
  showFollowModal.value = true
  
  try {
    const result = await getFollowList(userStore.user.id)
    if (result.code === 200) {
      const list = type === 'following' ? result.data.following : result.data.followers
      for (const user of list) {
        if (user.id !== userStore.user.id) {
          try {
            const followResult = await checkIsFollowing(user.id)
            if (followResult.code === 200) {
              user.isFollowing = followResult.data
            } else {
              user.isFollowing = false
            }
          } catch (e) {
            user.isFollowing = false
          }
        } else {
          user.isFollowing = false
        }
      }
      followList.value = list
    }
  } catch (err) {
    console.error('加载列表失败', err)
  }
}

const handleFollow = async (userId, user) => {
  try {
    const result = await toggleFollow(userId)
    if (result.code === 200) {
      user.isFollowing = result.data
      if (result.data) {
        followingCount.value++
      } else {
        followingCount.value--
      }
    }
  } catch (err) {
    console.error('关注操作失败', err)
  }
}

const closeFollowModal = () => {
  showFollowModal.value = false
  followList.value = []
}

const triggerUpload = () => {
  fileInput.value.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    errorMessage.value = '请选择图片文件'
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '图片大小不能超过5MB'
    return
  }

  errorMessage.value = ''
  successMessage.value = ''

  if (pendingAvatarUrl.value) {
    URL.revokeObjectURL(pendingAvatarUrl.value)
  }

  pendingAvatarFile.value = file
  pendingAvatarUrl.value = URL.createObjectURL(file)

  successMessage.value = '头像已选择，请点击保存修改确认'
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)

  event.target.value = ''
}

const cancelAvatarChange = () => {
  if (pendingAvatarUrl.value) {
    URL.revokeObjectURL(pendingAvatarUrl.value)
    pendingAvatarUrl.value = ''
    pendingAvatarFile.value = null
  }
}

const handleSave = async () => {
  successMessage.value = ''
  errorMessage.value = ''
  
  if (!form.username.trim()) {
    errorMessage.value = '用户名不能为空'
    return
  }

  saving.value = true

  try {
    const updateData = {
      ...userStore.user,
      username: form.username,
      gender: form.gender,
      bio: form.bio,
    }

    if (pendingAvatarFile.value) {
      console.log('开始上传头像到阿里云...', pendingAvatarFile.value.name)
      const result = await uploadAvatar(pendingAvatarFile.value)
      console.log('上传结果:', result)
      
      if (result.code === 200) {
        const oldAvatar = userStore.user?.avatar
        const newAvatar = result.data
        updateData.avatar = newAvatar

        if (oldAvatar && oldAvatar !== newAvatar) {
          try {
            await cleanupFiles([oldAvatar])
            console.log('已删除旧头像:', oldAvatar)
          } catch (err) {
            console.error('删除旧头像失败:', err)
          }
        }

        URL.revokeObjectURL(pendingAvatarUrl.value)
        pendingAvatarUrl.value = ''
        pendingAvatarFile.value = null
      } else {
        errorMessage.value = result.message || '头像上传失败'
        return
      }
    }

    userStore.setUser(updateData)

    successMessage.value = '保存成功'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (err) {
    errorMessage.value = '保存失败: ' + (err.message || '未知错误')
  } finally {
    saving.value = false
  }
}

const toggleBackgroundPicker = () => {
  showBackgroundPicker.value = !showBackgroundPicker.value
}

const selectBackground = async (color) => {
  currentBackground.value = color
  showBackgroundPicker.value = false
  
  try {
    const result = await updateBackground(color)
    if (result.code === 200) {
      userStore.user.background = color
      successMessage.value = '背景设置成功'
      setTimeout(() => {
        successMessage.value = ''
      }, 3000)
    }
  } catch (err) {
    console.error('更新背景失败', err)
    errorMessage.value = '背景设置失败'
    setTimeout(() => {
      errorMessage.value = ''
    }, 3000)
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.profile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  flex: 1;
}

.logo {
  font-size: 24px;
  font-weight: bold;
  color: #667eea;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.home-link {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #333;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.home-link:hover {
  background: #e0e0e0;
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
  background: #ff6b7a;
}

.login-btn {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #333;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.register-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.profile-main {
  padding: 40px;
  display: flex;
  justify-content: center;
}

.profile-card {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.background-section {
  height: 160px;
  width: 100%;
  cursor: pointer;
  position: relative;
  transition: opacity 0.3s;
  z-index: 10;
}

.background-section:hover {
  opacity: 0.9;
}

.background-hint {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 20px;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.background-section:hover .background-hint {
  opacity: 1;
}

.card-content {
  padding: 40px;
  padding-top: 0;
  pointer-events: none;
}

.card-content > * {
  pointer-events: auto;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  margin-top: -60px;
  border: 4px solid white;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.upload-icon {
  font-size: 24px;
}

.upload-text {
  font-size: 12px;
  color: white;
  margin-top: 5px;
}

.hidden-input {
  display: none;
}

.avatar-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
  gap: 12px;
}

.cancel-avatar-btn {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #666;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-avatar-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.stats-section {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-item:hover {
  transform: scale(1.05);
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:disabled {
  background: #f5f5f5;
  color: #999;
}

.form-textarea {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
  transition: all 0.3s;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.gender-options {
  display: flex;
  gap: 20px;
}

.gender-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.gender-option input {
  cursor: pointer;
}

.save-btn {
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 10px;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.save-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.success-message {
  text-align: center;
  color: #2ecc71;
  font-size: 14px;
  margin-top: 10px;
}

.error-message {
  text-align: center;
  color: #ff4757;
  font-size: 14px;
  margin-top: 10px;
}

.not-login {
  text-align: center;
  padding: 60px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.not-login h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 20px;
}

.login-link {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
}

.login-link:hover {
  opacity: 0.9;
}

.follow-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.follow-modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.follow-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.follow-modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #f0f0f0;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #e0e0e0;
}

.follow-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.follow-item:last-child {
  border-bottom: none;
}

.follow-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.follow-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.follow-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.follow-bio {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-action-btn {
  padding: 6px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.follow-action-btn:hover {
  background: #5a67d8;
}

.follow-action-btn.following {
  background: #f0f0f0;
  color: #666;
}

.follow-action-btn.following:hover {
  background: #ff4757;
  color: white;
}

.empty-follow {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.background-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.background-modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.background-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.background-modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.background-colors {
  display: flex;
  flex-wrap: wrap;
  padding: 20px;
  gap: 16px;
}

.color-item {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 2px solid transparent;
}

.color-item:hover {
  transform: scale(1.1);
}

.color-item.active {
  border-color: #333;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.1);
}
</style>
"@
$content | Set-Content -Path "c:\Users\27036\Desktop\dydemo\dyqianduan\src\views\Profile.vue" -Encoding UTF8
