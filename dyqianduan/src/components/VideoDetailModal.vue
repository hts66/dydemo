<template>
  <div v-if="currentVideo" class="video-modal" @click.self="close">
    <div class="modal-content">
      <button class="close-btn" type="button" @click="close">✕</button>

      <div class="modal-body">
        <div class="video-section">
          <video
            ref="videoPlayer"
            :src="getProxyUrl(currentVideo.url)"
            :poster="currentVideo.thumbnail"
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
              type="button"
              class="author-avatar-btn"
              @click.stop="goToAuthorProfile"
            >
              <img :src="currentVideo.avatar || defaultAvatar" alt="" />
            </button>
            <button
              type="button"
              class="author-name"
              @click.stop="goToAuthorProfile"
            >
              {{ currentVideo.username || '匿名用户' }}
            </button>
            <button
              v-if="!isOwnVideo"
              type="button"
              class="follow-btn"
              :class="{ followed: currentVideo.isFollowing }"
              @click.stop="handleFollow"
            >
              {{ currentVideo.isFollowing ? '已关注' : '+ 关注' }}
            </button>
          </div>

          <div class="action-bar">
            <button
              type="button"
              class="action-btn like-btn"
              :class="{ liked: currentVideo.isLiked }"
              @click="handleLike"
            >
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              <span>{{ currentVideo.likesCount || 0 }}</span>
            </button>
            <button type="button" class="action-btn" @click="scrollToComments">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18z"/>
              </svg>
              <span>{{ currentVideo.commentsCount || 0 }}</span>
            </button>
            <div class="share-wrapper">
              <button type="button" class="action-btn" @click.stop="toggleShare">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z"/>
                </svg>
              </button>
              <div v-if="showShare" class="share-popup" @click.stop>
                <div class="share-popup-header">
                  分享给好友
                  <button type="button" class="share-popup-close" @click="showShare = false">✕</button>
                </div>
                <div class="share-popup-list">
                  <div v-if="friends.length === 0" class="share-no-friends">你还没有好友，快去添加好友吧</div>
                  <div
                    v-for="friend in friends"
                    :key="friend.id"
                    class="share-friend-item"
                    @click="shareToFriend(friend)"
                  >
                    <img :src="friend.avatar || defaultAvatar" alt="" />
                    <span>{{ friend.username }}</span>
                  </div>
                </div>
                <div v-if="shareSuccess" class="share-success">已分享 ✓</div>
              </div>
            </div>
          </div>

          <div class="comments-section" ref="commentsSection">
            <h4 class="comments-title">评论</h4>
            <div class="comments-list">
              <div v-for="comment in comments" :key="comment.id" class="comment-item">
                <img
                  :src="comment.avatar || defaultAvatar"
                  class="comment-avatar"
                  alt=""
                  @click.stop="goToProfile(comment.userId)"
                />
                <div class="comment-content">
                  <span class="comment-author">{{ comment.username || '匿名用户' }}</span>
                  <span class="comment-text">{{ comment.content }}</span>
                </div>
                <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
              </div>
              <div v-if="comments.length === 0" class="no-comments">暂无评论，快来抢沙发吧~</div>
            </div>
            <div class="comment-input-section">
              <input
                v-model="commentInput"
                class="comment-input"
                type="text"
                placeholder="输入评论..."
                :disabled="!userStore.isLoggedIn"
                @keyup.enter="handleComment"
              />
              <button
                type="button"
                class="send-btn"
                :disabled="!commentInput.trim() || !userStore.isLoggedIn"
                @click="handleComment"
              >
                发送
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="notice" class="profile-notice">{{ notice }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { toggleLike, isLiked as checkIsLiked } from '../api/like'
import { getComments, addComment } from '../api/comment'
import { toggleFollow, checkIsFollowing, getFriends } from '../api/follow'
import { sendMessage } from '../api/message'

const props = defineProps({
  video: { type: Object, default: null },
  profileUserId: { type: [Number, String], default: null },
})

const emit = defineEmits(['close', 'update:video'])
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const currentVideo = ref(null)
const comments = ref([])
const commentInput = ref('')
const friends = ref([])
const showShare = ref(false)
const shareSuccess = ref(false)
const notice = ref('')
const videoPlayer = ref(null)
const commentsSection = ref(null)
let noticeTimer
let loadToken = 0

const isOwnVideo = computed(() => {
  const authorId = Number(currentVideo.value?.userId ?? currentVideo.value?.user_id)
  return Boolean(userStore.user?.id && authorId && Number(userStore.user.id) === authorId)
})

const getProxyUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return '/api/video/proxy?url=' + encodeURIComponent(url)
  }
  return url
}

const normalizeVideo = (video) => {
  if (!video) return null
  return {
    ...video,
    userId: video.userId ?? video.user_id,
    username: video.username || video.user_name,
    avatar: video.avatar || video.userAvatar,
  }
}

