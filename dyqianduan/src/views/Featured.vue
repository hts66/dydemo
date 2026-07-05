<template>
  <div class="featured-container">
    <!-- 视频网格 -->
    <div class="video-grid">
      <div 
        v-for="work in filteredWorks" 
        :key="work.id"
        class="video-card"
        @click="openVideo(work)"
      >
        <div class="video-thumbnail">
          <img 
            :src="work.thumbnail || work.url" 
            :alt="work.title" 
            @error="handleThumbnailError($event, work)"
          />
          <div class="video-duration">{{ formatDuration(work.duration) }}</div>
          <div class="play-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
              <path d="M8 5v14l11-7z"/>
            </svg>
          </div>
        </div>
        <div class="video-info">
          <h3 class="video-title">{{ work.title || '无标题' }}</h3>
          <p class="video-description" v-if="work.description">{{ work.description }}</p>
          <div class="video-meta">
            <div class="author-section">
              <button 
                class="author-follow-btn"
                :class="{ followed: work.isFollowing }"
                @click.stop="handleFollow(work)"
              >
                <img :src="work.avatar || defaultAvatar" @click.stop="goToProfile(work.userId)" />
                <svg v-if="!work.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
              <span class="video-author">{{ work.username || '匿名用户' }}</span>
            </div>
            <div class="stats-section">
              <span class="video-stat">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
                {{ formatCount(work.likesCount) }}
              </span>
              <span class="video-stat">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                  <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>
                </svg>
                {{ formatCount(work.commentsCount) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 视频播放弹窗 -->
    <div v-if="currentVideo" class="video-modal" @click.self="closeVideo">
      <div class="modal-content">
        <button class="close-btn" @click="closeVideo">✕</button>
        <div class="modal-body">
          <div class="video-section">
            <video
              ref="videoPlayer"
              :src="getProxyUrl(currentVideo.url)"
              :poster="currentVideo.thumbnail"
              class="modal-video"
              controls
              autoplay
              loop
              muted
            ></video>
          </div>
          <div class="interaction-section">
            <div class="author-bar">
              <button 
                class="follow-btn" 
                :class="{ followed: currentVideo.isFollowing }"
                @click.stop="handleFollow"
              >
                <img :src="currentVideo.avatar || defaultAvatar" @click.stop="goToProfile" />
                <svg v-if="!currentVideo.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                  <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
                </svg>
              </button>
              <span class="author-name">{{ currentVideo.username || '匿名用户' }}</span>
            </div>
            <div class="action-bar">
              <button 
                class="action-btn like-btn" 
                :class="{ liked: currentVideo.isLiked }"
                @click="handleLike"
              >
                <svg v-if="currentVideo.isLiked" viewBox="0 0 24 24" width="20" height="20" fill="#fe2c55">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                  <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3z"/>
                </svg>
                <span>{{ currentVideo.likesCount || 0 }}</span>
              </button>
              <button class="action-btn" @click="scrollToComments">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                  <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>
                </svg>
                <span>{{ currentVideo.commentsCount || 0 }}</span>
              </button>
            </div>
            <div class="comments-section" ref="commentsSection">
              <h4 class="comments-title">评论</h4>
              <div class="comments-list">
                <div v-for="comment in comments" :key="comment.id" class="comment-item">
                  <img :src="comment.avatar || defaultAvatar" class="comment-avatar" @click.stop="goToProfile(comment.userId)" />
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getWorks } from '../api/work'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
import { toggleFollow, checkIsFollowing } from '../api/follow'

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const getProxyUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `/api/video/proxy?url=${encodeURIComponent(url)}`
  }
  return url
}


const works = ref([])
const currentVideo = ref(null)
const comments = ref([])
const commentInput = ref('')
const videoPlayer = ref(null)
const commentsSection = ref(null)

const filteredWorks = computed(() => works.value)

const loadWorks = async () => {
  try {
    const result = await getWorks(1, 50)
    if (result.code === 200) {
      for (const work of result.data) {
        work.isLiked = false
        work.isFollowing = false
        try {
          const likeResult = await checkIsLiked(work.id)
          if (likeResult.code === 200) work.isLiked = likeResult.data
        } catch (e) {}
        if (userStore.isLoggedIn && work.userId) {
          try {
            const followResult = await checkIsFollowing(work.userId)
            if (followResult.code === 200) work.isFollowing = followResult.data
          } catch (e) {}
        }
      }
      works.value = result.data
    }
  } catch (err) {
    console.error('加载视频失败', err)
  }
}

