﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="my-container" ref="containerRef" @scroll="handleScroll">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="goBack">
      <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff">
        <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
      </svg>
    </button>
    <div
      class="profile-header-section"
      :class="{ 'header-editable': isOwnProfile }"
      :style="headerStyle"
      @click="isOwnProfile && toggleBackgroundPicker()"
    >
      <div v-if="isOwnProfile" class="background-hint">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff">
          <path d="M20.71 4.04a1 1 0 0 0-1.42 0l-3.02 3.02-7.19-7.19a1 1 0 0 0-1.42 0L3.29 5.36a1 1 0 0 0 0 1.42l7.19 7.19-3.02 3.02a1 1 0 0 0 0 1.42l2.12 2.12a1 1 0 0 0 1.42 0l3.02-3.02 7.19 7.19a1 1 0 0 0 1.42 0l2.12-2.12a1 1 0 0 0 0-1.42l-7.19-7.19 3.02-3.02a1 1 0 0 0 0-1.42z"/>
        </svg>
        <span>点击更换背景</span>
      </div>
      <div class="profile-info" @click.stop>
        <div
          class="avatar-wrapper"
          :class="{ 'avatar-editable': isOwnProfile }"
          @click.stop="isOwnProfile && triggerAvatarUpload()"
        >
          <img :src="previewAvatar || targetUser?.avatar || defaultAvatar" class="avatar-img" />
          <div v-if="isOwnProfile" class="avatar-edit-overlay">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff">
              <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
            </svg>
          </div>
        </div>
        <input
          type="file"
          ref="avatarFileInput"
          accept="image/*"
          style="display: none"
          @change="handleAvatarSelect"
        />
        <div class="user-details">
          <div class="username-row">
            <h2 class="username">{{ targetUser?.username || '用户' }}</h2>
            <button 
              v-if="!isOwnProfile && userStore.isLoggedIn"
              class="profile-follow-btn"
              :class="{ followed: isFollowingTarget }"
              @click.stop="handleFollowTarget"
            >
              {{ isFollowingTarget ? '已关注' : '+ 关注' }}
            </button>
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
          <p class="user-bio">{{ targetUser?.bio || '这个人很懒，什么都没写~' }}</p>
          <div v-if="showAvatarActions" class="avatar-actions">
            <button class="avatar-confirm-btn" @click.stop="confirmAvatar">确认更换</button>
            <button class="avatar-cancel-btn" @click.stop="cancelAvatar">取消</button>
          </div>
        </div>
      </div>
    </div>

    <div class="tabs-bar">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'works' }"
        @click="handleTabClick('works')"
      >
        作品 {{ targetWorks.length }}
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'following' }"
        @click="handleTabClick('following')"
      >
        关注
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'followers' }"
        @click="handleTabClick('followers')"
      >
        粉丝
      </div>
    </div>

    <div class="tab-content">
      <div v-if="activeTab === 'works'" class="works-grid">
        <div 
          v-for="work in targetWorks" 
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
        <div v-if="targetWorks.length === 0 && !worksLoading" class="empty-state">
          <p>还没有发布作品</p>
        </div>
        <div v-if="worksLoading" class="loading-state">
          <p>加载中...</p>
        </div>
      </div>

      <div v-if="activeTab === 'following'" class="user-list">
        <div 
          v-for="user in followingList" 
          :key="user.id" 
          class="user-item"
          @click="goToProfile(user.id)"
        >
          <img :src="user.avatar || defaultAvatar" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">{{ user.username || '匿名用户' }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒' }}</span>
          </div>
        </div>
        <div v-if="followingList.length === 0" class="empty-state">
          <p>还没有关注任何人</p>
        </div>
      </div>

      <div v-if="activeTab === 'followers'" class="user-list">
        <div 
          v-for="user in followerList" 
          :key="user.id" 
          class="user-item"
          @click="goToProfile(user.id)"
        >
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

    <!-- 视频播放弹窗（可复用组件） -->
    <VideoPlayerModal
      :visible="showVideoModal"
      :work="currentVideo"
      @close="closeVideo"
      @work-updated="handleWorkUpdated"
    />

    <div v-if="showBackgroundPicker" class="background-modal" @click.self="toggleBackgroundPicker">
      <div class="background-modal-content">
        <div class="background-modal-header">
          <h3>上传背景图片</h3>
          <button class="close-btn" @click="toggleBackgroundPicker">✕</button>
        </div>
        <div class="background-upload-area">
          <input 
            type="file" 
            ref="backgroundFileInput"
            accept="image/*"
            class="background-file-input"
            @change="handleBackgroundUpload"
          />
          <div class="upload-placeholder" v-if="!previewBackground" @click="triggerBackgroundUpload">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="#888">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
            <p>点击上传背景图片</p>
            <p class="upload-hint">支持 jpg、png、gif 格式</p>
          </div>
          <div v-if="previewBackground" class="background-preview">
            <img :src="previewBackground" />
            <div class="bg-actions">
              <button class="bg-confirm-btn" @click.stop="confirmBackground">确认更换</button>
            </div>
            <button class="remove-bg-btn" @click="removeBackground">移除背景</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getUserWorks } from '../api/work'
