<template>
  <div class="recommend-container">
    <div class="video-scroll" ref="scrollContainer" @wheel="handleWheel" @touchstart="handleTouchStart" @touchend="handleTouchEnd">
      <div
        v-for="(work, index) in works"
        :key="work.id"
        class="video-slide"
        :class="{ active: currentIndex === index }"
      >
        <video
          :ref="el => setVideoRef(el, index)"
          :src="work.url"
          :poster="work.thumbnail"
          class="slide-video"
          loop
          playsinline
          webkit-playsinline
          x5-playsinline
          @click="togglePlay(index)"
        ></video>

        <!-- 播放按钮覆盖层 -->
        <div v-if="!isPlaying[index]" class="play-overlay" @click="togglePlay(index)">
          <svg viewBox="0 0 24 24" width="64" height="64" fill="rgba(255,255,255,0.8)">
            <path d="M8 5v14l11-7z"/>
          </svg>
        </div>

        <!-- 底部信息 -->
        <div class="video-bottom-info">
          <div class="author-info">
            <span class="author-name">@{{ work.username || '匿名用户' }}</span>
            <span class="video-date">{{ formatDate(work.createdAt) }}</span>
          </div>
          <p class="video-desc">{{ work.description || work.title || '' }}</p>
          <div class="video-tags">
            <span v-for="tag in getTags(work.description)" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- 右侧操作栏 -->
        <div class="right-actions">
          <div class="action-item" @click="handleLike(work)">
            <div class="action-icon-wrapper">
              <svg v-if="work.isLiked" viewBox="0 0 24 24" width="32" height="32" fill="#fe2c55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/>
              </svg>
            </div>
            <span class="action-count">{{ formatCount(work.likesCount) }}</span>
          </div>

          <div class="action-item" @click="openComments(work)">
            <div class="action-icon-wrapper">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18zM18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
              </svg>
            </div>
            <span class="action-count">{{ formatCount(work.commentsCount) }}</span>
          </div>

          <div class="action-item">
            <div class="action-icon-wrapper">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
            </div>
            <span class="action-count">收藏</span>
          </div>

          <div class="action-item">
            <div class="action-icon-wrapper">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/>
              </svg>
            </div>
            <span class="action-count">分享</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 评论弹窗 -->
    <div v-if="showComments" class="comment-modal" @click.self="showComments = false">
      <div class="comment-modal-content">
        <div class="comment-header">
          <h3>评论 {{ currentWork?.commentsCount || 0 }}</h3>
          <button class="close-btn" @click="showComments = false">✕</button>
        </div>
        <div class="comment-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <img :src="comment.avatar || defaultAvatar" class="comment-avatar" />
            <div class="comment-body">
              <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
              <span class="comment-text">{{ comment.content }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
          </div>
          <div v-if="comments.length === 0" class="no-comments">
            <p>暂无评论，快来抢沙发吧~</p>
          </div>
        </div>
        <div class="comment-input-bar">
          <input
            type="text"
            v-model="commentInput"
            placeholder="发一条友好的评论吧"
            @keyup.enter="sendComment"
            :disabled="!userStore.isLoggedIn"
          />
          <button class="send-btn" @click="sendComment" :disabled="!commentInput.trim()">发送</button>
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

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const works = ref([])
const currentIndex = ref(0)
const isPlaying = ref({})
const videoRefs = ref({})
const scrollContainer = ref(null)
const showComments = ref(false)
const currentWork = ref(null)
const comments = ref([])
const commentInput = ref('')
const isLoading = ref(false)
let touchStartY = 0

const setVideoRef = (el, index) => {
  if (el) videoRefs.value[index] = el
}

const loadWorks = async () => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const page = Math.ceil(works.value.length / 10) + 1
    const result = await getWorks(page, 10)
    if (result.code === 200 && result.data.length > 0) {
      for (const work of result.data) {
        work.isLiked = false
        try {
          const likeResult = await checkIsLiked(work.id)
          if (likeResult.code === 200) work.isLiked = likeResult.data
        } catch (e) {}
      }
      works.value.push(...result.data)
    }
  } catch (err) {
    console.error('加载视频失败', err)
  } finally {
    isLoading.value = false
  }
}

const playCurrentVideo = async () => {
  await nextTick()
  // 暂停所有视频
  Object.values(videoRefs.value).forEach(v => {
    if (v) { v.pause(); v.currentTime = 0 }
  })
  // 播放当前视频
  const video = videoRefs.value[currentIndex.value]
  if (video) {
    try {
      video.muted = true
      await video.play()
      isPlaying.value[currentIndex.value] = true
    } catch (e) {
      isPlaying.value[currentIndex.value] = false
    }
  }
}

