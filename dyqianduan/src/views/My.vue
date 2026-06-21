<template>
  <div class="my-container">
    <div class="profile-header-section">
      <div class="profile-info">
        <div class="avatar-wrapper" @click="openEditModal" v-if="userStore.isLoggedIn">
          <img :src="userStore.user?.avatar || defaultAvatar" class="avatar-img" />
          <div class="avatar-edit-hint">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
              <path d="M3 4V1h2v3h3v2H5v3H3V6H0V4h3zm3 6V7h3V4h7l1.83 2H21c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2V10h3zm7 9c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-3.2-5c0 1.77 1.43 3.2 3.2 3.2s3.2-1.43 3.2-3.2-1.43-3.2-3.2-3.2-3.2 1.43-3.2 3.2z"/>
            </svg>
          </div>
        </div>
        <div class="avatar-wrapper" v-else>
          <img :src="defaultAvatar" class="avatar-img" />
        </div>
        <div class="user-details">
          <div class="username-row">
            <h2 class="username">{{ userStore.user?.username || '未登录' }}</h2>
            <button class="edit-btn" v-if="userStore.isLoggedIn" @click="openEditModal">编辑资料</button>
          </div>
          <div class="stats-row">
            <div class="stat-item">
              <span class="stat-num">{{ followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ worksCount }}</span>
              <span class="stat-label">获赞</span>
            </div>
          </div>
          <p class="user-bio">{{ userStore.user?.bio || '这个人很懒，什么都没写~' }}</p>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="tabs-bar">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'works' }"
        @click="activeTab = 'works'"
      >
        作品 {{ myWorks.length }}
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'liked' }"
        @click="activeTab = 'liked'"
      >
        喜欢
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'following' }"
        @click="activeTab = 'following'"
      >
        关注
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'followers' }"
        @click="activeTab = 'followers'"
      >
        粉丝
      </div>
    </div>

    <!-- 内容区 -->
    <div class="tab-content">
      <!-- 作品 -->
      <div v-if="activeTab === 'works'" class="works-grid">
        <div 
          v-for="work in myWorks" 
          :key="work.id"
          class="work-card"
          @click="openVideo(work)"
        >
          <div class="work-thumbnail">
            <img :src="work.thumbnail || work.url" />
            <div class="play-overlay">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </div>
          </div>
          <div class="work-info">
            <span class="work-title">{{ work.title || '无标题' }}</span>
            <div class="work-stats">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="#fe2c55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              {{ formatCount(work.likesCount) }}
            </div>
          </div>
        </div>
        <div v-if="myWorks.length === 0" class="empty-state">
          <p>还没有发布作品</p>
          <router-link to="/upload" class="upload-link">去发布第一个视频</router-link>
        </div>
      </div>

      <!-- 喜欢 -->
      <div v-if="activeTab === 'liked'" class="works-grid">
        <div 
          v-for="work in likedWorks" 
          :key="work.id"
          class="work-card"
          @click="openVideo(work)"
        >
          <div class="work-thumbnail">
            <img :src="work.thumbnail || work.url" />
          </div>
          <div class="work-info">
            <span class="work-title">{{ work.title || '无标题' }}</span>
          </div>
        </div>
        <div v-if="likedWorks.length === 0" class="empty-state">
          <p>还没有喜欢的作品</p>
        </div>
      </div>

      <!-- 关注 -->
      <div v-if="activeTab === 'following'" class="user-list">
        <div v-for="user in followingList" :key="user.id" class="user-item">
          <img :src="user.avatar || defaultAvatar" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">{{ user.username || '匿名用户' }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒' }}</span>
          </div>
          <button class="follow-btn following" @click="handleFollow(user.id)">已关注</button>
        </div>
        <div v-if="followingList.length === 0" class="empty-state">
          <p>还没有关注任何人</p>
        </div>
      </div>

      <!-- 粉丝 -->
      <div v-if="activeTab === 'followers'" class="user-list">
        <div v-for="user in followerList" :key="user.id" class="user-item">
          <img :src="user.avatar || defaultAvatar" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">{{ user.username || '匿名用户' }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒' }}</span>
          </div>
        </div>
        <div v-if="followerList.length === 0" class="empty-state">
          <p>还没有粉丝</p>
        </div>
      </div>
    </div>

    <!-- 视频播放弹窗 -->
    <div v-if="currentVideo" class="video-modal" @click.self="closeVideo">
      <div class="modal-content">
        <button class="close-btn" @click="closeVideo">✕</button>
        <video
          ref="videoPlayer"
          :src="currentVideo.url"
          :poster="currentVideo.thumbnail"
          class="modal-video"
          controls
          autoplay
        ></video>
        <div class="video-info-overlay">
          <h3>{{ currentVideo.title }}</h3>
          <p>{{ currentVideo.description }}</p>
        </div>
      </div>
    </div>

    <!-- 编辑资料弹窗 -->
    <div v-if="showEditModal" class="edit-modal" @click.self="closeEditModal">
      <div class="edit-modal-content">
        <div class="edit-modal-header">
          <h3>编辑资料</h3>
          <button class="close-btn" @click="closeEditModal">✕</button>
        </div>
        <div class="edit-modal-body">
          <!-- 头像上传 -->
          <div class="avatar-edit-section">
            <label class="avatar-edit-label">头像</label>
            <div class="avatar-edit-wrapper" @click="triggerAvatarUpload">
              <img :src="editForm.avatar || defaultAvatar" class="avatar-edit-img" />
              <div class="avatar-edit-overlay">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff">
                  <path d="M3 4V1h2v3h3v2H5v3H3V6H0V4h3zm3 6V7h3V4h7l1.83 2H21c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2V10h3zm7 9c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-3.2-5c0 1.77 1.43 3.2 3.2 3.2s3.2-1.43 3.2-3.2-1.43-3.2-3.2-3.2-3.2 1.43-3.2 3.2z"/>
                </svg>
                <span>更换头像</span>
              </div>
            </div>
            <input 
              type="file" 
              ref="avatarFileInput" 
              accept="image/*" 
              @change="handleAvatarChange" 
              class="hidden-input"
            />
          </div>

          <!-- 用户名 -->
          <div class="edit-field">
            <label class="edit-field-label">用户名</label>
            <input 
              type="text" 
              v-model="editForm.username" 
              class="edit-input"
              placeholder="请输入用户名"
            />
          </div>

          <!-- 性别 -->
          <div class="edit-field">
            <label class="edit-field-label">性别</label>
            <div class="gender-options">
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="0" />
                <span>未知</span>
              </label>
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="1" />
                <span>男</span>
              </label>
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="2" />
                <span>女</span>
              </label>
            </div>
          </div>

          <!-- 简介 -->
          <div class="edit-field">
            <label class="edit-field-label">简介</label>
            <textarea 
              v-model="editForm.bio" 
              class="edit-textarea"
              placeholder="介绍一下自己..."
              rows="4"
            ></textarea>
          </div>

          <div class="edit-actions">
            <button class="cancel-btn" @click="closeEditModal">取消</button>
            <button class="save-btn" @click="saveProfile" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>

          <p v-if="editSuccess" class="success-msg">保存成功</p>
          <p v-if="editError" class="error-msg">{{ editError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getWorks } from '../api/work'
import { getFollowList, toggleFollow } from '../api/follow'
import { uploadAvatar } from '../api/upload'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const activeTab = ref('works')
const myWorks = ref([])
const likedWorks = ref([])
const followingList = ref([])
const followerList = ref([])
const followingCount = ref(0)
const followerCount = ref(0)
const worksCount = ref(0)
const currentVideo = ref(null)
const videoPlayer = ref(null)

// 编辑资料弹窗
const showEditModal = ref(false)
const saving = ref(false)
const editSuccess = ref(false)
const editError = ref('')
const avatarFileInput = ref(null)
const editForm = ref({
  username: '',
  gender: 0,
  bio: '',
  avatar: '',
})

const openEditModal = () => {
  const user = userStore.user
  editForm.value = {
    username: user?.username || '',
    gender: user?.gender ?? 0,
    bio: user?.bio || '',
    avatar: user?.avatar || '',
  }
  editSuccess.value = false
  editError.value = ''
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
}

const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

const handleAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    editError.value = '请选择图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    editError.value = '图片大小不能超过5MB'
    return
  }
  try {
    const result = await uploadAvatar(file)
    if (result.code === 200) {
      editForm.value.avatar = result.data
      editError.value = ''
    } else {
      editError.value = result.message || '上传失败'
    }
  } catch (err) {
    editError.value = '上传失败，请重试'
  }
  // 重置input，允许重复选择同一文件
  e.target.value = ''
}

