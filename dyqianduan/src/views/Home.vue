<template>
  <div class="home-container">
    <header class="home-header">
      <div class="header-left">
        <h1 class="logo">🎵 短视频</h1>
      </div>
      <div class="header-right" v-if="userStore.isLoggedIn">
        <span class="user-name">{{ userStore.user?.username }}</span>
        <router-link to="/upload" class="upload-btn">➕ 发布</router-link>
        <router-link to="/profile" class="profile-btn">个人资料</router-link>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
      <div class="header-right" v-else>
        <router-link to="/login" class="login-btn">登录</router-link>
        <router-link to="/register" class="register-btn">注册</router-link>
      </div>
    </header>

    <main class="video-feed">
      <div class="video-list">
        <div
          v-for="work in works"
          :key="work.id"
          class="video-card"
          @click="openVideo(work)"
        >
          <div class="video-thumbnail">
            <img :src="work.thumbnail || work.url" :alt="work.title" />
            <div class="play-icon">▶</div>
          </div>
          <div class="video-info">
            <h3 class="video-title">{{ work.title || '无标题' }}</h3>
            <p class="video-author">{{ work.username || '匿名用户' }}</p>
            <div class="video-stats">
              <span>👍 {{ work.likesCount || 0 }}</span>
              <span>💬 {{ work.commentsCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading" class="loading-more">加载中...</div>
      <div v-if="!loading && works.length === 0" class="empty-state">
        <p>暂无视频</p>
        <router-link v-if="userStore.isLoggedIn" to="/upload" class="upload-link">去发布第一个视频</router-link>
      </div>
    </main>

    <!-- 视频播放弹窗 -->
    <div v-if="currentVideo" class="video-modal" @click.self="closeVideo">
      <div class="modal-content">
        <button class="close-btn" @click="closeVideo">✕</button>
        <div class="modal-body">
          <div class="video-section">
            <VideoPlayer
              :src="currentVideo.url"
              :thumbnail="currentVideo.thumbnail"
              :title="currentVideo.title"
              :description="currentVideo.description"
              :authorName="currentVideo.username"
              :avatar="currentVideo.avatar"
              ref="playerRef"
            />
          </div>
          <div class="interaction-section">
            <div class="author-bar">
              <img :src="currentVideo.avatar || defaultAvatar" class="author-avatar" />
              <span class="author-name">{{ currentVideo.username || '匿名用户' }}</span>
            </div>
            <div class="action-bar">
              <button 
                class="action-btn like-btn" 
                :class="{ liked: isLiked.value }"
                @click="handleLike"
              >
                <span class="action-icon">{{ isLiked.value ? '❤️' : '🤍' }}</span>
                <span class="action-count">{{ currentVideo.likesCount || 0 }}</span>
              </button>
              <button class="action-btn comment-btn" @click="scrollToComments">
                <span class="action-icon">💬</span>
                <span class="action-count">{{ currentVideo.commentsCount || 0 }}</span>
              </button>
            </div>
            <div class="comments-section" ref="commentsSection">
              <h4 class="comments-title">评论</h4>
              <div class="comments-list">
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <img :src="comment.avatar || defaultAvatar" class="comment-avatar" />
                  <div class="comment-content">
                    <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
                    <span class="comment-text">{{ comment.content }}</span>
                  </div>
                  <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
                </div>
                <div v-if="comments.length === 0" class="no-comments">
                  <p>暂无评论，快来抢沙发吧~</p>
                </div>
              </div>
              <div class="comment-input-section">
                <input
                  type="text"
                  v-model="commentInput"
                  class="comment-input"
                  placeholder="输入评论..."
                  @keyup.enter="handleComment"
                  :disabled="!userStore.isLoggedIn"
                />
                <button class="send-btn" @click="handleComment" :disabled="!commentInput.trim() || !userStore.isLoggedIn">
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getWorks } from '../api/work'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
import VideoPlayer from '../components/VideoPlayer.vue'

const router = useRouter()
const userStore = useUserStore()

const works = ref([])
const loading = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const currentVideo = ref(null)
const playerRef = ref(null)
const comments = ref([])
const isLiked = ref(false)
const commentInput = ref('')
const commentsSection = ref(null)
const defaultAvatar = 'https://via.placeholder.com/40'

const loadWorks = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const result = await getWorks(currentPage.value, 10)
    if (result.code === 200) {
      if (result.data.length === 0) {
        hasMore.value = false
      } else {
        works.value.push(...result.data)
        currentPage.value++
      }
    }
  } catch (err) {
    console.error('加载视频失败', err)
  } finally {
    loading.value = false
  }
}

