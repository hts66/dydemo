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
        <div class="nav-item" @click="openAiChat">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/>
            <circle cx="9" cy="10" r="1.5"/>
            <circle cx="15" cy="10" r="1.5"/>
          </svg>
          <span>AI 对话</span>
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
                <div v-for="friend in friends" :key="friend.id" class="friend-item" @click.stop="openChat(friend)">
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

    <!-- 聊天窗口 -->
    <div class="chat-panel" v-show="showChat">
      <div class="chat-sidebar">
        <div class="chat-sidebar-header">
          <span>消息</span>
          <button class="close-chat-btn" @click="closeChat">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        <div class="chat-sidebar-content">
          <div
            class="chat-sidebar-item ai-bot-item"
            :class="{ 'active': currentChatFriend?.id === AI_BOT.id }"
            @click="openAiChat"
          >
            <img :src="AI_BOT.avatar" />
            <span class="chat-sidebar-name">{{ AI_BOT.username }}</span>
            <span class="ai-badge">AI</span>
          </div>
          <div class="sidebar-divider"></div>
          <div v-if="friends.length === 0" class="no-friends">
            <p>你还没有好友</p>
            <p class="no-friends-hint">快去添加好友吧</p>
          </div>
          <div
            v-for="friend in friends"
            :key="friend.id"
            class="chat-sidebar-item"
            :class="{ 'active': currentChatFriend?.id === friend.id }"
            @click="openChat(friend)"
          >
            <img :src="friend.avatar || defaultAvatar" />
            <span class="chat-sidebar-name">{{ friend.username }}</span>
          </div>
        </div>
      </div>
      <div class="chat-main">
        <div class="chat-header" v-if="currentChatFriend">
          <div class="chat-user-info">
            <img :src="currentChatFriend?.avatar || defaultAvatar" />
            <span class="chat-username">{{ currentChatFriend?.username }}</span>
          </div>
          <button class="chat-more-btn">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#888">
              <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
            </svg>
          </button>
        </div>
        <div class="chat-messages" ref="chatMessagesRef">
          <div v-if="!currentChatFriend" class="no-chat-selected">
            <p>选择一个好友开始聊天</p>
          </div>
          <div v-else>
            <div 
              v-for="msg in chatMessages" 
              :key="msg.id" 
              class="chat-message"
              :class="{ 'sent': msg.senderId === userStore.user?.id }"
            >
              <img :src="msg.senderId === userStore.user?.id ? (userStore.user?.avatar || defaultAvatar) : (msg.senderAvatar || defaultAvatar)" class="msg-avatar" />
              <div class="msg-content">
                <span class="msg-text">{{ msg.content }}</span>
                <span class="msg-time">{{ formatMsgTime(msg.createdAt) }}</span>
              </div>
            </div>
            <div v-if="chatMessages.length === 0" class="no-messages">
              <p>暂无消息，开始聊天吧~</p>
            </div>
          </div>
        </div>
        <div class="chat-input-area" v-if="currentChatFriend">
          <input 
            type="text" 
            v-model="chatInput" 
            class="chat-input" 
            placeholder="发送消息..."
            @keyup.enter="sendChatMessage"
          />
          <button class="send-msg-btn" @click="sendChatMessage" :disabled="!chatInput.trim()">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { getFriends } from '../api/follow'
import { getChatMessages, sendMessage, saveBotMessage } from '../api/message'
import request from '../utils/request'

const userStore = useUserStore()
const router = useRouter()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const AI_BOT = {
  id: -1,
  username: 'AI 助手',
  avatar: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="20" fill="#fe2c55"/><text x="50" y="70" text-anchor="middle" font-size="52" font-weight="bold" fill="white" font-family="Arial">AI</text></svg>'),
}

const showFriends = ref(false)
const friends = ref([])
let friendsTimeout = null

const showChat = ref(false)
const currentChatFriend = ref(null)
const chatMessages = ref([])
const chatInput = ref('')
const chatMessagesRef = ref(null)

const goToFriends = () => {
  console.log('朋友按钮被点击了')
  router.push('/friends')
}

const goToFollowing = () => {
  console.log('关注按钮被点击了')
  router.push('/following')
}

