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
          <button
            v-if="$route.path === '/search'"
            class="top-back-btn"
            @click="$router.back()"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="#fff">
              <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
            </svg>
          </button>
          <input
            type="text"
            v-model="searchKeyword"
            placeholder="搜索你感兴趣的内容"
            class="search-input"
            @keyup.enter="doSearch"
          />
          <button class="search-btn" @click="doSearch">
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
                <template v-if="msg.isRecommend">
                  <div class="recommend-text">{{ msg.content.text }}</div>
                  <div class="recommend-video-list">
                    <div
                      v-for="video in msg.recommendVideos"
                      :key="video.id"
                      class="recommend-video-card"
                      @click.stop="playRecommendVideo(video)"
                    >
                      <div class="recommend-video-thumb">
                        <img :src="proxyThumb(video.thumbnail)" alt="" @error="e => e.target.style.display='none'" />
                        <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff"><path d="M8 5v14l11-7z"/></svg>
                      </div>
                      <div class="recommend-video-info">
                        <span class="recommend-video-title">{{ video.title || '无标题' }}</span>
                        <span class="recommend-video-author">@{{ video.username || '匿名用户' }}</span>
                        <span class="recommend-video-tags" v-if="video.tags">{{ video.tags.slice(0,5).join(' · ') }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                <template v-else-if="isShareMsg(msg.content)">
                  <div class="share-video-card" @click.stop="playSharedVideo(msg.content)">
                    <div class="share-video-thumb">
                      <img v-if="getShareThumbnail(msg.content)" :src="getShareThumbnail(msg.content)" class="share-video-cover" alt="" />
                      <svg class="share-video-play-icon" viewBox="0 0 24 24" width="28" height="28" fill="#fff"><path d="M8 5v14l11-7z"/></svg>
                    </div>
                    <div class="share-video-info">
                      <span class="share-video-label">{{ getShareTitle(msg.content) }}</span>
                      <span class="share-video-desc" v-if="getShareDescription(msg.content)">{{ getShareDescription(msg.content) }}</span>
                    </div>
                  </div>
                </template>
                <span v-else class="msg-text">{{ msg.content }}</span>
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

    <!-- 分享视频播放器 — 完整模态体验（与精选界面一致） -->
    <div v-if="showSharedVideo" class="shared-video-overlay" @click.self="closeSharedVideo">
      <div class="shared-video-container">
        <button class="shared-video-close" @click="closeSharedVideo">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff">
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
          </svg>
          <span>返回</span>
        </button>
        <div class="shared-video-body">
          <div class="shared-video-section">
            <video
              :src="sharedVideoUrl"
              :poster="sharedVideoPoster"
              class="shared-video-player"
              controls
              autoplay
              playsinline
              webkit-playsinline
              x5-playsinline
            ></video>
          </div>
          <div class="shared-interaction-section">
            <div class="shared-author-bar">
              <button
                v-if="sharedVideoData && sharedVideoData.userId && sharedVideoData.userId !== userStore.user?.id"
                class="shared-follow-btn"
                :class="{ followed: sharedVideoData.isFollowing }"
                @click.stop="handleSharedFollow"
              >
                <img :src="sharedVideoData.avatar || defaultAvatar" @click.stop="goToSharedProfile" />
                <svg v-if="!sharedVideoData.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
              <span class="shared-author-name">{{ sharedVideoData?.username || '匿名用户' }}</span>
            </div>
            <div class="shared-video-info-panel">
              <h4 class="shared-video-panel-title">{{ sharedVideoData?.title || sharedVideoData?.description || '无标题' }}</h4>
              <p class="shared-video-panel-desc" v-if="sharedVideoData?.description">{{ sharedVideoData.description }}</p>
            </div>
            <div class="shared-action-bar" v-if="canInteract">
              <button
                class="shared-action-btn"
                :class="{ liked: sharedVideoData?.isLiked }"
                @click="handleSharedLike"
              >
                <svg v-if="sharedVideoData?.isLiked" viewBox="0 0 24 24" width="20" height="20" fill="#fe2c55">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                  <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3z"/>
                </svg>
                <span>{{ formatCount(sharedVideoData?.likesCount || 0) }}</span>
              </button>
              <button class="shared-action-btn" @click="scrollSharedToComments">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                  <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>
                </svg>
                <span>{{ formatCount(sharedVideoData?.commentsCount || 0) }}</span>
              </button>
            </div>
            <div class="shared-comments-section" ref="sharedCommentsRef">
              <h4 class="shared-comments-title">评论</h4>
              <div class="shared-comments-list">
                <div v-for="comment in sharedVideoComments" :key="comment.id" class="shared-comment-item">
                  <img :src="comment.avatar || defaultAvatar" class="shared-comment-avatar" />
                  <div class="shared-comment-content">
                    <span class="shared-comment-author">{{ comment.username || '匿名用户' }}</span>
                    <span class="shared-comment-text">{{ comment.content }}</span>
                  </div>
                  <span class="shared-comment-time">{{ formatTimeAgo(comment.createdAt) }}</span>
                </div>
                <div v-if="sharedVideoComments.length === 0" class="shared-no-comments">
                  <p>暂无评论，快来抢沙发吧~</p>
                </div>
              </div>
              <div class="shared-comment-input-section" v-if="canInteract">
                <input
                  type="text"
                  v-model="sharedVideoCommentInput"
                  class="shared-comment-input"
                  placeholder="输入评论..."
                  @keyup.enter="handleSharedComment"
                  :disabled="!userStore.isLoggedIn"
                />
                <button class="shared-send-btn" @click="handleSharedComment" :disabled="!sharedVideoCommentInput.trim() || !userStore.isLoggedIn">
                  发送
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { getFriends, toggleFollow, checkIsFollowing } from '../api/follow'
import { getChatMessages, sendMessage, saveBotMessage } from '../api/message'
import { getWorkById } from '../api/work'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
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

// 搜索
const searchKeyword = ref('')
const doSearch = () => {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/search', query: { keyword: kw } })
}

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

  // 加载好友列表（如果尚未加载）
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
        const isRecommend = botResult.data && botResult.data.type === 'recommend'
        const botMsg = {
          id: Date.now() + 1,
          content: isRecommend ? botResult.data : botResult.data,
          senderId: AI_BOT.id,
          senderUsername: AI_BOT.username,
          senderAvatar: AI_BOT.avatar,
          createdAt: new Date().toISOString(),
          isRecommend: isRecommend,
          recommendVideos: isRecommend ? botResult.data.videos : null
        }
        chatMessages.value.push(botMsg)
        // 后台持久化AI回复（推荐类消息存文本部分）
        const saveContent = isRecommend ? botResult.data.text : botResult.data
        saveBotMessage(saveContent).catch(() => {})
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

// 分享视频相关
const sharedVideoUrl = ref('')
const showSharedVideo = ref(false)
const sharedVideoData = ref(null)  // 完整的作品数据
const sharedVideoPoster = computed(() => {
  const thumb = sharedVideoData.value?.thumbnail
  if (thumb) {
    if (thumb.startsWith('http://') || thumb.startsWith('https://')) {
      return `/api/video/proxy?url=${encodeURIComponent(thumb)}`
    }
    return thumb
  }
  return ''
})

const isShareMsg = (content) => content && content.startsWith('📹 [分享视频]')

// 解析分享消息：兼容多代格式
// 旧格式(2行): 📹 [分享视频] {title}\n{url}
// 新格式(4行): 📹 [分享视频] {id}\n{title}\n{description}\n{url}
// 最新格式(5行): 📹 [分享视频] {id}\n{title}\n{description}\n{url}\n{thumbnail}
const parseShareMsg = (content) => {
  const lines = content.split('\n')
  const prefix = '📹 [分享视频] '
  const firstPart = lines[0]?.replace(prefix, '') || ''

  if (lines.length >= 5) {
    // 最新格式(5行): id, title, description, url, thumbnail
    return {
      workId: firstPart,
      title: lines[1] || '无标题',
      description: lines[2] || '',
      url: lines[3] || '',
      thumbnail: lines[4] || '',
      isNewFormat: true
    }
  } else if (lines.length >= 4) {
    // 新格式(4行): id, title, description, url
    return {
      workId: firstPart,
      title: lines[1] || '无标题',
      description: lines[2] || '',
      url: lines[3] || '',
      thumbnail: '',
      isNewFormat: true
    }
  } else {
    // 旧格式(2行): title, url
    return {
      workId: null,
      title: firstPart || '无标题',
      description: '',
      url: lines[1] || '',
      thumbnail: '',
      isNewFormat: false
    }
  }
}

const getShareTitle = (content) => parseShareMsg(content).title
const getShareDescription = (content) => parseShareMsg(content).description
const getShareUrl = (content) => parseShareMsg(content).url
const getShareWorkId = (content) => parseShareMsg(content).workId

// 获取分享视频的封面URL（外部URL走代理）
const getShareThumbnail = (content) => {
  const thumb = parseShareMsg(content).thumbnail
  if (thumb) {
    if (thumb.startsWith('http://') || thumb.startsWith('https://')) {
      return `/api/video/proxy?url=${encodeURIComponent(thumb)}`
    }
    return thumb
  }
  return ''
}

// 从后端获取完整作品数据
const fetchVideoDetail = async (workId) => {
  if (!workId) return null
  try {
    const result = await getWorkById(workId)
    if (result.code === 200) return result.data
  } catch (_) {}
  return null
}

// 点击推荐视频卡片播放
const playRecommendVideo = async (video) => {
  if (!video.url) return

  sharedVideoUrl.value = video.url.startsWith('http')
    ? `/api/video/proxy?url=${encodeURIComponent(video.url)}`
    : video.url

  sharedVideoData.value = {
    id: video.id,
    title: video.title,
    description: video.description || '',
    url: video.url,
    thumbnail: video.thumbnail || '',
    username: video.username || '匿名用户',
    isLiked: false,
    isFollowing: false
  }
  sharedVideoComments.value = []

  if (userStore.isLoggedIn && video.id) {
    try {
      const workData = await fetchVideoDetail(video.id)
      if (workData) {
        workData.isLiked = false
        workData.isFollowing = false
        const [likeResult, followResult] = await Promise.all([
          checkIsLiked(workData.id),
          workData.userId ? checkIsFollowing(workData.userId) : Promise.resolve({ code: 200, data: false })
        ])
        if (likeResult.code === 200) workData.isLiked = likeResult.data
        if (followResult.code === 200) workData.isFollowing = followResult.data
        sharedVideoData.value = { ...sharedVideoData.value, ...workData }
      }
    } catch (_) {}
  }

  // 加载评论
  if (video.id) {
    try {
      const cr = await getComments(video.id)
      if (cr.code === 200) sharedVideoComments.value = cr.data
    } catch (_) {}
  }

  showSharedVideo.value = true
  document.body.style.overflow = 'hidden'
}

// 封面图代理
const proxyThumb = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `/api/video/proxy?url=${encodeURIComponent(url)}`
  }
  return url
}

