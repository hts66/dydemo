<template>
  <div class="profile-container">
    <header class="profile-header">
      <div class="header-left">
        <h1 class="logo">🎵 短视频</h1>
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
        <h2 class="card-title">个人资料</h2>
        
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerUpload">
            <img 
              :src="userStore.user?.avatar || defaultAvatar" 
              alt="头像" 
              class="avatar-img"
            />
            <div class="avatar-overlay">
              <span class="upload-icon">📷</span>
              <span class="upload-text">点击更换头像</span>
            </div>
          </div>
          <input 
            type="file" 
            ref="fileInput" 
            accept="image/*" 
            @change="handleFileChange" 
            class="hidden-input"
          />
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
    </main>

    <main class="profile-main" v-else>
      <div class="not-login">
        <h2>请先登录</h2>
        <router-link to="/login" class="login-link">去登录</router-link>
      </div>
    </main>

    <!-- 关注/粉丝列表弹窗 -->
    <div v-if="showFollowModal" class="follow-modal" @click.self="closeFollowModal">
      <div class="follow-modal-content">
        <div class="follow-modal-header">
          <h3>{{ followModalTitle }}</h3>
          <button class="close-btn" @click="closeFollowModal">✕</button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { uploadAvatar } from '../api/upload'
import { getFollowList, toggleFollow, checkIsFollowing } from '../api/follow'

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
    loadFollowCounts()
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
      // 为每个用户添加关注状态
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
      // 更新计数
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

const handleFileChange = async (event) => {
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
  saving.value = true

  try {
    console.log('开始上传头像...', file.name, file.size)
    const result = await uploadAvatar(file)
    console.log('上传结果:', result)
    if (result.code === 200) {
      const newAvatar = result.data
      userStore.setUser({
        ...userStore.user,
        avatar: newAvatar,
      })
      successMessage.value = '头像上传成功'
      setTimeout(() => {
        successMessage.value = ''
      }, 3000)
    } else {
      errorMessage.value = result.message || '上传失败'
    }
  } catch (err) {
    console.error('上传失败:', err)
    errorMessage.value = '上传失败: ' + (err.message || '请检查后端是否启动')
  } finally {
    saving.value = false
  }

  event.target.value = ''
}

const handleSave = () => {
  successMessage.value = ''
  errorMessage.value = ''
  
  if (!form.username.trim()) {
    errorMessage.value = '用户名不能为空'
    return
  }

  userStore.setUser({
    ...userStore.user,
    username: form.username,
    gender: form.gender,
    bio: form.bio,
  })

  successMessage.value = '保存成功'
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
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
  padding: 40px;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
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

/* 关注/粉丝弹窗 */
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
</style>