const handleThumbnailError = (event, work) => {
  const img = event.target
  if (work.url) {
    img.src = `/api/video/proxy?url=${encodeURIComponent(work.url)}`
  } else {
    img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%232a2a2a" width="400" height="300"/%3E%3Ctext fill="%23666" font-family="sans-serif" font-size="14" x="50%25" y="50%25" text-anchor="middle" dominant-baseline="middle"%3E视频加载失败%3C/text%3E%3C/svg%3E'
  }
}

onMounted(async () => {
  await loadWorks()
})

const openVideo = async (work) => {
  currentVideo.value = work
  document.body.style.overflow = 'hidden'
  comments.value = []
  
  try {
    const [likeResult, commentResult, followResult] = await Promise.all([
      checkIsLiked(work.id),
      getComments(work.id),
      userStore.isLoggedIn ? checkIsFollowing(work.userId) : Promise.resolve({ code: 200, data: false })
    ])
    if (likeResult.code === 200) {
      currentVideo.value.isLiked = likeResult.data
    }
    if (commentResult.code === 200) {
      comments.value = commentResult.data
    }
    if (followResult.code === 200) {
      currentVideo.value.isFollowing = followResult.data
    }
  } catch (err) {
    console.error('加载视频信息失败', err)
  }
  
  await nextTick()
  
  if (videoPlayer.value) {
    videoPlayer.value.currentTime = 0
    videoPlayer.value.muted = true
    videoPlayer.value.play().catch(e => console.error('播放失败', e))
  }
}

const closeVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
  }
  currentVideo.value = null
  comments.value = []
  commentInput.value = ''
  document.body.style.overflow = ''
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  try {
    const result = await toggleLike(currentVideo.value.id)
    if (result.code === 200) {
      currentVideo.value.isLiked = result.data
      currentVideo.value.likesCount += result.data ? 1 : -1
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const handleFollow = async (work) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  const targetWork = work || currentVideo.value
  if (!targetWork) return
  
  try {
    const result = await toggleFollow(targetWork.userId)
    if (result.code === 200) {
      targetWork.isFollowing = result.data
      if (work) {
        const idx = filteredWorks.value.findIndex(w => w.id === work.id)
        if (idx !== -1) {
          filteredWorks.value[idx].isFollowing = result.data
        }
      }
    }
  } catch (err) {
    console.error('关注失败', err)
  }
}

const goToProfile = (userId) => {
  const targetUserId = userId || (currentVideo.value && currentVideo.value.userId)
  if (targetUserId) {
    router.push(`/profile/${targetUserId}`)
  }
}

const handleComment = async () => {
  if (!userStore.isLoggedIn) {
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

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const formatDuration = (seconds) => {
  if (!seconds) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
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

</script>

<style scoped>
.featured-container {
  height: 100%;
  overflow-y: auto;
  background: #1a1a1a;
}

/* 视频网格 */
.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  padding: 20px;
}

.video-card {
  background: #2a2a2a;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.video-card:hover {
  transform: translateY(-4px);
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

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.video-card:hover .play-icon {
  opacity: 1;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.video-description {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.video-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #888;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  margin: 0;
  position: relative;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.author-follow-btn img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.6);
  cursor: pointer;
}

.author-follow-btn.followed img {
  border-color: #fe2c55;
}

.author-follow-btn svg {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background: #fe2c55;
  border-radius: 50%;
  padding: 2px;
  border: 1px solid rgba(0, 0, 0, 0.5);
}

.video-author {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.video-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 视频弹窗 */
.video-modal {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
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

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  z-index: 10;
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

.modal-video {
  max-width: 100%;
  max-height: 100%;
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

.follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  margin: 0;
  position: relative;
  width: 50px;
  height: 50px;
  flex-shrink: 0;
}

.follow-btn img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.8);
  cursor: pointer;
}

.follow-btn.followed img {
  border-color: #fe2c55;
}

.follow-btn svg {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: #fe2c55;
  border-radius: 50%;
  padding: 3px;
  border: 2px solid rgba(0, 0, 0, 0.5);
}

.follow-btn:hover {
  transform: scale(1.1);
}

.author-name {
  flex: 1;
  color: #fff;
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
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.action-btn.liked {
  background: rgba(254, 44, 85, 0.2);
}

.comments-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comments-title {
  color: #fff;
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

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}

.comment-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-author {
  color: #fff;
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
}

.comment-input {
  flex: 1;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid #333;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.comment-input:focus {
  border-color: #fe2c55;
}

.send-btn {
  padding: 10px 24px;
  background: #fe2c55;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:hover:not(:disabled) {
  background: #e0264d;
}

.send-btn:disabled {
  background: #555;
  cursor: not-allowed;
}
</style>