const playSharedVideo = async (content) => {
  const parsed = parseShareMsg(content)
  if (!parsed.url) return

  sharedVideoUrl.value = `/api/video/proxy?url=${encodeURIComponent(parsed.url)}`

  // 尝试获取完整作品数据
  if (parsed.workId) {
    const workData = await fetchVideoDetail(parseInt(parsed.workId))
    if (workData) {
      workData.isLiked = false
      workData.isFollowing = false
      // 补齐可能缺失的字段
      workData.title = workData.title || parsed.title
      workData.description = workData.description || parsed.description
      workData.url = workData.url || parsed.url

      if (userStore.isLoggedIn) {
        try {
          const [likeResult, followResult] = await Promise.all([
            checkIsLiked(workData.id),
            workData.userId ? checkIsFollowing(workData.userId) : Promise.resolve({ code: 200, data: false })
          ])
          if (likeResult.code === 200) workData.isLiked = likeResult.data
          if (followResult.code === 200) workData.isFollowing = followResult.data
        } catch (_) {}
      }
      sharedVideoData.value = workData

      // 加载评论
      try {
        const commentResult = await getComments(workData.id)
        if (commentResult.code === 200) {
          sharedVideoComments.value = commentResult.data
          scrollSharedCommentsToBottom()
        }
      } catch (_) {}
    } else {
      // 无法获取数据库数据时，用解析数据兜底
      sharedVideoData.value = { id: parseInt(parsed.workId) || 0, title: parsed.title, description: parsed.description, url: parsed.url, username: '', avatar: '' }
      sharedVideoComments.value = []
    }
  } else {
    // 旧格式消息：无 workId，用解析数据兜底
    sharedVideoData.value = { id: 0, title: parsed.title, description: parsed.description, url: parsed.url, username: '', avatar: '' }
    sharedVideoComments.value = []
  }

  document.body.style.overflow = 'hidden'
  showSharedVideo.value = true
}

