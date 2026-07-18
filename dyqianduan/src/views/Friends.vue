<template>
  <div class="recommend-container">
    <div class="video-wrapper" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd" @wheel="handleWheel">
      <div
        v-for="(work, index) in works"
        :key="work.id"
        class="video-slide"
        :style="{ transform: getSlideTransform(index) }"
      >
        <div class="video-content">
          <div v-if="videoLoading[index] && !videoLoaded[index]" class="video-loading">
            <div class="loading-spinner"></div>
            <span class="loading-text">加载中...</span>
          </div>
          <img 
            v-if="!videoLoaded[index]" 
            :src="work.thumbnail || work.url.replace('/videos/', '/images/').replace('.mp4', '.jpg')" 
            class="video-placeholder"
          />
          <video
          :ref="el => setVideoRef(el, index)"
          :src="getProxyUrl(work.url)"
          class="slide-video"
          loop
          playsinline
          webkit-playsinline
          x5-playsinline
          muted
          preload="auto"
          @loadstart="onVideoLoadStart(index)"
          @loadedmetadata="onVideoLoaded(index)"
          @error="onVideoError(index)"
          @play="isPlaying[index] = true"
          @pause="isPlaying[index] = false"
        ></video>

          <div class="video-overlay" @click="togglePlay(index)"></div>

          <div v-if="!isPlaying[index] && videoLoaded[index]" class="pause-icon" @click.stop="togglePlay(index)">
            <svg viewBox="0 0 24 24" width="64" height="64" fill="#fff">
              <path d="M8 5v14l11-7z"/>
            </svg>
          </div>

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
        </div>

        <div class="right-actions">
          <button class="follow-btn" :class="{ followed: work.isFollowing }" @click.stop="handleFollow(work)">
            <img :src="work.avatar || defaultAvatar" />
            <svg v-if="work.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="#fff">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
          </button>

          <div class="action-item" @click.stop="handleLike(work)">
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

          <div class="action-item" @click.stop="openComments(work)">
            <div class="action-icon-wrapper">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18zM18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
              </svg>
            </div>
            <span class="action-count">{{ formatCount(work.commentsCount) }}</span>
          </div>

          <div class="action-item share-action" @click.stop="toggleSharePopup(work)">
            <div class="action-icon-wrapper">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z"/>
              </svg>
            </div>
            <span class="action-count">分享</span>
          </div>

          <div class="share-popup-overlay" v-if="shareWork?.id === work.id" @click.stop>
            <div class="share-popup-header">分享给好友</div>
            <div class="share-popup-list">
              <div v-if="friends.length === 0" class="share-no-friends">你还没有好友，快去添加好友吧</div>
              <div v-for="f in friends" :key="f.id" class="share-friend-item" @click="shareToFriend(f, work)">
                <img :src="f.avatar || defaultAvatar" /><span>{{ f.username }}</span>
              </div>
            </div>
            <div v-if="shareOk" class="share-success">已分享 ✓</div>
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

    <div v-if="works.length === 0 && !isLoading" class="empty-state">
      <p>你的朋友很内向，还没有发表作品，去催催他们吧</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getFriendsWorks } from '../api/work'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
import { toggleFollow, checkIsFollowing, getFriends } from '../api/follow'
import { sendMessage } from '../api/message'

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const works = ref([])
const videoRefs = ref({})
const isPlaying = ref({})
const videoLoaded = ref({})
const videoLoading = ref({})
const currentIndex = ref(0)
const showComments = ref(false)
const shareWork = ref(null)
const shareOk = ref(false)
const friends = ref([])
const currentWork = ref(null)
const comments = ref([])
const commentInput = ref('')
const isLoading = ref(false)
const isAnimating = ref(false)
const offsetY = ref(0)
const isUnmounted = ref(false)

let touchStartY = 0
let touchStartX = 0
let touchCurrentY = 0
let isDragging = false
let wheelTimeout = null
let animationFrameId = null

const setVideoRef = (el, index) => {
  if (el) {
    videoRefs.value[index] = el
  }
}

const getSlideTransform = (index) => {
  const distance = (index - currentIndex.value) * window.innerHeight - offsetY.value
  return `translateY(${distance}px)`
}

const getProxyUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `/api/video/proxy?url=${encodeURIComponent(url)}`
  }
  return url
}

