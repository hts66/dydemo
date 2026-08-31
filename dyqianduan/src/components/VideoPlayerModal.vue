<template>
  <div v-if="visible" class="video-modal" @click.self="closeVideo">
    <div class="modal-content">
      <button class="close-btn" @click="closeVideo">✕</button>
      <div class="modal-body">
        <div class="video-section">
          <video
            ref="videoPlayer"
            :src="getProxyUrl(work?.url)"
            :poster="work?.thumbnail"
            class="modal-video"
            controls
            loop
            preload="auto"
            playsinline
            webkit-playsinline
            x5-playsinline
          ></video>
        </div>
        <div class="interaction-section">
          <div class="author-bar">
            <button
              v-if="work && !isOwnVideo"
              class="follow-btn"
              :class="{ followed: work.isFollowing }"
              @click.stop="handleFollow"
            >
              <img
                :src="work.avatar || defaultAvatar"
                @click.stop="goToProfile(work.userId)"
              />
              <svg v-if="!work.isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="#fff">
                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
            </button>
            <div v-else class="follow-btn" style="cursor: pointer;">
              <img
                :src="work?.avatar || defaultAvatar"
                @click.stop="goToProfile(work?.userId)"
              />
            </div>
            <span class="author-name">{{ work?.username || '匿名用户' }}</span>
          </div>
          <div class="action-bar">
            <button
              class="action-btn like-btn"
              :class="{ liked: work?.isLiked }"
              @click="handleLike"
            >
              <svg v-if="work?.isLiked" viewBox="0 0 24 24" width="20" height="20" fill="#fe2c55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                <path d="M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3z"/>
              </svg>
              <span>{{ work?.likesCount || 0 }}</span>
            </button>
            <button class="action-btn" @click="scrollToComments">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>
              </svg>
              <span>{{ work?.commentsCount || 0 }}</span>
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
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { isLiked as checkIsLiked, toggleLike } from '../api/like'
import { getComments, addComment } from '../api/comment'
import { checkIsFollowing, toggleFollow } from '../api/follow'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  work: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'work-updated'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const videoPlayer = ref(null)
const commentsSection = ref(null)
const comments = ref([])
const commentInput = ref('')

const getProxyUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `/api/video/proxy?url=${encodeURIComponent(url)}`
  }
  return url
}

const isOwnVideo = () => {
  if (!userStore.isLoggedIn || !userStore.user || !props.work) return false
  return props.work.userId === userStore.user.id
}

const loadWorkData = async () => {
  if (!props.work) return
  
  try {
    const [likeResult, commentResult, followResult] = await Promise.all([
      checkIsLiked(props.work.id),
      getComments(props.work.id),
      userStore.isLoggedIn ? checkIsFollowing(props.work.userId) : Promise.resolve({ code: 200, data: false })
    ])
    
    if (likeResult.code === 200) {
      props.work.isLiked = likeResult.data
    }
    if (commentResult.code === 200) {
      comments.value = commentResult.data
    }
    if (followResult.code === 200) {
      props.work.isFollowing = followResult.data
    }
  } catch (err) {
    console.error('加载视频信息失败', err)
  }
}

const playVideo = async () => {
  await nextTick()
  if (videoPlayer.value) {
    videoPlayer.value.currentTime = 0
    videoPlayer.value.muted = false
    videoPlayer.value.play().catch(e => {
      if (e.name === 'NotAllowedError') {
        videoPlayer.value.muted = true
        videoPlayer.value.play().catch(err => console.error('播放失败', err))
      } else {
        console.error('播放失败', e)
      }
    })
  }
}

const pauseVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
  }
}

const closeVideo = () => {
  pauseVideo()
  emit('close')
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  try {
    const result = await toggleLike(props.work.id)
    if (result.code === 200) {
      props.work.isLiked = result.data
      props.work.likesCount += result.data ? 1 : -1
      emit('work-updated', props.work)
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  try {
    const result = await toggleFollow(props.work.userId)
    if (result.code === 200) {
      props.work.isFollowing = result.data
      emit('work-updated', props.work)
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
      closeVideo()
      router.push('/my')
      return
    }
  }
  
  closeVideo()
  router.push(`/profile/${userId}`)
}

const handleComment = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  if (!commentInput.value.trim()) return
  
  try {
    const result = await addComment(props.work.id, commentInput.value.trim())
    if (result.code === 200) {
      commentInput.value = ''
      props.work.commentsCount++
      emit('work-updated', props.work)
      
      const commentResult = await getComments(props.work.id)
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

watch(() => props.visible, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden'
    loadWorkData()
    playVideo()
  } else {
    document.body.style.overflow = ''
    pauseVideo()
    comments.value = []
    commentInput.value = ''
  }
})

watch(() => props.work, (newWork) => {
  if (props.visible && newWork) {
    loadWorkData()
    playVideo()
  }
})
</script>

<style scoped>
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