const closeSharedVideo = () => {
  showSharedVideo.value = false
  sharedVideoUrl.value = ''
  sharedVideoData.value = null
  sharedVideoComments.value = []
  sharedVideoCommentInput.value = ''
  document.body.style.overflow = ''
}

// 分享视频详情面板 — 互动操作
const sharedVideoComments = ref([])
const sharedVideoCommentInput = ref('')

// 只要有有效的作品 ID，就允许点赞和评论
const canInteract = computed(() => !!(sharedVideoData.value?.id))

const handleSharedLike = async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  if (!canInteract.value) return
  try {
    const result = await toggleLike(sharedVideoData.value.id)
    if (result.code === 200) {
      sharedVideoData.value.isLiked = result.data
      sharedVideoData.value.likesCount = (sharedVideoData.value.likesCount || 0) + (result.data ? 1 : -1)
    }
  } catch (_) {}
}

const handleSharedFollow = async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  if (!sharedVideoData.value?.userId) return
  try {
    const result = await toggleFollow(sharedVideoData.value.userId)
    if (result.code === 200) {
      sharedVideoData.value.isFollowing = result.data
    }
  } catch (_) {}
}

const handleSharedComment = async () => {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  if (!sharedVideoCommentInput.value.trim() || !canInteract.value) return
  try {
    const result = await addComment(sharedVideoData.value.id, sharedVideoCommentInput.value.trim())
    if (result.code === 200) {
      sharedVideoCommentInput.value = ''
      sharedVideoData.value.commentsCount = (sharedVideoData.value.commentsCount || 0) + 1
      const commentResult = await getComments(sharedVideoData.value.id)
      if (commentResult.code === 200) {
        sharedVideoComments.value = commentResult.data
        scrollSharedCommentsToBottom()
      }
    }
  } catch (_) {}
}