const togglePlay = (index) => {
  const video = videoRefs.value[index]
  if (!video) return
  if (video.paused) {
    video.play()
    isPlaying.value[index] = true
  } else {
    video.pause()
    isPlaying.value[index] = false
  }
}

const handleWheel = (e) => {
  e.preventDefault()
  if (e.deltaY > 0 && currentIndex.value < works.value.length - 1) {
    currentIndex.value++
    playCurrentVideo()
  } else if (e.deltaY < 0 && currentIndex.value > 0) {
    currentIndex.value--
    playCurrentVideo()
  }
  // 加载更多
  if (currentIndex.value >= works.value.length - 2) {
    loadWorks()
  }
}

const handleTouchStart = (e) => {
  touchStartY = e.touches[0].clientY
}

const handleTouchEnd = (e) => {
  const diff = touchStartY - e.changedTouches[0].clientY
  if (Math.abs(diff) > 50) {
    if (diff > 0 && currentIndex.value < works.value.length - 1) {
      currentIndex.value++
    } else if (diff < 0 && currentIndex.value > 0) {
      currentIndex.value--
    }
    playCurrentVideo()
  }
}

const handleLike = async (work) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const result = await toggleLike(work.id)
    if (result.code === 200) {
      work.isLiked = result.data
      work.likesCount += result.data ? 1 : -1
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const openComments = async (work) => {
  currentWork.value = work
  showComments.value = true
  try {
    const result = await getComments(work.id)
    if (result.code === 200) comments.value = result.data
  } catch (err) {
    console.error('获取评论失败', err)
  }
}

const sendComment = async () => {
  if (!userStore.isLoggedIn || !commentInput.value.trim()) return
  try {
    const result = await addComment(currentWork.value.id, commentInput.value.trim())
    if (result.code === 200) {
      commentInput.value = ''
      currentWork.value.commentsCount++
      const commentResult = await getComments(currentWork.value.id)
      if (commentResult.code === 200) comments.value = commentResult.data
    }
  } catch (err) {
    console.error('评论失败', err)
  }
}

const getTags = (desc) => {
  if (!desc) return []
  return desc.match(/#[^\s#]+/g) || []
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
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

onMounted(async () => {
  await loadWorks()
  await playCurrentVideo()
})

onUnmounted(() => {
  Object.values(videoRefs.value).forEach(v => {
    if (v) v.pause()
  })
})
</script>

<style scoped>
.recommend-container {
  height: 100%;
  overflow: hidden;
  background: #000;
}

.video-scroll {
  height: 100%;
  overflow: hidden;
  scroll-snap-type: y mandatory;
}

.video-slide {
  height: 100%;
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  scroll-snap-align: start;
}

.slide-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
}

.play-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 5;
  cursor: pointer;
}

/* 底部信息 */
.video-bottom-info {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 80px;
  z-index: 10;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.author-name {
  font-size: 16px;
  font-weight: bold;
  color: #fff;
}

.video-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.video-desc {
  font-size: 14px;
  color: #fff;
  line-height: 1.5;
  margin-bottom: 8px;
}

.video-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 13px;
  color: #6ea8fe;
}

/* 右侧操作栏 */
.right-actions {
  position: absolute;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  z-index: 10;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.action-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: transform 0.2s;
}

.action-item:hover .action-icon-wrapper {
  transform: scale(1.1);
}

.action-count {
  font-size: 12px;
  color: #fff;
}

/* 评论弹窗 */
.comment-modal {
  position: fixed;
  bottom: 0;
  left: 200px;
  right: 0;
  height: 60vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.comment-modal-content {
  width: 600px;
  max-width: 90%;
  height: 100%;
  background: #2a2a2a;
  border-radius: 12px 12px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #3a3a3a;
}

.comment-header h3 {
  font-size: 16px;
  color: #fff;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #3a3a3a;
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.comment-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #3a3a3a;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-author {
  font-size: 13px;
  color: #aaa;
}

.comment-text {
  font-size: 14px;
  color: #fff;
}

.comment-time {
  font-size: 12px;
  color: #666;
}

.no-comments {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.comment-input-bar {
  display: flex;
  gap: 10px;
  padding: 12px 20px;
  border-top: 1px solid #3a3a3a;
  background: #2a2a2a;
}

.comment-input-bar input {
  flex: 1;
  padding: 10px 16px;
  background: #3a3a3a;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.comment-input-bar input::placeholder {
  color: #888;
}

.send-btn {
  padding: 10px 20px;
  background: #fe2c55;
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.send-btn:disabled {
  background: #555;
  cursor: not-allowed;
}
</style>