const loadWorks = async () => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const userId = userStore.user?.id || 1
    const result = await getFriendsWorks(userId)
    if (result.code === 200 && result.data.length > 0) {
      const newWorks = []
      for (const work of result.data) {
        const newWork = { ...work }
        newWork.userId = newWork.userId || newWork.user_id
        newWork.likesCount = newWork.likesCount || newWork.likes_count || 0
        newWork.commentsCount = newWork.commentsCount || newWork.comments_count || 0
        newWork.createdAt = newWork.createdAt || newWork.created_at
        newWork.isLiked = false
        newWork.isFollowing = true
        try {
          const likeResult = await checkIsLiked(newWork.id)
          if (likeResult.code === 200) newWork.isLiked = likeResult.data
        } catch (e) {}
        newWorks.push(newWork)
      }
      works.value.push(...newWorks)
      if (works.value.length === newWorks.length) {
        await nextTick()
        playCurrentVideo()
      }
    }
  } catch (err) {
    console.error('加载视频失败', err)
  } finally {
    isLoading.value = false
  }
}

const playCurrentVideo = async () => {
  await nextTick()
  Object.keys(videoRefs.value).forEach(key => {
    const idx = parseInt(key)
    const v = videoRefs.value[key]
    if (v && key !== currentIndex.value.toString()) {
      v.pause()
      isPlaying.value[idx] = false
    }
  })
  const video = videoRefs.value[currentIndex.value]
  if (video) {
    video.muted = true
    if (video.readyState >= 2) {
      video.play().then(() => {
        isPlaying.value[currentIndex.value] = true
        videoLoading.value[currentIndex.value] = false
      }).catch(e => {
        console.error('播放失败:', e)
        videoLoading.value[currentIndex.value] = false
      })
      isPlaying.value[currentIndex.value] = true
    } else {
      videoLoading.value[currentIndex.value] = true
      video.addEventListener('loadedmetadata', () => {
        video.play().then(() => {
          isPlaying.value[currentIndex.value] = true
          videoLoading.value[currentIndex.value] = false
        }).catch(e => {
          console.error('播放失败:', e)
          videoLoading.value[currentIndex.value] = false
        })
        isPlaying.value[currentIndex.value] = true
      }, { once: true })
      video.addEventListener('error', () => {
        videoLoading.value[currentIndex.value] = false
      }, { once: true })
    }
  }
}

const onVideoLoaded = (index) => {
  videoLoaded.value[index] = true
  videoLoading.value[index] = false
  if (index === currentIndex.value) {
    const video = videoRefs.value[index]
    if (video) {
      video.muted = true
      video.play().then(() => {
        isPlaying.value[index] = true
      }).catch(e => console.error('视频自动播放失败:', e))
      isPlaying.value[index] = true
    }
  }
}

const onVideoError = (index) => {
  videoLoading.value[index] = false
  console.error('视频加载失败:', index)
}

const onVideoLoadStart = (index) => {
  videoLoading.value[index] = true
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
  if (isAnimating.value) return
  if (wheelTimeout) clearTimeout(wheelTimeout)
  wheelTimeout = setTimeout(() => {
    if (e.deltaY > 0) {
      switchVideo(1)
    } else if (e.deltaY < 0) {
      switchVideo(-1)
    }
  }, 50)
}

const handleTouchStart = (e) => {
  if (isAnimating.value) return
  touchStartY = e.touches[0].clientY
  touchStartX = e.touches[0].clientX
  touchCurrentY = touchStartY
  isDragging = true
}

const handleTouchMove = (e) => {
  if (!isDragging || isAnimating.value) return
  touchCurrentY = e.touches[0].clientY
  const deltaY = touchCurrentY - touchStartY
  const deltaX = e.touches[0].clientX - touchStartX
  if (Math.abs(deltaX) > Math.abs(deltaY)) {
    isDragging = false
    return
  }
  offsetY.value = deltaY
}

const handleTouchEnd = (e) => {
  if (!isDragging || isAnimating.value) {
    isDragging = false
    return
  }
  isDragging = false
  const deltaY = touchCurrentY - touchStartY
  const threshold = window.innerHeight * 0.2
  if (Math.abs(deltaY) > threshold) {
    if (deltaY > 0) {
      switchVideo(1)
    } else {
      switchVideo(-1)
    }
  } else {
    animateOffsetTo(0)
  }
}

const switchVideo = (direction) => {
  if (isAnimating.value) return
  const newIndex = currentIndex.value + direction
  if (newIndex < 0 || newIndex >= works.value.length) {
    animateOffsetTo(0)
    return
  }
  isAnimating.value = true
  const targetOffset = direction * window.innerHeight
  animateOffsetTo(targetOffset, () => {
    offsetY.value = 0
    currentIndex.value = newIndex
    isAnimating.value = false
    playCurrentVideo()
  })
}