const goToSharedProfile = () => {
  if (sharedVideoData.value?.userId) {
    router.push(`/profile/${sharedVideoData.value.userId}`)
  }
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const formatTimeAgo = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(diff / 3600000)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(diff / 86400000)
  return `${days}天前`
}

const sharedCommentsRef = ref(null)

const scrollSharedToComments = () => {
  nextTick(() => {
    if (sharedCommentsRef.value) {
      sharedCommentsRef.value.scrollIntoView({ behavior: 'smooth' })
    }
  })
}

const scrollSharedCommentsToBottom = () => {
  nextTick(() => {
    if (sharedCommentsRef.value) {
      const list = sharedCommentsRef.value.querySelector('.shared-comments-list')
      if (list) list.scrollTop = list.scrollHeight
    }
  })
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

.top-back-btn {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: transparent; border: none; border-radius: 50%;
  cursor: pointer; transition: background 0.2s;
  flex-shrink: 0; margin-left: 4px;
}
.top-back-btn:hover { background: rgba(255,255,255,0.1); }

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

/* AI推荐视频列表 */
.recommend-text {
  font-size: 13px;
  color: #ccc;
  margin-bottom: 8px;
  line-height: 1.5;
}
.recommend-video-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.recommend-video-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #2a2a2a;
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  transition: background 0.2s;
  max-width: 300px;
}
.recommend-video-card:hover { background: #3a3a3a; }
.recommend-video-thumb {
  width: 80px; height: 100px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  background: #1a1a1a;
}
.recommend-video-thumb img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.recommend-video-thumb svg {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%,-50%);
  filter: drop-shadow(0 1px 3px rgba(0,0,0,0.6));
}
.recommend-video-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow: hidden;
  flex: 1;
  min-width: 0;
}
.recommend-video-title {
  font-size: 13px; color: #fff;
  font-weight: 500;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.recommend-video-author {
  font-size: 11px; color: rgba(255,255,255,0.5);
}
.recommend-video-tags {
  font-size: 10px;
  color: #fe2c55;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* 分享视频卡片 */
.share-video-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #2a2a2a;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: background 0.2s;
  max-width: 280px;
}
.share-video-card:hover { background: #3a3a3a; }
.share-video-thumb {
  width: 56px; height: 56px;
  background: linear-gradient(135deg, #fe2c55, #ff6b81);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}
.share-video-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  z-index: 0;
}
.share-video-play-icon {
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 1px 3px rgba(0,0,0,0.5));
}
.share-video-info { display: flex; flex-direction: column; gap: 4px; overflow: hidden; }
.share-video-label { font-size: 13px; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.share-video-desc { font-size: 11px; color: rgba(255,255,255,0.5); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 分享视频播放器 — 完整模态体验 */
.shared-video-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.95); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
}
.shared-video-container {
  position: relative;
  width: 90%;
  max-width: 900px;
  height: 90vh;
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.shared-video-close {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.15);
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  z-index: 100;
  transition: background 0.2s;
}
.shared-video-close:hover { background: rgba(255, 255, 255, 0.3); }
.shared-video-close span { font-size: 13px; }

.shared-video-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.shared-video-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}
.shared-video-player {
  max-width: 100%;
  max-height: 100%;
}
.shared-interaction-section {
  width: 350px;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  border-left: 1px solid #333;
  overflow: hidden;
}
.shared-author-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #333;
}
.shared-follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0; margin: 0;
  position: relative;
  width: 50px; height: 50px;
  flex-shrink: 0;
}
.shared-follow-btn img {
  width: 44px; height: 44px;
  border-radius: 50%; object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.8);
  cursor: pointer;
}
.shared-follow-btn.followed img { border-color: #fe2c55; }
.shared-follow-btn svg {
  position: absolute;
  bottom: 0; right: 0;
  width: 18px; height: 18px;
  background: #fe2c55;
  border-radius: 50%;
  padding: 3px;
  border: 2px solid rgba(0, 0, 0, 0.5);
}
.shared-author-name {
  flex: 1;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}
.shared-video-info-panel {
  padding: 12px 16px;
  border-bottom: 1px solid #333;
}
.shared-video-panel-title {
  font-size: 14px; font-weight: 500; color: #fff; margin: 0 0 4px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.shared-video-panel-desc {
  font-size: 12px; color: rgba(255,255,255,0.5); margin: 0;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box;
  -webkit-line-clamp: 3; -webkit-box-orient: vertical;
}
.shared-action-bar {
  display: flex; gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #333;
}
.shared-action-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: none; border-radius: 20px;
  color: #fff; font-size: 14px; cursor: pointer;
  transition: background 0.2s;
}
.shared-action-btn:hover { background: rgba(255, 255, 255, 0.2); }
.shared-action-btn.liked { background: rgba(254, 44, 85, 0.2); }