import { getFollowList, toggleFollow, checkIsFollowing } from '../api/follow'
import { getUserById, updateBackground, updateAvatar, updateBackgroundFile } from '../api/user'
import VideoPlayerModal from '../components/VideoPlayerModal.vue'
import { emit, EVENT_KEY } from '../utils/bus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const containerRef = ref(null)

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/featured')
  }
}

const activeTab = ref('works')
const targetUser = ref(null)
const targetWorks = ref([])
const followingList = ref([])
const followerList = ref([])
const followingCount = ref(0)
const followerCount = ref(0)
const worksCount = ref(0)
const isFollowingTarget = ref(false)
const currentVideo = ref(null)
const showVideoModal = ref(false)
const headerHeight = ref(300)

// 分页懒加载
const worksPage = ref(1)
const worksHasMore = ref(true)
const worksLoading = ref(false)

const isOwnProfile = computed(() => {
  return userStore.user && targetUser.value && userStore.user.id === targetUser.value.id
})

const currentBackground = ref('')
const showBackgroundPicker = ref(false)
const backgroundFileInput = ref(null)
const previewBackground = ref('')
const pendingBackgroundFile = ref(null)   // 待上传的背景图文件

const avatarFileInput = ref(null)
const previewAvatar = ref('')
const showAvatarActions = ref(false)
const pendingAvatarFile = ref(null)

const toggleBackgroundPicker = () => {
  if (isOwnProfile.value) {
    showBackgroundPicker.value = !showBackgroundPicker.value
    if (showBackgroundPicker.value && currentBackground.value) {
      previewBackground.value = currentBackground.value
    } else {
      // 关闭弹窗 → 丢弃所有本地预览
      previewBackground.value = ''
      pendingBackgroundFile.value = null
    }
  }
}

const triggerBackgroundUpload = () => {
  backgroundFileInput.value?.click()
}

const handleBackgroundUpload = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 仅本地预览，不上传到 MinIO！上传在 confirmBackground 点"确认更换"时才执行
  pendingBackgroundFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewBackground.value = ev.target.result
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

const confirmBackground = async () => {
  if (!pendingBackgroundFile.value) return

  try {
    const result = await updateBackgroundFile(pendingBackgroundFile.value)
    if (result.code === 200) {
      currentBackground.value = result.data.background
      previewBackground.value = result.data.background
      userStore.setUser(result.data)
      pendingBackgroundFile.value = null
    }
  } catch (err) {
    console.error('更换背景图失败', err)
  }
}

const cancelBackground = () => {
  previewBackground.value = currentBackground.value || ''
  pendingBackgroundFile.value = null
}

const removeBackground = async () => {
  currentBackground.value = ''
  previewBackground.value = ''
  userStore.user.background = ''
  try {
    await updateBackground('')
  } catch (err) {
    console.error('移除背景失败', err)
  }
}

const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

const handleAvatarSelect = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 本地预览
  pendingAvatarFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    previewAvatar.value = e.target.result
    showAvatarActions.value = true
  }
  reader.readAsDataURL(file)

  // 重置 input，允许重复选择同一文件
  event.target.value = ''
}

const confirmAvatar = async () => {
  if (!pendingAvatarFile.value) return

  try {
    const result = await updateAvatar(pendingAvatarFile.value)
    if (result.code === 200) {
      userStore.setUser(result.data)
      // 清除本地预览，使用 MinIO 返回的 URL
      previewAvatar.value = ''
    }
  } catch (err) {
    console.error('更换头像失败', err)
  } finally {
    showAvatarActions.value = false
    pendingAvatarFile.value = null
  }
}

