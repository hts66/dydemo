<template>
  <div class="slide-list">
    <SlideVerticalInfinite
      ref="slideRef"
      :list="works"
      :active="currentActiveIndex"
      unique-id="id"
      :virtual-total="5"
      :has-more="hasMore"
      :is-loading-more="isLoading && works.length > 0"
      :is-refreshing="isRefreshing"
      @update:index="onIndexChange"
      @load-more="loadMore"
      @refresh="() => refreshData()"
    >
      <template #default="{ item, index, isActive }">
        <div class="video-slide">
          <div class="video-content">
            <BaseVideo
              :item="item"
              :is-play="isActive"
            />

            <!-- Bottom info: author avatar + name + title -->
            <div class="video-bottom-info">
              <div class="author-info" @click.stop="goToProfile(item)">
                <img
                  :src="item.avatar || defaultAvatar"
                  class="author-avatar"
                  @error="e => e.target.src = defaultAvatar"
                />
                <div class="author-text">
                  <span class="author-name">@{{ item.username || '匿名用户' }}</span>
                  <span class="video-date">{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
              <p class="video-desc">{{ item.title || item.description || '' }}</p>
            </div>
          </div>

          <!-- Right actions bar -->
          <div class="right-actions">
            <!-- Follow button (hide for own videos) -->
            <button
              v-if="!isOwnVideo(item)"
              class="follow-btn"
              :class="{ followed: item.isFollowing }"
              @click.stop="handleFollow(item)"
            >
              <img :src="item.avatar || defaultAvatar" alt="" />
              <svg v-if="item.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
            </button>

            <!-- Like -->
            <div class="action-item" @click.stop="handleLike(item)">
              <div class="action-icon-wrapper">
                <svg v-if="item.isLiked" viewBox="0 0 24 24" width="32" height="32" fill="#fe2c55">
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                  <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z"/>
                </svg>
              </div>
              <span class="action-count">{{ formatCount(item.likesCount) }}</span>
            </div>

            <!-- Comment -->
            <div class="action-item" @click.stop="openComments(item)">
              <div class="action-icon-wrapper">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                  <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18zM18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/>
                </svg>
              </div>
              <span class="action-count">{{ formatCount(item.commentsCount) }}</span>
            </div>

            <!-- Share -->
            <div class="action-item" @click.stop="handleShare(item)">
              <div class="action-icon-wrapper">
                <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                  <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/>
                </svg>
              </div>
              <span class="action-count">分享</span>
            </div>
          </div>
        </div>
      </template>
    </SlideVerticalInfinite>

    <!-- Comment modal -->
    <Teleport to="body">
      <div v-if="showComments" class="comment-overlay" @click.self="showComments = false">
        <div class="comment-modal">
          <div class="comment-header">
            <h3>评论 {{ currentWork?.commentsCount || 0 }}</h3>
            <button class="close-btn" @click="showComments = false">✕</button>
          </div>
          <div class="comment-list" ref="commentListRef">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <img :src="comment.avatar || defaultAvatar" class="comment-avatar" alt="" />
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
    </Teleport>

    <!-- Share modal -->
    <Teleport to="body">
      <div v-if="showShare" class="share-overlay" @click.self="showShare = false">
        <div class="share-modal">
          <h3>分享到</h3>
          <div class="share-options">
            <div class="share-option" @click="copyLink">
              <div class="share-icon">🔗</div>
              <span>复制链接</span>
            </div>
            <div class="share-option">
              <div class="share-icon">💬</div>
              <span>微信</span>
            </div>
            <div class="share-option">
              <div class="share-icon">🐦</div>
              <span>微博</span>
            </div>
            <div class="share-option">
              <div class="share-icon">📱</div>
              <span>朋友圈</span>
            </div>
          </div>
          <button class="cancel-share-btn" @click="showShare = false">取消</button>
        </div>
      </div>
    </Teleport>

    <!-- Empty / Initial loading state -->
    <div v-if="isLoading && works.length === 0" class="initial-loading">
      <div class="loading-spinner"></div>
      <p>正在为你挑选精彩内容...</p>
    </div>
    <div v-else-if="!isLoading && works.length === 0" class="empty-state">
      <p>暂无推荐内容</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getRecommendWorks, recordWatchHistory } from '../api/work'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