const syncVideo = () => {
  if (currentVideo.value) emit('update:video', { ...currentVideo.value })
}

const showNotice = (message) => {
  notice.value = message
  clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => {
    notice.value = ''
  }, 1800)
}

const playVideo = async () => {
  await nextTick()
  if (!videoPlayer.value) return
  videoPlayer.value.play().catch((error) => {
    if (error.name === 'NotAllowedError') {
      videoPlayer.value.muted = true
      videoPlayer.value.play().catch(() => {})
    }
  })
}

const loadVideoData = async (video) => {
  const token = ++loadToken
  comments.value = []
  try {
    const [likeResult, commentResult, followResult] = await Promise.all([
      checkIsLiked(video.id).catch(() => null),
      getComments(video.id).catch(() => null),
      userStore.isLoggedIn && video.userId
        ? checkIsFollowing(video.userId).catch(() => null)
        : Promise.resolve(null),
    ])

    if (token !== loadToken || !currentVideo.value) return
    if (likeResult?.code === 200) currentVideo.value.isLiked = likeResult.data
    if (commentResult?.code === 200) comments.value = commentResult.data || []
    if (followResult?.code === 200) currentVideo.value.isFollowing = followResult.data
    syncVideo()
  } finally {
    if (token === loadToken) playVideo()
  }
}

watch(
  () => props.video,
  (video) => {
    currentVideo.value = normalizeVideo(video)
    showShare.value = false
    shareSuccess.value = false
    commentInput.value = ''
    if (currentVideo.value) {
      document.body.style.overflow = 'hidden'
      loadVideoData(currentVideo.value)
    } else {
      document.body.style.overflow = ''
    }
  },
  { immediate: true },
)

const close = () => {
  videoPlayer.value?.pause()
  document.body.style.overflow = ''
  emit('close')
}

const profileId = computed(() => Number(props.profileUserId || route.params.userId))

const goToAuthorProfile = () => {
  const authorId = Number(currentVideo.value?.userId)
  if (!authorId) return
  if (profileId.value && profileId.value === authorId) {
    showNotice('已进入该作者主页')
    return
  }
  close()
  router.push('/profile/' + authorId)
}

const goToProfile = (userId) => {
  const targetId = Number(userId)
  if (!targetId) return
  if (profileId.value && profileId.value === targetId) {
    showNotice('已进入该作者主页')
    return
  }
  close()
  router.push('/profile/' + targetId)
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const result = await toggleLike(currentVideo.value.id).catch(() => null)
  if (result?.code === 200) {
    currentVideo.value.isLiked = result.data
    currentVideo.value.likesCount = Math.max(
      0,
      (currentVideo.value.likesCount || 0) + (result.data ? 1 : -1),
    )
    syncVideo()
  }
}

const handleFollow = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const authorId = Number(currentVideo.value?.userId)
  if (!authorId) return
  const result = await toggleFollow(authorId).catch(() => null)
  if (result?.code === 200) {
    currentVideo.value.isFollowing = result.data
    syncVideo()
  }
}

const toggleShare = async () => {
  if (showShare.value) {
    showShare.value = false
    shareSuccess.value = false
    return
  }
  if (userStore.user?.id) {
    const result = await getFriends(userStore.user.id).catch(() => null)
    friends.value = result?.code === 200 ? result.data || [] : []
  }
  showShare.value = true
}

const shareToFriend = async (friend) => {
  const video = currentVideo.value
  if (!video) return
  const content = '📹 [分享视频] ' + video.id + '\n' +
    (video.title || '无标题') + '\n' + (video.description || '') + '\n' +
    (video.url || '') + '\n' + (video.thumbnail || '')
  const result = await sendMessage(friend.id, content).catch(() => null)
  if (result?.code === 200) {
    shareSuccess.value = true
    setTimeout(() => {
      showShare.value = false
      shareSuccess.value = false
    }, 1000)
  }
}

const scrollToComments = () => {
  commentsSection.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleComment = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const content = commentInput.value.trim()
  if (!content || !currentVideo.value) return
  const result = await addComment(currentVideo.value.id, content).catch(() => null)
  if (result?.code === 200) {
    commentInput.value = ''
    currentVideo.value.commentsCount = (currentVideo.value.commentsCount || 0) + 1
    const commentResult = await getComments(currentVideo.value.id).catch(() => null)
    if (commentResult?.code === 200) comments.value = commentResult.data || []
    syncVideo()
  }
}

const formatTime = (dateString) => {
  if (!dateString) return ''
  const minutes = Math.floor((Date.now() - new Date(dateString).getTime()) / 60000)
  if (minutes < 60) return Math.max(0, minutes) + '分钟前'
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return hours + '小时前'
  return Math.floor(hours / 24) + '天前'
}

onBeforeUnmount(() => {
  clearTimeout(noticeTimer)
  videoPlayer.value?.pause()
  document.body.style.overflow = ''
})
</script>