const cancelAvatar = () => {
  previewAvatar.value = ''
  showAvatarActions.value = false
  pendingAvatarFile.value = null
}

const headerStyle = computed(() => {
  const styles = {
    height: headerHeight.value + 'px'
  }
  if (currentBackground.value) {
    styles.backgroundImage = `url(${currentBackground.value})`
    styles.backgroundSize = 'cover'
    styles.backgroundPosition = 'center'
    styles.backgroundRepeat = 'no-repeat'
  } else {
    styles.background = 'linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%)'
  }
  return styles
})

const handleScroll = (event) => {
  const { scrollTop, scrollHeight, clientHeight } = event.target
  // 头部缩放
  const minHeight = 120
  const maxHeight = 300
  const newHeight = Math.max(minHeight, maxHeight - scrollTop * 0.5)
  headerHeight.value = newHeight
  // 触底加载更多作品
  if (activeTab.value === 'works' && scrollTop + clientHeight >= scrollHeight - 150) {
    loadWorks()
  }
}

const loadUserProfile = async () => {
  const userId = parseInt(route.params.userId)
  
  if (!userId) {
    if (userStore.user) {
      targetUser.value = userStore.user
      loadWorks()
      loadFollowData()
      if (userStore.user?.background) {
        currentBackground.value = userStore.user.background
      }
    }
    return
  }
  
  try {
    const result = await getUserById(userId)
    if (result.code === 200) {
      targetUser.value = result.data
      loadWorks()
      loadFollowData()
      
      if (targetUser.value.background) {
        currentBackground.value = targetUser.value.background
      }
      
      if (userStore.user && userStore.user.id !== userId) {
        const followResult = await checkIsFollowing(userId)
        if (followResult.code === 200) {
          isFollowingTarget.value = followResult.data
        }
      }
    }
  } catch (err) {
    console.error('加载用户资料失败', err)
  }
}

const loadWorks = async (reset = false) => {
  if (!targetUser.value) return
  if (worksLoading.value) return
  if (reset) {
    worksPage.value = 1
    worksHasMore.value = true
    targetWorks.value = []
  }
  if (!worksHasMore.value) return
  worksLoading.value = true
  try {
    const result = await getUserWorks(targetUser.value.id, worksPage.value, 12)
    if (result.code === 200) {
      if (result.data.length === 0) {
        worksHasMore.value = false
      } else {
        targetWorks.value.push(...result.data)
        worksPage.value++
      }
      worksCount.value = targetWorks.value.reduce((sum, w) => sum + (w.likesCount || 0), 0)
    }
  } catch (err) {
    console.error('加载作品失败', err)
  } finally {
    worksLoading.value = false
  }
}