const saveProfile = async () => {
  if (!editForm.value.username.trim()) {
    editError.value = '用户名不能为空'
    return
  }
  saving.value = true
  editError.value = ''
  editSuccess.value = false
  try {
    const result = await request.put('/auth/user/profile', {
      username: editForm.value.username.trim(),
      gender: editForm.value.gender,
      bio: editForm.value.bio.trim(),
      avatar: editForm.value.avatar,
    })
    if (result.code === 200) {
      // 更新store中的用户信息
      userStore.setUser({
        ...userStore.user,
        username: editForm.value.username.trim(),
        gender: editForm.value.gender,
        bio: editForm.value.bio.trim(),
        avatar: editForm.value.avatar,
      })
      editSuccess.value = true
      setTimeout(() => {
        closeEditModal()
      }, 1000)
    } else {
      editError.value = result.message || '保存失败'
    }
  } catch (err) {
    editError.value = '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

const loadMyWorks = async () => {
  if (!userStore.user) return
  try {
    const result = await getWorks(1, 100)
    if (result.code === 200) {
      myWorks.value = result.data.filter(w => w.userId === userStore.user.id)
      worksCount.value = myWorks.value.length
    }
  } catch (err) {
    console.error('加载作品失败', err)
  }
}

const loadFollowData = async () => {
  if (!userStore.user) return
  try {
    const result = await getFollowList(userStore.user.id)
    if (result.code === 200) {
      followingList.value = result.data.following
      followerList.value = result.data.followers
      followingCount.value = result.data.following.length
      followerCount.value = result.data.followers.length
    }
  } catch (err) {
    console.error('加载关注数据失败', err)
  }
}

const openVideo = (work) => {
  currentVideo.value = work
  document.body.style.overflow = 'hidden'
}

const closeVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
  }
  currentVideo.value = null
  document.body.style.overflow = ''
}