const openAiChat = async () => {
  // 如果已经在和AI聊天，不清空现有消息
  const wasAiChat = currentChatFriend.value?.id === AI_BOT.id
  currentChatFriend.value = AI_BOT
  showChat.value = true
  chatInput.value = ''

  if (!wasAiChat) {
    chatMessages.value = []
    // 从数据库加载AI聊天历史
    if (userStore.user?.id) {
      try {
        const result = await getChatMessages(AI_BOT.id)
        if (result.code === 200 && result.data) {
          chatMessages.value = result.data.map(msg => ({
            id: msg.id,
            content: msg.content,
            senderId: msg.senderId,
            senderUsername: msg.senderId === AI_BOT.id ? AI_BOT.username : (msg.senderUsername || '我'),
            senderAvatar: msg.senderId === AI_BOT.id ? AI_BOT.avatar : (msg.senderAvatar || defaultAvatar),
            createdAt: msg.createdAt
          }))
        }
      } catch (_) {}
    }
  }
  await nextTick()
  scrollToBottom()
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

const openChat = async (friend) => {
  showFriends.value = false
  currentChatFriend.value = friend
  showChat.value = true
  chatMessages.value = []
  chatInput.value = ''
  
  if (userStore.user?.id) {
    try {
      const result = await getChatMessages(friend.id)
      if (result.code === 200) {
        chatMessages.value = result.data
      }
    } catch (err) {
      console.error('获取聊天记录失败', err)
    }
  }
  
  await nextTick()
  scrollToBottom()
}

const closeChat = () => {
  showChat.value = false
  currentChatFriend.value = null
  chatMessages.value = []
  chatInput.value = ''
}

const sendChatMessage = async () => {
  if (!chatInput.value.trim() || !currentChatFriend.value || !userStore.user?.id) return

  const message = chatInput.value.trim()
  chatInput.value = ''

  // 立即在界面显示用户消息
  chatMessages.value.push({
    id: Date.now(),
    content: message,
    senderId: userStore.user.id,
    senderUsername: userStore.user.username || '我',
    senderAvatar: userStore.user.avatar || defaultAvatar,
    createdAt: new Date().toISOString()
  })
  await nextTick()
  scrollToBottom()

  try {
    // 模式1: AI 助手聊天
    if (currentChatFriend.value.id === AI_BOT.id) {
      // 后台持久化用户消息（不阻塞UI）
      sendMessage(AI_BOT.id, message).catch(() => {})

      // 构建对话历史
      const history = []
      for (const msg of chatMessages.value) {
        if (msg.senderId === AI_BOT.id) {
          history.push({ role: 'assistant', content: msg.content })
        } else if (msg.senderId === userStore.user.id) {
          history.push({ role: 'user', content: msg.content })
        }
      }
      const botResult = await request.post('/chat/bot', { messages: history })
      if (botResult.code === 200) {
        const botMsg = {
          id: Date.now() + 1,
          content: botResult.data,
          senderId: AI_BOT.id,
          senderUsername: AI_BOT.username,
          senderAvatar: AI_BOT.avatar,
          createdAt: new Date().toISOString()
        }
        chatMessages.value.push(botMsg)
        // 后台持久化AI回复
        saveBotMessage(botResult.data).catch(() => {})
        await nextTick()
        scrollToBottom()
      }
      return
    }

    // 模式2: 普通好友聊天
    sendMessage(currentChatFriend.value.id, message).catch(() => {})
  } catch (err) {
    console.error('发送消息失败', err)
  }
}

const scrollToBottom = () => {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

const formatMsgTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
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

.chat-panel {
  position: fixed;
  top: 80px;
  right: 24px;
  width: 600px;
  height: 500px;
  background: #1a1a1a;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  border: 1px solid #3a3a3a;
  display: flex;
  z-index: 1000001;
}

.chat-sidebar {
  width: 200px;
  border-right: 1px solid #3a3a3a;
  display: flex;
  flex-direction: column;
}

.chat-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #3a3a3a;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  background: #2a2a2a;
  border-radius: 16px 0 0 0;
}

.chat-sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.chat-sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-sidebar-item:hover {
  background: #2a2a2a;
}

.chat-sidebar-item.active {
  background: #fe2c55;
}

.chat-sidebar-item img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-sidebar-name {
  font-size: 13px;
  color: #fff;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: 0 16px 16px 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #3a3a3a;
  background: #2a2a2a;
  border-radius: 0 16px 0 0;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-user-info img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-username {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.chat-more-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  transition: background 0.2s;
}

.chat-more-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.close-chat-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  transition: background 0.2s;
}

.close-chat-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.no-messages {
  padding: 24px;
  text-align: center;
  color: #888;
  font-size: 13px;
}

.no-messages p {
  margin: 0;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  max-width: 100%;
}

.chat-message.sent {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.msg-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 70%;
}

.chat-message.sent .msg-content {
  align-items: flex-end;
}

.msg-text {
  background: #2a2a2a;
  padding: 8px 12px;
  border-radius: 16px;
  font-size: 13px;
  color: #fff;
  word-break: break-word;
}

.chat-message.sent .msg-text {
  background: #fe2c55;
}

.msg-time {
  font-size: 11px;
  color: #888;
}

.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #3a3a3a;
  background: #2a2a2a;
}

.chat-input {
  flex: 1;
  padding: 10px 14px;
  background: #3a3a3a;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  outline: none;
}

.chat-input::placeholder {
  color: #888;
}

.send-msg-btn {
  padding: 10px 14px;
  background: #fe2c55;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.send-msg-btn:hover:not(:disabled) {
  background: #e0264d;
}

.send-msg-btn:disabled {
  background: #555;
  cursor: not-allowed;
}

.no-chat-selected {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #888;
  font-size: 14px;
}

.no-chat-selected p {
  margin: 0;
}

.no-friends-hint {
  font-size: 11px;
  color: #888;
  margin-top: 4px !important;
}

.ai-bot-item {
  position: relative;
}

.ai-badge {
  margin-left: auto;
  padding: 2px 6px;
  background: linear-gradient(135deg, #fe2c55, #ff6b81);
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}

.sidebar-divider {
  height: 1px;
  background: #3a3a3a;
  margin: 4px 12px;
}

.page-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  min-height: 0;
}
</style>