const loadFollowData = async () => {
  if (!targetUser.value) return
  try {
    const userId = targetUser.value.id
    const result = await getFollowList(userId)
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

const handleFollowTarget = async () => {
  if (!targetUser.value) return
  
  try {
    const result = await toggleFollow(targetUser.value.id)
    if (result.code === 200) {
      isFollowingTarget.value = result.data
      if (result.data) {
        followerCount.value++
      } else {
        followerCount.value--
      }
      // 广播关注状态变化，通知推荐/关注/朋友界面同步
      emit(EVENT_KEY.FOLLOW_STATUS_CHANGED, { userId: targetUser.value.id, isFollowing: result.data })
    }
  } catch (err) {
    console.error('关注操作失败', err)
  }
}

const goToProfile = (userId) => {
  if (!userId) return
  
  const currentProfileId = route.params.userId
  if (currentProfileId && String(currentProfileId) === String(userId)) {
    alert('你已进入该作者主页')
    return
  }
  
  if (userStore.isLoggedIn) {
    const currentUserId = userStore.user?.id
    if (currentUserId && String(currentUserId) === String(userId)) {
      router.push('/my')
      return
    }
  }
  
  router.push(`/profile/${userId}`)
}

const openVideo = (work) => {
  currentVideo.value = { ...work }
  showVideoModal.value = true
}

const closeVideo = () => {
  showVideoModal.value = false
  currentVideo.value = null
}

const handleWorkUpdated = (updatedWork) => {
  const idx = targetWorks.value.findIndex(w => w.id === updatedWork.id)
  if (idx !== -1) {
    targetWorks.value[idx] = { ...targetWorks.value[idx], ...updatedWork }
  }
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const handleTabClick = (tab) => {
  activeTab.value = tab
  if (tab === 'works') {
    loadWorks(true)
  }
}

onMounted(() => {
  loadUserProfile()
})

// keep-alive 激活时：若 userId 变化（跳转到另一位作者主页），重新加载该用户数据
onActivated(async () => {
  const routeUserId = route.params.userId ? parseInt(route.params.userId) : null
  const currentUser = targetUser.value
  const currentLoadedId = currentUser?.id || null

  if (routeUserId !== currentLoadedId) {
    // 重置状态，避免残留上一个作者的数据
    activeTab.value = 'works'
    targetWorks.value = []
    worksPage.value = 1
    worksHasMore.value = true
    followingList.value = []
    followerList.value = []
    isFollowingTarget.value = false
    currentBackground.value = ''
    headerHeight.value = 300
    await nextTick()
    if (containerRef.value) containerRef.value.scrollTop = 0
    loadUserProfile()
  } else if (containerRef.value) {
    // 同一作者主页重新激活（如从推荐界面返回），恢复滚动位置由浏览器/keep-alive 处理
  }
})
</script>

<style scoped>
.my-container {
  height: 100%;
  overflow-y: auto;
  background: #1a1a1a;
}

.back-btn {
  position: fixed;
  top: 72px;
  right: 32px;
  z-index: 100;
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.5);
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.7);
}

.profile-header-section {
  padding: 20px 60px;
  padding-bottom: 30px;
  position: relative;
  overflow: hidden;
  transition: height 0.1s ease-out;
}

.header-editable {
  cursor: pointer;
}

.profile-header-section::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.9));
  pointer-events: none;
}

.background-hint {
  position: absolute;
  bottom: 20px;
  right: 60px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 16px;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.profile-header-section:hover .background-hint {
  opacity: 1;
}

.profile-info {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-editable {
  cursor: pointer;
  position: relative;
}

.avatar-editable:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.avatar-confirm-btn,
.avatar-cancel-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.avatar-confirm-btn {
  background: #fe2c55;
  color: #fff;
}

.avatar-confirm-btn:hover {
  background: #e6204a;
}

.avatar-cancel-btn {
  background: #3a3a3a;
  color: #ccc;
}

.avatar-cancel-btn:hover {
  background: #4a4a4a;
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
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
}

.profile-follow-btn {
  padding: 6px 20px;
  background: #fe2c55;
  border: none;
  border-radius: 16px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.profile-follow-btn:hover {
  background: #e6204a;
}

.profile-follow-btn.followed {
  background: #3a3a3a;
}

.profile-follow-btn.followed:hover {
  background: #fe2c55;
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
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
}

.stat-label {
  font-size: 13px;
  color: #ddd;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.user-bio {
  font-size: 14px;
  color: #ddd;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

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
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #3a3a3a;
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

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #888;
}

.loading-state {
  text-align: center;
  padding: 20px 0;
  color: #888;
  font-size: 13px;
}

.background-modal {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.background-modal-content {
  background: #2a2a2a;
  border-radius: 12px;
  width: 400px;
  max-width: 90vw;
  overflow: hidden;
}

.background-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #3a3a3a;
}

.background-modal-header h3 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.background-upload-area {
  padding: 20px;
}

.background-file-input {
  display: none;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  border: 2px dashed #444;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-placeholder:hover {
  border-color: #fe2c55;
  background: rgba(254, 44, 85, 0.1);
}

.upload-placeholder p {
  color: #888;
  margin: 8px 0 0 0;
  font-size: 14px;
}

.upload-hint {
  font-size: 12px !important;
  color: #666 !important;
}

.background-preview {
  margin-top: 16px;
  text-align: center;
}

.background-preview img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  object-fit: contain;
}

.bg-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: center;
}

.bg-confirm-btn,
.bg-cancel-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.bg-confirm-btn {
  background: #fe2c55;
  color: #fff;
}

.bg-confirm-btn:hover {
  background: #e6204a;
}

.bg-cancel-btn {
  background: #3a3a3a;
  color: #ccc;
}

.bg-cancel-btn:hover {
  background: #4a4a4a;
}

.remove-bg-btn {
  margin-top: 8px;
  padding: 8px 24px;
  background: #444;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.remove-bg-btn:hover {
  background: #555;
}
</style>
