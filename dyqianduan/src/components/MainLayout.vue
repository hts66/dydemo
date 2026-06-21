<template>
  <div class="main-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <div class="logo-section">
        <div class="logo-icon">
          <svg viewBox="0 0 48 48" width="32" height="32">
            <path fill="#fe2c55" d="M34.125 10.32a7.07 7.07 0 0 1-4.095-1.29 7.07 7.07 0 0 1-2.94-3.51h-6.66v24.18a5.94 5.94 0 1 1-3.48-5.4v-6.72a12.42 12.42 0 1 0 10.08 12.18V18.18a13.5 13.5 0 0 0 7.92 2.52V14.04a7.08 7.08 0 0 1-.825-.06z"/>
          </svg>
        </div>
        <span class="logo-text">抖音精选</span>
      </div>

      <nav class="nav-menu">
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
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
          </svg>
          <span>AI 搜索</span>
        </div>
      </nav>

      <div class="nav-divider"></div>

      <nav class="nav-menu">
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
          <span>关注</span>
          <span class="badge">1</span>
        </div>
        <div class="nav-item">
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

      <div class="nav-divider"></div>

      <nav class="nav-menu">
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M21 6h-7.59l3.29-3.29L16 2l-4 4-4-4-.71.71L10.59 6H3c-1.1 0-2 .89-2 2v12c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.11-.9-2-2-2zm0 14H3V8h18v12zM9 10v8l7-4z"/>
          </svg>
          <span>直播</span>
        </div>
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/>
          </svg>
          <span>放映厅</span>
        </div>
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z"/>
          </svg>
          <span>短剧</span>
        </div>
        <div class="nav-item">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-10 7H8v3H6v-3H3v-2h3V8h2v3h3v2zm4.5 2c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4-3c-.83 0-1.5-.67-1.5-1.5S18.67 9 19.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/>
          </svg>
          <span>小游戏</span>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="download-btn">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
            <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/>
          </svg>
          <span>下载抖音精选</span>
        </div>
        <div class="footer-text">手机版时看更方便</div>
      </div>
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
          <span class="action-item">免广告</span>
          <span class="action-item">客户端</span>
          <span class="action-item">壁纸</span>
          <span class="action-item">通知</span>
          <span class="action-item">私信</span>
          <span class="action-item">投稿</span>
          <div class="user-avatar" v-if="userStore.isLoggedIn" @click="$router.push('/my')">
            <img :src="userStore.user?.avatar || defaultAvatar" />
          </div>
          <button v-else class="login-btn" @click="$router.push('/login')">登录</button>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="page-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
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
}
</style>