const animateOffsetTo = (targetOffset, callback) => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  const startOffset = offsetY.value
  const diff = targetOffset - startOffset
  const duration = 300
  const startTime = performance.now()
  const animate = (currentTime) => {
    if (isUnmounted.value) return
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    offsetY.value = startOffset + diff * eased
    if (progress < 1) {
      animationFrameId = requestAnimationFrame(animate)
    } else {
      animationFrameId = null
      offsetY.value = targetOffset
      if (callback) callback()
    }
  }
  animationFrameId = requestAnimationFrame(animate)
}

const handleLike = async (work) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  try {
    const result = await toggleLike(work.id)
    if (result.code === 200) {
      const idx = works.value.findIndex(w => w.id === work.id)
      if (idx !== -1) {
        works.value[idx].isLiked = result.data
        works.value[idx].likesCount += result.data ? 1 : -1
      }
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const toggleSharePopup = async (work) => {
  if (shareWork.value?.id === work.id) { shareWork.value = null; shareOk.value = false; return }
  shareWork.value = work
  shareOk.value = false
  if (userStore.user?.id) {
    try { const r = await getFriends(userStore.user.id); if (r.code === 200) friends.value = r.data || [] } catch (_) { friends.value = [] }
  }
}

const shareToFriend = async (friend, work) => {
  const title = work.title || '无标题'
  try {
    await sendMessage(friend.id, `📹 [分享视频] ${title}\n${work.url}`)
    shareOk.value = true
    setTimeout(() => { shareWork.value = null; shareOk.value = false }, 1000)
  } catch (_) {}
}

const handleFollow = async (work) => {
  if (!userStore.isLoggedIn) {
    alert('请先登录才能关注！')
    router.push('/login')
    return
  }
  const followeeId = work.userId || work.user_id
  if (!followeeId) {
    console.error('用户ID不存在', work.id)
    return
  }
  try {
    const result = await toggleFollow(followeeId)
    if (result.code === 200) {
      if (!result.data) {
        works.value = works.value.filter(w => w.userId !== work.userId)
        if (currentIndex.value >= works.value.length) {
          currentIndex.value = Math.max(0, works.value.length - 1)
        }
      }
    }
  } catch (err) {
    console.error('关注失败', err)
    alert('关注失败，请重试')
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
      const idx = works.value.findIndex(w => w.id === currentWork.value.id)
      if (idx !== -1) {
        works.value[idx].commentsCount++
      }
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
})

onUnmounted(() => {
  isUnmounted.value = true
  if (wheelTimeout) clearTimeout(wheelTimeout)
  if (animationFrameId) cancelAnimationFrame(animationFrameId)
  Object.values(videoRefs.value).forEach(v => {
    if (v) {
      v.pause()
      v.src = ''
    }
  })
})
</script>

<style scoped>
.recommend-container {
  height: 100%;
  overflow: hidden;
  background: #000;
  position: relative;
}

.video-wrapper {
  height: 100%;
  overflow: visible;
  position: relative;
  touch-action: none;
}

.video-slide {
  height: 100%;
  width: 100%;
  position: relative;
  background: #000;
}

.video-content {
  height: 100%;
  width: calc(100% - 80px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.slide-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
  pointer-events: none;
}

.video-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 2;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 80px;
  bottom: 0;
  z-index: 3;
  cursor: pointer;
  background: transparent;
  pointer-events: auto;
}

.pause-icon {
  position: absolute;
  top: 50%;
  left: calc(50% + 5px);
  transform: translate(-50%, -50%);
  z-index: 5;
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;
  width: 64px;
  height: 64px;
  pointer-events: none;
  background: transparent;
}

.pause-icon svg {
  pointer-events: auto;
  width: 100%;
  height: 100%;
}

.pause-icon:hover {
  opacity: 1;
}

.video-bottom-info {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 80px;
  z-index: 10;
  pointer-events: none;
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

.right-actions {
  position: absolute;
  right: 16px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  z-index: 10;
  pointer-events: auto;
  width: 50px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.follow-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  margin: 0;
  position: relative;
  width: 50px;
  height: 60px;
  z-index: 100000;
}

.follow-btn img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.8);
}

.follow-btn.followed img {
  border-color: #fe2c55;
}

.follow-btn svg {
  margin-top: -6px;
  width: 20px;
  height: 20px;
  background: #fe2c55;
  border-radius: 50%;
  padding: 3px;
  border: 2px solid rgba(0, 0, 0, 0.5);
}

.follow-btn.followed svg {
  background: #2ecc71;
}

.follow-btn:hover {
  transform: scale(1.1);
}

.author-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.comment-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
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

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #666;
}

.empty-state p {
  font-size: 18px;
  margin: 0;
}
</style>