import { toggleFollow, checkIsFollowing } from '../api/follow'
import SlideVerticalInfinite from './SlideVerticalInfinite.vue'
import BaseVideo from './BaseVideo.vue'
import { on, off, EVENT_KEY } from '../utils/bus'

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const slideRef = ref(null)
const commentListRef = ref(null)

// ── Data state ────────────────────────────────────────
const works = ref([])
const currentActiveIndex = ref(0)
const isLoading = ref(false)
const isRefreshing = ref(false)
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 10

// ── Comment state ─────────────────────────────────────
const showComments = ref(false)
const currentWork = ref(null)
const comments = ref([])
const commentInput = ref('')

// ── Share state ───────────────────────────────────────
const showShare = ref(false)

// ── API calls ─────────────────────────────────────────
const getData = async (refresh = false) => {
  if (refresh) {
    currentPage.value = 1
    works.value = []
    hasMore.value = true
  }
  await loadWorks()
}

const refreshData = async () => {
  isRefreshing.value = true
  currentPage.value = 1
  hasMore.value = true
  const oldWorks = [...works.value]

  try {
    const result = await getRecommendWorks(1, pageSize)

    if (result.code === 200 && result.data.length > 0) {
      const newWorks = await enrichWorks(result.data)
      works.value = newWorks
      currentActiveIndex.value = 0
      currentPage.value = 2

      if (result.data.length < pageSize) {
        hasMore.value = false
      }
    }
  } catch (err) {
    console.error('刷新推荐视频失败', err)
  } finally {
    isRefreshing.value = false
  }
}

const loadWorks = async () => {
  if (isLoading.value || !hasMore.value) return
  isLoading.value = true

  try {
    const result = await getRecommendWorks(currentPage.value, pageSize)

    if (result.code === 200 && result.data.length > 0) {
      const enriched = await enrichWorks(result.data)
      works.value.push(...enriched)
      currentPage.value++

      if (result.data.length < pageSize) {
        hasMore.value = false
      }
    } else {
      hasMore.value = false
    }
  } catch (err) {
    console.error('加载推荐视频失败', err)
  } finally {
    isLoading.value = false
  }
}

// Enrich works with like/follow status
const enrichWorks = async (data) => {
  const enriched = []

  for (const work of data) {
    const newWork = { ...work }
    newWork.userId = newWork.userId || newWork.user_id
    newWork.likesCount = newWork.likesCount || newWork.likes_count || 0
    newWork.commentsCount = newWork.commentsCount || newWork.comments_count || 0
    newWork.createdAt = newWork.createdAt || newWork.created_at
    newWork.isLiked = false
    newWork.isFollowing = false

    if (userStore.isLoggedIn) {
      try {
        const [likeResult, followResult] = await Promise.all([
          checkIsLiked(newWork.id),
          newWork.userId ? checkIsFollowing(newWork.userId) : Promise.resolve(null)
        ])
        if (likeResult && likeResult.code === 200) newWork.isLiked = likeResult.data
        if (followResult && followResult.code === 200) newWork.isFollowing = followResult.data
      } catch (_e) {
        // ignore individual status fetch errors
      }
    }

    enriched.push(newWork)
  }

  return enriched
}

const loadMore = () => {
  loadWorks()
}

// ── Index change handler ──────────────────────────────
const onIndexChange = (newIndex) => {
  currentActiveIndex.value = newIndex
}

// ── Like ──────────────────────────────────────────────
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

// ── Follow ────────────────────────────────────────────
const handleFollow = async (work) => {
  if (!userStore.isLoggedIn) {
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
      works.value.forEach(w => {
        if (w.userId === followeeId) w.isFollowing = result.data
      })
    }
  } catch (err) {
    console.error('关注失败', err)
  }
}

// ── Comments ──────────────────────────────────────────
const openComments = async (work) => {
  currentWork.value = work
  showComments.value = true
  commentInput.value = ''

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

      // Update local count
      const idx = works.value.findIndex(w => w.id === currentWork.value.id)
      if (idx !== -1) {
        works.value[idx].commentsCount++
      }

      // Refresh comments
      const commentResult = await getComments(currentWork.value.id)
      if (commentResult.code === 200) comments.value = commentResult.data
    }
  } catch (err) {
    console.error('评论失败', err)
  }
}