const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  if (scrollTop + windowHeight >= documentHeight - 100) {
    loadWorks()
  }
}

const openVideo = async (work) => {
  currentVideo.value = work
  document.body.style.overflow = 'hidden'
  comments.value = []
  
  try {
    const [likeResult, commentResult] = await Promise.all([
      checkIsLiked(work.id),
      getComments(work.id)
    ])
    if (likeResult.code === 200) {
      isLiked.value = likeResult.data
    }
    if (commentResult.code === 200) {
      comments.value = commentResult.data
    }
  } catch (err) {
    console.error('加载视频信息失败', err)
  }
}

const closeVideo = () => {
  if (playerRef.value) {
    playerRef.value.pause()
  }
  currentVideo.value = null
  comments.value = []
  commentInput.value = ''
  isLiked.value = false
  document.body.style.overflow = ''
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }
  
  try {
    const result = await toggleLike(currentVideo.value.id)
    if (result.code === 200) {
      isLiked.value = result.data
      currentVideo.value.likesCount += result.data ? 1 : -1
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const handleComment = async () => {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    router.push('/login')
    return
  }
  
  if (!commentInput.value.trim()) return
  
  try {
    const result = await addComment(currentVideo.value.id, commentInput.value.trim())
    if (result.code === 200) {
      commentInput.value = ''
      currentVideo.value.commentsCount++
      
      const commentResult = await getComments(currentVideo.value.id)
      if (commentResult.code === 200) {
        comments.value = commentResult.data
      }
    }
  } catch (err) {
    console.error('评论失败', err)
  }
}

const scrollToComments = () => {
  nextTick(() => {
    commentsSection.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes}分钟前`
  
  const hours = Math.floor(diff / 3600000)
  if (hours < 24) return `${hours}小时前`
  
  const days = Math.floor(diff / 86400000)
  return `${days}天前`
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadWorks()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 32px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
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
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: #333;
}

.upload-btn {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.upload-btn:hover {
  background: #5a67d8;
}

.profile-btn {
  padding: 8px 16px;
  background: #f0f0f0;
  color: #333;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.profile-btn:hover {
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
  background: #ff3344;
}

.login-btn,
.register-btn {
  padding: 8px 16px;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}

.login-btn {
  color: #667eea;
  border: 1px solid #667eea;
}

.register-btn {
  background: #667eea;
  color: white;
}

.video-feed {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.video-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.video-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
  overflow: hidden;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 50px;
  height: 50px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card:hover .play-icon {
  opacity: 1;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-author {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.video-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
}

.loading-more {
  text-align: center;
  padding: 20px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state p {
  font-size: 16px;
  margin-bottom: 12px;
}

.upload-link {
  color: #667eea;
  text-decoration: none;
}

/* 视频弹窗 */
.video-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  position: relative;
  width: 90%;
  max-width: 800px;
  height: 90vh;
  background: #1a1a1a;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 18px;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.modal-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.video-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}

.video-section :deep(.video-player-wrapper) {
  width: 100%;
  height: 100%;
}

.interaction-section {
  width: 350px;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  border-left: 1px solid #333;
}

.author-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #333;
}

.author-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.action-bar {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid #333;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.action-btn.liked {
  background: rgba(255, 100, 100, 0.2);
}

.action-icon {
  font-size: 18px;
}

.action-count {
  font-size: 14px;
}

.comments-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comments-title {
  color: white;
  font-size: 16px;
  font-weight: 500;
  padding: 12px 16px;
  margin: 0;
  border-bottom: 1px solid #333;
}

.comments-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #2a2a2a;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-author {
  color: white;
  font-size: 14px;
  font-weight: 500;
}

.comment-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.comment-time {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  flex-shrink: 0;
}

.no-comments {
  text-align: center;
  padding: 40px 0;
  color: rgba(255, 255, 255, 0.5);
}

.comment-input-section {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #333;
  background: #1a1a1a;
}

.comment-input {
  flex: 1;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid #333;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  outline: none;
}

.comment-input:focus {
  border-color: #667eea;
}

.comment-input:disabled {
  opacity: 0.5;
}

.send-btn {
  padding: 10px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:hover:not(:disabled) {
  background: #5a67d8;
}

.send-btn:disabled {
  background: #555;
  cursor: not-allowed;
}
</style>