.shared-comments-section {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
}
.shared-comments-title {
  color: #fff; font-size: 14px; font-weight: 500;
  padding: 10px 16px; margin: 0;
  border-bottom: 1px solid #333;
}
.shared-comments-list {
  flex: 1; overflow-y: auto; padding: 8px 16px;
}
.shared-comment-item {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 8px 0; border-bottom: 1px solid #2a2a2a;
}
.shared-comment-avatar {
  width: 32px; height: 32px;
  border-radius: 50%; object-fit: cover; flex-shrink: 0;
}
.shared-comment-content {
  flex: 1; display: flex; flex-direction: column; gap: 2px;
}
.shared-comment-author { color: #fff; font-size: 13px; font-weight: 500; }
.shared-comment-text { color: rgba(255,255,255,0.75); font-size: 12px; }
.shared-comment-time { color: rgba(255,255,255,0.4); font-size: 11px; flex-shrink: 0; }
.shared-no-comments { text-align: center; padding: 30px 0; color: rgba(255,255,255,0.4); font-size: 13px; }

.shared-comment-input-section {
  display: flex; gap: 8px;
  padding: 10px 16px; border-top: 1px solid #333;
}
.shared-comment-input {
  flex: 1;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid #333; border-radius: 20px;
  color: #fff; font-size: 13px; outline: none;
}
.shared-comment-input:focus { border-color: #fe2c55; }
.shared-send-btn {
  padding: 8px 20px;
  background: #fe2c55; color: #fff;
  border: none; border-radius: 20px;
  font-size: 13px; cursor: pointer;
}
.shared-send-btn:hover:not(:disabled) { background: #e0264d; }
.shared-send-btn:disabled { background: #555; cursor: not-allowed; }
</style>
