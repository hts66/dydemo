<template>
  <div class="main-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <nav class="nav-menu" style="padding-top: 24px;">
        <div class="nav-item" :class="{ active: $route.path === '/featured' }" @click="$router.push('/featured')">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"/>
          </svg>
          <span>精选</span>
        </div>
        <div class="nav-item" :class="{ active: $route.path === '/recommend' }" @click="$router.push('/recommend')">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-2z"/>
          </svg>
          <span>推荐</span>
        </div>
        <div class="nav-item" @click="showAiSearchTip">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
          </svg>
          <span>AI 搜索</span>
        </div>
      </nav>

      <div class="nav-divider"></div>

      <nav class="nav-menu">
        <div class="nav-item" :class="{ active: $route.path === '/following' }" @click="goToFollowing">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
          <span>关注</span>
        </div>
        <div class="nav-item" :class="{ active: $route.path === '/friends' }" @click="goToFriends">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/>
          </svg>
          <span>朋友</span>
        </div>
        <div class="nav-item" :class="{ active: $route.path === '/my' }" @click="$router.push('/my')">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
          <span>我的</span>
        </div>
      </nav>

      
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部栏 -->
      <header class="top-bar">
        <div class="search-container">
          <input type="text" placeholder="搜索你感兴趣的内容" class="search-input" />
          <button class="search-btn">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
            </svg>
            搜索
          </button>
        </div>
        <div class="top-actions">
          <div 
            class="user-avatar-container" 
            v-if="userStore.isLoggedIn"
            @mouseenter="showFriendsPopup"
            @mouseleave="hideFriendsPopup"
          >
            <div class="user-avatar" @click="$router.push('/my')">
              <img :src="userStore.user?.avatar || defaultAvatar" />
            </div>
            <div class="friends-popup" v-show="showFriends">
              <div class="friends-popup-header">
                <span>好友列表</span>
              </div>
              <div class="friends-popup-content">
                <div v-if="friends.length === 0" class="no-friends">
                  <p>你还没有好友，快去添加吧</p>
                </div>
                <div v-for="friend in friends" :key="friend.id" class="friend-item" @click.stop="goToProfile(friend.id)">
                  <img :src="friend.avatar || defaultAvatar" />
                  <span class="friend-name">{{ friend.username }}</span>
                </div>
              </div>
            </div>
          </div>
          <button v-else class="login-btn" @click="$router.push('/login')">登录</button>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="page-content">
        <router-view :key="$route.fullPath" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { getFriends } from '../api/follow'

const userStore = useUserStore()
const router = useRouter()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const showFriends = ref(false)
const friends = ref([])
let friendsTimeout = null

const goToFriends = () => {
  console.log('朋友按钮被点击了')
  router.push('/friends')
}

const goToFollowing = () => {
  console.log('关注按钮被点击了')
  router.push('/following')
}

const showAiSearchTip = () => {
  alert('该功能尚未开发')
}

const goToProfile = (userId) => {
  if (userId) {
    router.push(`/profile/${userId}`)
  }
}

const showFriendsPopup = async () => {
  if (friendsTimeout) clearTimeout(friendsTimeout)
  showFriends.value = true
  if (friends.value.length === 0 && userStore.user?.id) {
    try {
      const result = await getFriends(userStore.user.id)
      if (result.code === 200) {
        friends.value = result.data
      }
    } catch (err) {
      console.error('获取好友列表失败', err)
    }
  }
}

const hideFriendsPopup = () => {
  friendsTimeout = setTimeout(() => {
    showFriends.value = false
  }, 200)
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  background: #000;
  color: #fff;
}

.sidebar {
  width: 200px;
  background: #1a1a1a;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  border-right: 1px solid #2a2a2a;
  z-index: 999999;
  position: relative;
  flex-shrink: 0;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px 24px;
}

.logo-icon {
  width: 32px;
  height: 32px;
}

.logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
  pointer-events: auto;
  z-index: 1;
}

.nav-item:hover {
  background: #2a2a2a;
}

.nav-item.active {
  background: #2a2a2a;
  font-weight: 500;
}

.nav-item svg {
  flex-shrink: 0;
}

.nav-item span {
  font-size: 14px;
}

.badge {
  position: absolute;
  right: 16px;
  background: #fe2c55;
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.nav-divider {
  height: 1px;
  background: #2a2a2a;
  margin: 8px 16px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
}

.download-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fe2c55;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.download-btn:hover {
  background: #e0264d;
}

.footer-text {
  font-size: 12px;
  color: #888;
  margin-top: 8px;
  text-align: center;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #1a1a1a;
  border-bottom: 1px solid #2a2a2a;
}

.search-container {
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 500px;
  background: #2a2a2a;
  border-radius: 20px;
  overflow: hidden;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.search-input::placeholder {
  color: #888;
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: #fe2c55;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.search-btn:hover {
  background: #e0264d;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-item {
  font-size: 13px;
  color: #ccc;
  cursor: pointer;
  transition: color 0.2s;
}

.action-item:hover {
  color: #fff;
}

.user-avatar-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.friends-popup {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background: #2a2a2a;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  overflow: hidden;
  z-index: 1000000;
  border: 1px solid #3a3a3a;
}

.friends-popup-header {
  padding: 12px 16px;
  border-bottom: 1px solid #3a3a3a;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.friends-popup-content {
  max-height: 300px;
  overflow-y: auto;
}

.no-friends {
  padding: 24px;
  text-align: center;
  color: #888;
  font-size: 13px;
}

.no-friends p {
  margin: 0;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.friend-item:hover {
  background: #3a3a3a;
}

.friend-item img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.friend-name {
  font-size: 13px;
  color: #fff;
}

.login-btn {
  padding: 6px 16px;
  background: #fe2c55;
  border: none;
  border-radius: 16px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.login-btn:hover {
  background: #e0264d;
}

.page-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  min-height: 0;
}
</style>