// ── Share ─────────────────────────────────────────────
const handleShare = (work) => {
  currentWork.value = work
  showShare.value = true
}

const copyLink = () => {
  const url = `${window.location.origin}/video/${currentWork.value?.id}`
  navigator.clipboard.writeText(url).then(() => {
    alert('链接已复制！')
    showShare.value = false
  }).catch(() => {
    alert('复制失败，请手动复制')
  })
}

// ── Navigation ────────────────────────────────────────
const goToProfile = (item) => {
  const userId = item.userId || item.user_id
  if (userId) {
    router.push(`/profile/${userId}`)
  }
}

// ── Utilities ─────────────────────────────────────────
const isOwnVideo = (item) => {
  if (!userStore.isLoggedIn || !userStore.user) return false
  const userId = item.userId || item.user_id
  return userId === userStore.user.id
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
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`

  const hours = Math.floor(diff / 3600000)
  if (hours < 24) return `${hours}小时前`

  const days = Math.floor(diff / 86400000)
  if (days < 30) return `${days}天前`

  return formatDate(dateStr)
}

// ── Event bus: record watch history ───────────────────
const handleCurrentItem = (data) => {
  if (data.item && userStore.isLoggedIn) {
    recordWatchHistory({ workId: data.item.id, watchDuration: 0, isComplete: false }).catch(() => {})
  }
}

onMounted(() => {
  on(EVENT_KEY.CURRENT_ITEM, handleCurrentItem)
  getData()
})

onUnmounted(() => {
  off(EVENT_KEY.CURRENT_ITEM, handleCurrentItem)
})

defineExpose({
  works,
  getData,
  refreshData
})
</script>

<style scoped>
.slide-list {
  height: 100%;
  overflow: hidden;
  background: #000;
  position: relative;
}

/* ── Video slide ───────────────────────────────────── */
.video-slide {
  height: 100%;
  width: 100%;
  position: relative;
  background: #000;
}

.video-content {
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
}

/* ── Right actions bar ─────────────────────────────── */
.right-actions {
  position: absolute;
  right: 16px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
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
  -webkit-tap-highlight-color: transparent;
}

/* Follow button - avatar style */
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

.action-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: transform 0.2s;
}

.action-item:active .action-icon-wrapper {
  transform: scale(0.9);
}

.action-count {
  font-size: 12px;
  color: #fff;
  font-weight: 500;
}

/* ── Bottom info ───────────────────────────────────── */
.video-bottom-info {
  position: absolute;
  bottom: 80px;
  left: 20px;
  right: 80px;
  z-index: 10;
  pointer-events: auto;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  cursor: pointer;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
}

.author-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.author-name {
  font-size: 15px;
  font-weight: bold;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.video-desc {
  font-size: 14px;
  color: #fff;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── Comment modal ─────────────────────────────────── */
.comment-overlay {
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

.comment-modal {
  width: 600px;
  max-width: 100%;
  max-height: 75vh;
  background: #1a1a1a;
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #2a2a2a;
}

.comment-header h3 {
  font-size: 16px;
  color: #fff;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #2a2a2a;
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
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
  border-bottom: 1px solid #2a2a2a;
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
  line-height: 1.5;
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
  border-top: 1px solid #2a2a2a;
  background: #1a1a1a;
}

.comment-input-bar input {
  flex: 1;
  padding: 10px 16px;
  background: #2a2a2a;
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
  white-space: nowrap;
}

.send-btn:disabled {
  background: #555;
  cursor: not-allowed;
}

/* ── Share modal ───────────────────────────────────── */
.share-overlay {
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

.share-modal {
  width: 400px;
  max-width: 100%;
  background: #1a1a1a;
  border-radius: 16px 16px 0 0;
  padding: 24px;
  text-align: center;
}

.share-modal h3 {
  font-size: 16px;
  color: #fff;
  margin: 0 0 20px;
}

.share-options {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
}

.share-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
}

.share-icon {
  width: 48px;
  height: 48px;
  background: #2a2a2a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.share-option span {
  font-size: 12px;
  color: #ccc;
}

.cancel-share-btn {
  width: 100%;
  padding: 12px;
  background: #2a2a2a;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}

/* ── States ────────────────────────────────────────── */
.initial-loading,
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #888;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.initial-loading p,
.empty-state p {
  font-size: 18px;
  margin: 0;
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
</style>
