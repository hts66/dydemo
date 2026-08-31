<template>
  <div class="featured-container" @scroll="handleScroll">
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
            :src="mediaUrl(work.thumbnail || work.url)" 
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
                v-if="!isOwnVideo(work)"
                class="author-follow-btn"
                :class="{ followed: work.isFollowing }"
                @click.stop="handleFollow(work)"
              >
                <img :src="mediaUrl(work.avatar) || defaultAvatar" @click.stop="goToProfile(work.userId)" />
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
      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>
    </div>

    <!-- 视频播放弹窗（可复用组件） -->
    <VideoPlayerModal
      :visible="showVideoModal"
      :work="currentVideo"
      @close="closeVideo"
      @work-updated="handleWorkUpdated"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getWorks } from '../api/work'
import { toggleFollow, checkIsFollowing } from '../api/follow'
import VideoPlayerModal from '../components/VideoPlayerModal.vue'
import { mediaUrl } from '../utils/media'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const works = ref([])
const currentVideo = ref(null)
const showVideoModal = ref(false)

// 分页懒加载 + 随机种子
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const randomSeed = ref(Math.floor(Math.random() * 2147483647))

const filteredWorks = computed(() => works.value)

const loadWorks = async (reset = false) => {
  if (loading.value) return
  if (reset) {
    page.value = 1
    hasMore.value = true
    works.value = []
    randomSeed.value = Math.floor(Math.random() * 2147483647)
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const result = await getWorks(page.value, 12, true, randomSeed.value)
    if (result.code === 200) {
      if (result.data.length === 0) {
        hasMore.value = false
      } else {
        for (const work of result.data) {
          work.isLiked = false
          work.isFollowing = false
          if (userStore.isLoggedIn && work.userId) {
            try {
              const followResult = await checkIsFollowing(work.userId)
              if (followResult.code === 200) work.isFollowing = followResult.data
            } catch (e) {}
          }
        }
        works.value.push(...result.data)
        page.value++
      }
    }
  } catch (err) {
    console.error('加载视频失败', err)
  } finally {
    loading.value = false
  }
}

const handleScroll = (event) => {
  const { scrollTop, scrollHeight, clientHeight } = event.target
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    loadWorks()
  }
}

const handleThumbnailError = (event, work) => {
  const img = event.target
  if (work.url) {
    img.src = mediaUrl(work.url)
  } else {
    img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300"%3E%3Crect fill="%232a2a2a" width="400" height="300"/%3E%3Ctext fill="%23666" font-family="sans-serif" font-size="14" x="50%25" y="50%25" text-anchor="middle" dominant-baseline="middle"%3E视频加载失败%3C/text%3E%3C/svg%3E'
  }
}

onMounted(async () => {
  await loadWorks()
})

const openVideo = async (work) => {
  currentVideo.value = { ...work }
  showVideoModal.value = true
}

const closeVideo = () => {
  showVideoModal.value = false
  currentVideo.value = null
}

const handleWorkUpdated = (updatedWork) => {
  const idx = works.value.findIndex(w => w.id === updatedWork.id)
  if (idx !== -1) {
    works.value[idx] = { ...works.value[idx], ...updatedWork }
  }
}

const handleFollow = async (work) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (!work) return
  
  try {
    const result = await toggleFollow(work.userId)
    if (result.code === 200) {
      work.isFollowing = result.data
      const idx = filteredWorks.value.findIndex(w => w.id === work.id)
      if (idx !== -1) {
        filteredWorks.value[idx].isFollowing = result.data
      }
    }
  } catch (err) {
    console.error('关注失败', err)
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

const isOwnVideo = (work) => {
  if (!userStore.isLoggedIn || !userStore.user || !work) return false
  return work.userId === userStore.user.id
}
</script>

<style scoped>
.featured-container {
  height: 100%;
  overflow-y: auto;
  background: #1a1a1a;
}

.loading-state {
  text-align: center;
  padding: 20px 0;
  color: #888;
  font-size: 13px;
  grid-column: 1 / -1;
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
</style>