const handleFollow = async (userId) => {
  try {
    const result = await toggleFollow(userId)
    if (result.code === 200 && !result.data) {
      followingList.value = followingList.value.filter(u => u.id !== userId)
      followingCount.value--
    }
  } catch (err) {
    console.error('取消关注失败', err)
  }
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadMyWorks()
    loadFollowData()
  }
})
</script>

<style scoped>
.my-container {
  height: 100%;
  overflow-y: auto;
  background: #1a1a1a;
}

.profile-header-section {
  padding: 40px 60px;
  background: linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%);
}

.profile-info {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  cursor: pointer;
}

.avatar-wrapper:hover .avatar-edit-hint {
  opacity: 1;
}

.avatar-edit-hint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.username {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  margin: 0;
}

.edit-btn {
  padding: 6px 20px;
  background: transparent;
  border: 1px solid #555;
  border-radius: 16px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.stats-row {
  display: flex;
  gap: 32px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-num {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.stat-label {
  font-size: 13px;
  color: #888;
}

.user-bio {
  font-size: 14px;
  color: #aaa;
  margin: 0;
}

/* Tabs */
.tabs-bar {
  display: flex;
  gap: 0;
  border-bottom: 1px solid #2a2a2a;
  padding: 0 60px;
}

.tab-item {
  padding: 14px 24px;
  font-size: 14px;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #fff;
}

.tab-item.active {
  color: #fff;
  border-bottom-color: #fe2c55;
}

/* Content */
.tab-content {
  padding: 20px 60px;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.work-card {
  background: #2a2a2a;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.work-card:hover {
  transform: translateY(-4px);
}

.work-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 130%;
  overflow: hidden;
}

.work-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.work-card:hover .play-overlay {
  opacity: 1;
}

.work-info {
  padding: 10px;
}

.work-title {
  font-size: 13px;
  color: #fff;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.work-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

/* User List */
.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #2a2a2a;
  border-radius: 8px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
}

.user-bio {
  font-size: 12px;
  color: #888;
}

.follow-btn {
  padding: 6px 20px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.follow-btn.following {
  background: #3a3a3a;
  color: #fff;
}

.follow-btn.following:hover {
  background: #fe2c55;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #888;
}

.upload-link {
  display: inline-block;
  margin-top: 12px;
  padding: 8px 20px;
  background: #fe2c55;
  color: #fff;
  text-decoration: none;
  border-radius: 16px;
  font-size: 13px;
}

/* Video Modal */
.video-modal {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  position: relative;
  width: 80%;
  max-width: 900px;
}

.close-btn {
  position: absolute;
  top: -40px;
  right: 0;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.modal-video {
  width: 100%;
  border-radius: 8px;
}

.video-info-overlay {
  padding: 16px 0;
}

.video-info-overlay h3 {
  color: #fff;
  font-size: 18px;
  margin: 0 0 8px 0;
}

.video-info-overlay p {
  color: #aaa;
  font-size: 14px;
  margin: 0;
}

/* Edit Modal */
.edit-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.edit-modal-content {
  background: #2a2a2a;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}

.edit-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #3a3a3a;
}

.edit-modal-header h3 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.edit-modal-header .close-btn {
  position: static;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-modal-body {
  padding: 24px;
}

.avatar-edit-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar-edit-label {
  color: #aaa;
  font-size: 14px;
  min-width: 48px;
}

.avatar-edit-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar-edit-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-edit-overlay span {
  color: #fff;
  font-size: 11px;
}

.edit-field {
  margin-bottom: 20px;
}

.edit-field-label {
  display: block;
  color: #aaa;
  font-size: 14px;
  margin-bottom: 8px;
}

.edit-input {
  width: 100%;
  padding: 10px 14px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.edit-input:focus {
  border-color: #fe2c55;
}

.edit-textarea {
  width: 100%;
  padding: 10px 14px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.edit-textarea:focus {
  border-color: #fe2c55;
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
  color: #ccc;
  font-size: 14px;
}

.gender-option input[type="radio"] {
  accent-color: #fe2c55;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn {
  padding: 10px 24px;
  background: transparent;
  border: 1px solid #555;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #3a3a3a;
}

.save-btn {
  padding: 10px 24px;
  background: #fe2c55;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.save-btn:hover {
  background: #e6204a;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.success-msg {
  color: #4caf50;
  font-size: 13px;
  margin: 12px 0 0;
  text-align: center;
}

.error-msg {
  color: #fe2c55;
  font-size: 13px;
  margin: 12px 0 0;
  text-align: center;
}

.hidden-input {
  display: none;
}
</style>
