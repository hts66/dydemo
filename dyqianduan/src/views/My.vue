<template>
  <div class="my-container" ref="containerRef" @scroll="handleScroll">
    <div 
      class="profile-header-section"
      :style="headerStyle"
      @click="toggleBackgroundPicker"
    >
      <button class="logout-btn" v-if="userStore.isLoggedIn" @click.stop="handleLogout">退出登录</button>
      <div class="background-hint">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff">
          <path d="M20.71 4.04a1 1 0 0 0-1.42 0l-3.02 3.02-7.19-7.19a1 1 0 0 0-1.42 0L3.29 5.36a1 1 0 0 0 0 1.42l7.19 7.19-3.02 3.02a1 1 0 0 0 0 1.42l2.12 2.12a1 1 0 0 0 1.42 0l3.02-3.02 7.19 7.19a1 1 0 0 0 1.42 0l2.12-2.12a1 1 0 0 0 0-1.42l-7.19-7.19 3.02-3.02a1 1 0 0 0 0-1.42z"/>
        </svg>
        <span>点击更换背景</span>
      </div>
      <div class="profile-info" @click.stop>
        <div class="avatar-wrapper" @click.stop="openEditModal" v-if="userStore.isLoggedIn">
          <img :src="mediaUrl(userStore.user?.avatar) || defaultAvatar" class="avatar-img" />
          <div class="avatar-edit-hint">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
              <path d="M3 4V1h2v3h3v2H5v3H3V6H0V4h3zm3 6V7h3V4h7l1.83 2H21c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2V10h3zm7 9c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-3.2-5c0 1.77 1.43 3.2 3.2 3.2s3.2-1.43 3.2-3.2-1.43-3.2-3.2-3.2-3.2 1.43-3.2 3.2z"/>
            </svg>
          </div>
        </div>
        <div class="avatar-wrapper" v-else>
          <img :src="defaultAvatar" class="avatar-img" />
        </div>
        <div class="user-details">
          <div class="username-row">
            <h2 class="username">{{ userStore.user?.username || '未登录' }}</h2>
            <button class="edit-btn" v-if="userStore.isLoggedIn" @click.stop="openEditModal">编辑资料</button>
          </div>
          <div class="stats-row">
            <div class="stat-item">
              <span class="stat-num">{{ followingCount }}</span>
              <span class="stat-label">关注</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ worksCount }}</span>
              <span class="stat-label">获赞</span>
            </div>
          </div>
          <p class="user-bio">{{ userStore.user?.bio || '这个人很懒，什么都没写~' }}</p>
        </div>
      </div>
    </div>

    <div class="tabs-bar">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'works' }"
        @click="handleTabClick('works')"
      >
        作品 {{ myWorks.length }}
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'liked' }"
        @click="handleTabClick('liked')"
      >
        喜欢
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'following' }"
        @click="handleTabClick('following')"
      >
        关注
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'followers' }"
        @click="handleTabClick('followers')"
      >
        粉丝
      </div>
      
      <div class="tabs-right">
        <template v-if="!isBatchMode">
          <button 
            v-if="activeTab === 'works' || activeTab === 'liked' || activeTab === 'following'" 
            class="batch-btn"
            @click="enterBatchMode"
          >
            批量管理
          </button>
        </template>
        <template v-else>
          <button class="batch-btn cancel" @click="exitBatchMode">取消</button>
          <button class="batch-btn select-all" @click="toggleSelectAll">
            {{ isAllSelected() ? '取消全选' : '全选' }}
          </button>
          <button
            class="batch-btn confirm"
            :disabled="selectedItems.length === 0"
            @click="confirmBatchAction"
          >
            {{ getBatchActionText() }} ({{ selectedItems.length }})
          </button>
        </template>
      </div>
    </div>

    <div class="tab-content">
      <div v-if="activeTab === 'works'" class="works-grid">
        <div 
          v-for="work in myWorks" 
          :key="work.id"
          class="work-card"
          :class="{ 'batch-selected': isBatchMode && selectedItems.includes(work.id) }"
          @click="handleWorkClick(work)"
        >
          <input 
            type="checkbox" 
            v-if="isBatchMode" 
            class="batch-checkbox"
            :checked="selectedItems.includes(work.id)"
            @click.stop="toggleSelectItem(work.id)"
          />
          <div class="work-thumbnail">
            <img :src="mediaUrl(work.thumbnail || work.url)" />
            <div class="play-overlay">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </div>
          </div>
          <div class="work-info">
            <span class="work-title">{{ work.title || '无标题' }}</span>
            <div class="work-stats">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="#fe2c55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              {{ formatCount(work.likesCount) }}
            </div>
          </div>
        </div>
        <div class="work-card upload-card" v-if="userStore.isLoggedIn" @click="$router.push('/upload')">
          <div class="upload-placeholder">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="#555">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
            <span class="upload-text">发布作品</span>
          </div>
        </div>
        <div v-if="myWorks.length === 0 && !userStore.isLoggedIn && !worksLoading" class="empty-state">
          <p>还没有发布作品</p>
        </div>
        <div v-if="worksLoading" class="loading-state">
          <p>加载中...</p>
        </div>
      </div>

      <div v-if="activeTab === 'liked'" class="works-grid">
        <div 
          v-for="work in likedWorks" 
          :key="work.id"
          class="work-card"
          :class="{ 'batch-selected': isBatchMode && selectedItems.includes(work.id) }"
          @click="handleWorkClick(work)"
        >
          <input 
            type="checkbox" 
            v-if="isBatchMode" 
            class="batch-checkbox"
            :checked="selectedItems.includes(work.id)"
            @click.stop="toggleSelectItem(work.id)"
          />
          <div class="work-thumbnail">
            <img :src="mediaUrl(work.thumbnail || work.url)" />
            <div class="play-overlay">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="#fff">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </div>
          </div>
          <div class="work-info">
            <span class="work-title">{{ work.title || '无标题' }}</span>
            <div class="work-stats">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="#fe2c55">
                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
              </svg>
              {{ formatCount(work.likesCount) }}
            </div>
          </div>
        </div>
        <div v-if="likedWorks.length === 0 && !likedLoading" class="empty-state">
          <p>还没有喜欢的作品</p>
        </div>
        <div v-if="likedLoading" class="loading-state">
          <p>加载中...</p>
        </div>
      </div>

      <div v-if="activeTab === 'following'" class="user-list">
        <div 
          v-for="user in followingList" 
          :key="user.id" 
          class="user-item" @click="goToProfile(user.id)"
          :class="{ 'batch-selected': isBatchMode && selectedItems.includes(user.id) }"
        >
          <input 
            type="checkbox" 
            v-if="isBatchMode" 
            class="batch-checkbox"
            :checked="selectedItems.includes(user.id)"
            @click.stop="toggleSelectItem(user.id)"
          />
          <img :src="mediaUrl(user.avatar) || defaultAvatar" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">{{ user.username || '匿名用户' }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒' }}</span>
          </div>
          <button 
            v-if="!isBatchMode"
            class="follow-btn following" 
            @click.stop="handleFollow(user.id)"
          >
            已关注
          </button>
        </div>
        <div v-if="followingList.length === 0" class="empty-state">
          <p>还没有关注任何人</p>
        </div>
      </div>

      <div v-if="activeTab === 'followers'" class="user-list">
        <div v-for="user in followerList" :key="user.id" class="user-item" @click="goToProfile(user.id)">
          <img :src="mediaUrl(user.avatar) || defaultAvatar" class="user-avatar" />
          <div class="user-info">
            <span class="user-name">{{ user.username || '匿名用户' }}</span>
            <span class="user-bio">{{ user.bio || '这个人很懒' }}</span>
          </div>
        </div>
        <div v-if="followerList.length === 0" class="empty-state">
          <p>还没有粉丝</p>
        </div>
      </div>
    </div>

    <div v-if="currentVideo" class="video-modal" @click.self="closeVideo">
      <div class="modal-content">
        <button class="close-btn" @click="closeVideo">✕</button>
        <div class="modal-body">
          <div class="video-section">
            <video
              ref="videoPlayer"
              :src="mediaUrl(currentVideo.url)"
              :poster="mediaUrl(currentVideo.thumbnail)"
              class="modal-video"
              controls
              autoplay
              loop
            ></video>
          </div>
          <div class="interaction-section">
            <div class="author-bar">
              <button
                v-if="currentVideo.userId !== userStore.user?.id"
                class="follow-btn"
                :class="{ followed: currentVideo.isFollowing }"
                @click.stop="handleFollow(currentVideo.userId)"
              >
                <img :src="mediaUrl(currentVideo.avatar) || defaultAvatar" />
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
              <div class="share-wrapper">
                <button class="action-btn" @click.stop="toggleSharePopup">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff">
                    <path d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z"/>
                  </svg>
                </button>
                <div class="share-popup" v-if="showSharePopup" @click.stop>
                  <div class="share-popup-header">
                    分享给好友
                    <button class="share-popup-close" @click="showSharePopup = false; shareSuccess = false">✕</button>
                  </div>
                  <div class="share-popup-list">
                    <div v-if="friends.length === 0" class="share-no-friends">
                      <p>你还没有好友，快去添加好友吧</p>
                    </div>
                    <div v-for="friend in friends" :key="friend.id" class="share-friend-item" @click="shareToFriend(friend)">
                      <img :src="mediaUrl(friend.avatar) || defaultAvatar" />
                      <span>{{ friend.username }}</span>
                    </div>
                  </div>
                  <div v-if="shareSuccess" class="share-success">已分享 ✓</div>
                </div>
              </div>
            </div>
            <div class="video-info-panel">
              <h3>{{ currentVideo.title || '无标题' }}</h3>
              <p v-if="currentVideo.description">{{ currentVideo.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showEditModal" class="edit-modal" @click.self="closeEditModal">
      <div class="edit-modal-content">
        <div class="edit-modal-header">
          <h3>编辑资料</h3>
          <button class="close-btn" @click="closeEditModal">✕</button>
        </div>
        <div class="edit-modal-body">
          <div class="avatar-edit-section">
            <label class="avatar-edit-label">头像</label>
            <div class="avatar-edit-wrapper" @click="triggerAvatarUpload">
              <img :src="mediaUrl(editForm.avatar) || defaultAvatar" class="avatar-edit-img" />
              <div class="avatar-edit-overlay">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff">
                  <path d="M3 4V1h2v3h3v2H5v3H3V6H0V4h3zm3 6V7h3V4h7l1.83 2H21c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2V10h3zm7 9c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-3.2-5c0 1.77 1.43 3.2 3.2 3.2s3.2-1.43 3.2-3.2-1.43-3.2-3.2-3.2-3.2 1.43-3.2 3.2z"/>
                </svg>
                <span>更换头像</span>
              </div>
            </div>
            <input 
              type="file" 
              ref="avatarFileInput" 
              accept="image/*" 
              @change="handleAvatarChange" 
              class="hidden-input"
            />
          </div>

          <div class="edit-field">
            <label class="edit-field-label">用户名</label>
            <input 
              type="text" 
              v-model="editForm.username" 
              class="edit-input"
              placeholder="请输入用户名"
            />
          </div>

          <div class="edit-field">
            <label class="edit-field-label">性别</label>
            <div class="gender-options">
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="0" />
                <span>未知</span>
              </label>
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="1" />
                <span>男</span>
              </label>
              <label class="gender-option">
                <input type="radio" v-model="editForm.gender" :value="2" />
                <span>女</span>
              </label>
            </div>
          </div>

          <div class="edit-field">
            <label class="edit-field-label">简介</label>
            <textarea 
              v-model="editForm.bio" 
              class="edit-textarea"
              placeholder="介绍一下自己..."
              rows="4"
            ></textarea>
          </div>

          <div class="edit-actions">
            <button class="cancel-btn" @click="closeEditModal">取消</button>
            <button class="save-btn" @click="saveProfile" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>

          <p v-if="editSuccess" class="success-msg">保存成功</p>
          <p v-if="editError" class="error-msg">{{ editError }}</p>
        </div>
      </div>
    </div>

    <div v-if="showBackgroundPicker" class="background-modal" @click.self="toggleBackgroundPicker">
      <div class="background-modal-content">
        <div class="background-modal-header">
          <h3>上传背景图片</h3>
          <button class="close-btn" @click="toggleBackgroundPicker">✕</button>
        </div>
        <div class="background-upload-area">
          <input 
            type="file" 
            ref="backgroundFileInput"
            accept="image/*"
            class="background-file-input"
            @change="handleBackgroundUpload"
          />
          <div class="upload-placeholder" v-if="!previewBackground" @click="triggerBackgroundUpload">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="#888">
              <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
            </svg>
            <p>点击上传背景图片</p>
            <p class="upload-hint">支持 jpg、png、gif 格式</p>
          </div>
          <div v-if="previewBackground" class="background-preview">
            <img :src="mediaUrl(previewBackground)" />
            <div class="bg-actions">
              <button class="bg-confirm-btn" @click.stop="confirmBackground">确认更换</button>
            </div>
            <button class="remove-bg-btn" @click="removeBackground">移除背景</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { deleteWork, getUserWorks } from '../api/work'
import { getFollowList, toggleFollow, checkIsFollowing, getFriends } from '../api/follow'
import { getLikedWorks, toggleLike, isLiked as checkIsLiked } from '../api/like'
import { sendMessage } from '../api/message'
import { updateBackground, updateAvatar, updateBackgroundFile } from '../api/user'
import request from '../utils/request'
import { mediaUrl } from '../utils/media'

const router = useRouter()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const containerRef = ref(null)

const activeTab = ref('works')
const myWorks = ref([])
const likedWorks = ref([])
const followingList = ref([])
const followerList = ref([])
const followingCount = ref(0)
const followerCount = ref(0)
const worksCount = ref(0)
const currentVideo = ref(null)
const videoPlayer = ref(null)

// 分页懒加载 — 作品
const worksPage = ref(1)
const worksHasMore = ref(true)
const worksLoading = ref(false)
// 分页懒加载 — 喜欢
const likedPage = ref(1)
const likedHasMore = ref(true)
const likedLoading = ref(false)

const isBatchMode = ref(false)
const selectedItems = ref([])

const showEditModal = ref(false)
const saving = ref(false)
const editSuccess = ref(false)
const editError = ref('')
const avatarFileInput = ref(null)
const pendingAvatarFile = ref(null)      // 待上传的头像文件（选择后暂存，保存时才上传）
const originalAvatar = ref('')           // 打开弹窗时的原始头像URL
const editForm = ref({
  username: '',
  gender: 0,
  bio: '',
  avatar: '',
})

const currentBackground = ref('')
const showBackgroundPicker = ref(false)
const backgroundFileInput = ref(null)
const previewBackground = ref('')
const pendingBackgroundFile = ref(null)   // 待上传的背景图文件
const headerHeight = ref(300)

const toggleBackgroundPicker = () => {
  showBackgroundPicker.value = !showBackgroundPicker.value
  if (showBackgroundPicker.value && currentBackground.value) {
    previewBackground.value = currentBackground.value
  } else {
    // 关闭弹窗 → 丢弃所有本地预览
    previewBackground.value = ''
    pendingBackgroundFile.value = null
  }
}

const triggerBackgroundUpload = () => {
  backgroundFileInput.value?.click()
}

const handleBackgroundUpload = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 仅本地预览，不上传到 MinIO！上传在 confirmBackground 点"确认更换"时才执行
  pendingBackgroundFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewBackground.value = ev.target.result
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

const confirmBackground = async () => {
  if (!pendingBackgroundFile.value) return

  try {
    const result = await updateBackgroundFile(pendingBackgroundFile.value)
    if (result.code === 200) {
      currentBackground.value = result.data.background
      previewBackground.value = result.data.background
      userStore.setUser(result.data)
      pendingBackgroundFile.value = null
    }
  } catch (err) {
    console.error('更换背景图失败', err)
  }
}

const cancelBackground = () => {
  previewBackground.value = currentBackground.value || ''
  pendingBackgroundFile.value = null
}

const removeBackground = async () => {
  currentBackground.value = ''
  previewBackground.value = ''
  userStore.user.background = ''
  try {
    await updateBackground('')
  } catch (err) {
    console.error('移除背景失败', err)
  }
}

const headerStyle = computed(() => {
  const styles = {
    height: headerHeight.value + 'px'
  }
  if (currentBackground.value) {
    styles.backgroundImage = `url(${mediaUrl(currentBackground.value)})`
    styles.backgroundSize = 'cover'
    styles.backgroundPosition = 'center'
    styles.backgroundRepeat = 'no-repeat'
  } else {
    styles.background = 'linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%)'
  }
  return styles
})

const handleScroll = (event) => {
  const { scrollTop, scrollHeight, clientHeight } = event.target
  // 头部缩放
  const minHeight = 120
  const maxHeight = 300
  const newHeight = Math.max(minHeight, maxHeight - scrollTop * 0.5)
  headerHeight.value = newHeight
  // 触底加载更多（作品一次全量、喜欢列表分页）
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    if (activeTab.value === 'liked') loadLikedWorks()
  }
}

const openEditModal = () => {
  const user = userStore.user
  editForm.value = {
    username: user?.username || '',
    gender: user?.gender ?? 0,
    bio: user?.bio || '',
    avatar: user?.avatar || '',
  }
  // 保存原始头像URL，用于取消时恢复
  originalAvatar.value = user?.avatar || ''
  pendingAvatarFile.value = null
  editSuccess.value = false
  editError.value = ''
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  // 取消编辑 → 丢弃本地预览，什么都不上传到 MinIO
  pendingAvatarFile.value = null
  editForm.value.avatar = originalAvatar.value
}

const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

const handleAvatarChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    editError.value = '请选择图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    editError.value = '图片大小不能超过5MB'
    return
  }

  // 仅本地预览，不上传到 MinIO！上传在 saveProfile 点"保存"时才执行
  pendingAvatarFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    editForm.value.avatar = ev.target.result
  }
  reader.readAsDataURL(file)
  editError.value = ''
  e.target.value = ''
}

const saveProfile = async () => {
  if (!editForm.value.username.trim()) {
    editError.value = '用户名不能为空'
    return
  }
  saving.value = true
  editError.value = ''
  editSuccess.value = false

  try {
    // 只有点"保存"时才真正上传头像到 MinIO
    if (pendingAvatarFile.value) {
      const avatarResult = await updateAvatar(pendingAvatarFile.value)
      if (avatarResult.code !== 200) {
        editError.value = avatarResult.message || '头像上传失败'
        saving.value = false
        return
      }
      // updateAvatar 已原子完成：上传MinIO + 删旧头像 + 更新DB
      userStore.setUser(avatarResult.data)
      pendingAvatarFile.value = null
    }

    // 更新其他资料字段（用户名、性别、简介）
    // 注意：如果头像已通过 updateAvatar 更新，不要再传 avatar，
    // 否则后端 updateProfile 会误删刚上传的头像
    const result = await request.put('/auth/user/profile', {
      username: editForm.value.username.trim(),
      gender: editForm.value.gender,
      bio: editForm.value.bio.trim(),
    })
    if (result.code === 200) {
      // 头像已在上面更新了 store，这里再同步一次确保一致
      userStore.setUser(result.data)
      editSuccess.value = true
      setTimeout(() => {
        closeEditModal()
      }, 1000)
    } else {
      editError.value = result.message || '保存失败'
    }
  } catch (err) {
    editError.value = '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

const loadMyWorks = async (reset = false) => {
  if (!userStore.user) return
  if (worksLoading.value) return
  worksLoading.value = true
  try {
    // 自己的作品页不用懒加载，一次性拉取全部
    const result = await getUserWorks(userStore.user.id, 1, 10000)
    if (result.code === 200) {
      myWorks.value = result.data || []
      worksCount.value = myWorks.value.length
    }
  } catch (err) {
    console.error('加载作品失败', err)
  } finally {
    worksLoading.value = false
  }
}

const loadFollowData = async () => {
  if (!userStore.user) return
  try {
    const result = await getFollowList(userStore.user.id)
    if (result.code === 200) {
      followingList.value = result.data.following
      followerList.value = result.data.followers
      followingCount.value = result.data.following.length
      followerCount.value = result.data.followers.length
    }
  } catch (err) {
    console.error('加载关注数据失败', err)
  }
}

const loadLikedWorks = async (reset = false) => {
  if (!userStore.user) return
  if (likedLoading.value) return
  if (reset) {
    likedPage.value = 1
    likedHasMore.value = true
    likedWorks.value = []
  }
  if (!likedHasMore.value) return
  likedLoading.value = true
  try {
    const result = await getLikedWorks(userStore.user.id, likedPage.value, 12)
    if (result.code === 200) {
      if (result.data.length === 0) {
        likedHasMore.value = false
      } else {
        likedWorks.value.push(...result.data)
        likedPage.value++
      }
    }
  } catch (err) {
    console.error('加载点赞视频失败', err)
  } finally {
    likedLoading.value = false
  }
}

const handleLike = async () => {
  if (!currentVideo.value) return
  try {
    const result = await toggleLike(currentVideo.value.id)
    if (result.code === 200) {
      currentVideo.value.isLiked = !currentVideo.value.isLiked
      if (currentVideo.value.isLiked) {
        currentVideo.value.likesCount = (currentVideo.value.likesCount || 0) + 1
      } else {
        currentVideo.value.likesCount = Math.max(0, (currentVideo.value.likesCount || 0) - 1)
      }
    }
  } catch (err) {
    console.error('点赞失败', err)
  }
}

const openVideo = (work) => {
  currentVideo.value = work
  document.body.style.overflow = 'hidden'
  showSharePopup.value = false
  shareSuccess.value = false
  // 加载点赞和关注状态
  if (userStore.isLoggedIn) {
    checkIsLiked(work.id).then(r => { if (r.code === 200) currentVideo.value.isLiked = r.data }).catch(() => {})
    if (work.userId && work.userId !== userStore.user?.id) {
      checkIsFollowing(work.userId).then(r => { if (r.code === 200) currentVideo.value.isFollowing = r.data }).catch(() => {})
    }
  }
}

// 分享相关
const showSharePopup = ref(false)
const shareSuccess = ref(false)
const friends = ref([])

const toggleSharePopup = async () => {
  if (showSharePopup.value) { showSharePopup.value = false; shareSuccess.value = false; return }
  if (userStore.user?.id) {
    try {
      const result = await getFriends(userStore.user.id)
      if (result.code === 200) friends.value = result.data || []
    } catch (_) { friends.value = [] }
  }
  showSharePopup.value = true
}

const shareToFriend = async (friend) => {
  if (!currentVideo.value) return
  const title = currentVideo.value.title || '无标题'
  const description = currentVideo.value.description || ''
  const content = `📹 [分享视频] ${currentVideo.value.id}\n${title}\n${description}\n${currentVideo.value.url}\n${currentVideo.value.thumbnail || ''}`
  try {
    await sendMessage(friend.id, content)
    shareSuccess.value = true
    setTimeout(() => { showSharePopup.value = false; shareSuccess.value = false }, 1000)
  } catch (_) {}
}

const closeVideo = () => {
  if (videoPlayer.value) {
    videoPlayer.value.pause()
  }
  currentVideo.value = null
  document.body.style.overflow = ''
}

const handleFollow = async (userId) => {
  try {
    const result = await toggleFollow(userId)
    if (result.code === 200 && !result.data) {
      followingList.value = followingList.value.filter(u => u.id !== userId)
      followingCount.value--
    }
  } catch (err) {
    console.error('取消关注失败', err)
  }
}

const goToProfile = (userId) => {
  router.push(`/profile/${userId}`)
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  return count.toString()
}

const handleTabClick = (tab) => {
  activeTab.value = tab
  exitBatchMode()
  if (tab === 'works') loadMyWorks(true)
  else if (tab === 'liked') loadLikedWorks(true)
}

const enterBatchMode = () => {
  isBatchMode.value = true
  selectedItems.value = []
}

const exitBatchMode = () => {
  isBatchMode.value = false
  selectedItems.value = []
}

const toggleSelectItem = (id) => {
  const index = selectedItems.value.indexOf(id)
  if (index > -1) {
    selectedItems.value.splice(index, 1)
  } else {
    selectedItems.value.push(id)
  }
}

// 获取当前 tab 对应的列表数据
const getCurrentList = () => {
  switch (activeTab.value) {
    case 'works': return myWorks.value
    case 'liked': return likedWorks.value
    case 'following': return followingList.value
    default: return []
  }
}

const isAllSelected = () => {
  const list = getCurrentList()
  if (list.length === 0) return false
  return list.every(item => selectedItems.value.includes(item.id))
}

const toggleSelectAll = () => {
  if (isAllSelected()) {
    selectedItems.value = []
  } else {
    const list = getCurrentList()
    selectedItems.value = list.map(item => item.id)
  }
}

const handleWorkClick = (work) => {
  if (isBatchMode.value) {
    toggleSelectItem(work.id)
  } else {
    openVideo(work)
  }
}

const getBatchActionText = () => {
  switch (activeTab.value) {
    case 'works':
      return '批量删除'
    case 'liked':
      return '批量取消点赞'
    case 'following':
      return '批量取消关注'
    default:
      return '确认'
  }
}

const confirmBatchAction = async () => {
  if (selectedItems.value.length === 0) return
  
  const confirmed = confirm(`确定要${getBatchActionText().replace('批量', '')}选中的${selectedItems.value.length}项吗？`)
  if (!confirmed) return
  
  try {
    switch (activeTab.value) {
      case 'works':
        for (const id of selectedItems.value) {
          const result = await deleteWork(id)
          if (result.code !== 200) {
            throw new Error(result.message || '删除失败')
          }
        }
        myWorks.value = myWorks.value.filter(w => !selectedItems.value.includes(w.id))
        worksCount.value = myWorks.value.length
        break
      case 'liked':
        for (const id of selectedItems.value) {
          const result = await toggleLike(id)
          if (result.code !== 200) {
            throw new Error(result.message || '取消点赞失败')
          }
        }
        likedWorks.value = likedWorks.value.filter(w => !selectedItems.value.includes(w.id))
        break
      case 'following':
        for (const id of selectedItems.value) {
          const result = await toggleFollow(id)
          if (result.code !== 200) {
            throw new Error(result.message || '取消关注失败')
          }
        }
        followingList.value = followingList.value.filter(u => !selectedItems.value.includes(u.id))
        followingCount.value = followingList.value.length
        break
    }
    alert('操作成功')
  } catch (err) {
    console.error('批量操作失败', err)
    alert('操作失败: ' + (err.message || '请重试'))
  } finally {
    exitBatchMode()
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    loadMyWorks()
    loadFollowData()
    loadLikedWorks()
    if (userStore.user?.background) {
      currentBackground.value = userStore.user.background
    }
  }
})
</script>

<style scoped>
.my-container {
  height: 100%;
  overflow-y: auto;
  background: #1a1a1a;
}

.profile-header-section {
  padding: 20px 60px;
  padding-bottom: 30px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  transition: height 0.1s ease-out;
}

.profile-header-section::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), rgba(0, 0, 0, 0.9));
  pointer-events: none;
}

.logout-btn {
  position: absolute;
  top: 40px;
  right: 60px;
  padding: 8px 16px;
  background: #fe2c55;
  color: #fff;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  z-index: 2;
}

.logout-btn:hover {
  background: #e6204a;
}

.background-hint {
  position: absolute;
  bottom: 20px;
  right: 60px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 16px;
  color: white;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
}

.profile-header-section:hover .background-hint {
  opacity: 1;
}

.profile-info {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  cursor: pointer;
}

.avatar-wrapper:hover .avatar-edit-hint {
  opacity: 1;
}

.avatar-edit-hint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details {
  flex: 1;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.username {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);
}

.edit-btn {
  padding: 6px 20px;
  background: transparent;
  border: 1px solid #555;
  border-radius: 16px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.stats-row {
  display: flex;
  gap: 32px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-num {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
}

.stat-label {
  font-size: 13px;
  color: #ddd;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.user-bio {
  font-size: 14px;
  color: #ddd;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
}

.tabs-bar {
  display: flex;
  gap: 0;
  border-bottom: 1px solid #2a2a2a;
  padding: 0 60px;
  justify-content: space-between;
  align-items: center;
}

.tab-item {
  padding: 14px 24px;
  font-size: 14px;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab-item:hover {
  color: #fff;
}

.tab-item.active {
  color: #fff;
  border-bottom-color: #fe2c55;
}

.tabs-right {
  display: flex;
  gap: 12px;
}

.batch-btn {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  transition: background 0.2s;
}

.batch-btn {
  background: #fe2c55;
  color: #fff;
}

.batch-btn:hover {
  background: #e6204a;
}

.batch-btn.cancel {
  background: #3a3a3a;
}

.batch-btn.cancel:hover {
  background: #4a4a4a;
}

.batch-btn.select-all {
  background: #3a3a3a;
}

.batch-btn.select-all:hover {
  background: #4a4a4a;
}

.batch-btn.confirm {
  background: #fe2c55;
}

.batch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tab-content {
  padding: 20px 60px;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.work-card {
  background: #2a2a2a;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  position: relative;
}

.work-card:hover {
  transform: translateY(-4px);
}

.work-card.batch-selected {
  border: 2px solid #fe2c55;
}

.batch-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 20px;
  height: 20px;
  z-index: 10;
  cursor: pointer;
  accent-color: #fe2c55;
}

.upload-card {
  background: #2a2a2a;
  border: 2px dashed #444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-card:hover {
  border-color: #fe2c55;
  transform: translateY(-4px);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #888;
}

.upload-text {
  font-size: 13px;
}

.work-thumbnail {
  position: relative;
  width: 100%;
  padding-top: 130%;
  overflow: hidden;
}

.work-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s;
}

.work-card:hover .play-overlay {
  opacity: 1;
}

.work-info {
  padding: 10px;
}

.work-title {
  font-size: 13px;
  color: #fff;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.work-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #2a2a2a;
  border-radius: 8px;
  position: relative;
  cursor: pointer;
}

.user-item.batch-selected {
  border: 2px solid #fe2c55;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 14px;
  color: #fff;
  font-weight: 500;
}

.user-bio {
  font-size: 12px;
  color: #888;
}

.follow-btn {
  padding: 6px 20px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.follow-btn.following {
  background: #3a3a3a;
  color: #fff;
}

.follow-btn.following:hover {
  background: #fe2c55;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #888;
}

.loading-state {
  text-align: center;
  padding: 20px 0;
  color: #888;
  font-size: 13px;
  grid-column: 1 / -1;
}

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
  height: 80vh;
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
  object-fit: contain;
}

.interaction-section {
  width: 320px;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
  border-left: 1px solid #333;
  overflow-y: auto;
}

.author-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #333;
}

.author-name {
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}

.follow-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  width: 44px;
  height: 44px;
  position: relative;
  flex-shrink: 0;
}

.follow-btn img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.follow-btn svg {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: #fe2c55;
  border-radius: 50%;
  padding: 2px;
}

.follow-btn.followed svg {
  display: none;
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
  background: transparent;
  border: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
}

.video-info-panel {
  padding: 16px;
}

.video-info-panel h3 {
  color: #fff;
  font-size: 15px;
  margin: 0 0 8px 0;
  word-break: break-word;
}

.video-info-panel p {
  color: #aaa;
  font-size: 13px;
  margin: 0;
  word-break: break-word;
}

.video-info-overlay p {
  color: #aaa;
  font-size: 14px;
  margin: 0;
}

.edit-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.edit-modal-content {
  background: #2a2a2a;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}

.edit-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #3a3a3a;
}

.edit-modal-header h3 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.edit-modal-header .close-btn {
  position: static;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.edit-modal-body {
  padding: 24px;
}

.avatar-edit-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.avatar-edit-label {
  color: #aaa;
  font-size: 14px;
  min-width: 48px;
}

.avatar-edit-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar-edit-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-edit-overlay span {
  color: #fff;
  font-size: 11px;
}

.edit-field {
  margin-bottom: 20px;
}

.edit-field-label {
  display: block;
  color: #aaa;
  font-size: 14px;
  margin-bottom: 8px;
}

.edit-input {
  width: 100%;
  padding: 10px 14px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.edit-input:focus {
  border-color: #fe2c55;
}

.edit-textarea {
  width: 100%;
  padding: 10px 14px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
}

.edit-textarea:focus {
  border-color: #fe2c55;
}

.gender-options {
  display: flex;
  gap: 20px;
}

.gender-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #ccc;
  font-size: 14px;
}

.gender-option input[type="radio"] {
  accent-color: #fe2c55;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn {
  padding: 10px 24px;
  background: transparent;
  border: 1px solid #555;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.cancel-btn:hover {
  background: #3a3a3a;
}

.save-btn {
  padding: 10px 24px;
  background: #fe2c55;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.save-btn:hover {
  background: #e6204a;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.success-msg {
  color: #4caf50;
  font-size: 13px;
  margin: 12px 0 0;
  text-align: center;
}

.error-msg {
  color: #fe2c55;
  font-size: 13px;
  margin: 12px 0 0;
  text-align: center;
}

.hidden-input {
  display: none;
}

.background-modal {
  position: fixed;
  top: 0;
  left: 200px;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.background-modal-content {
  background: #2a2a2a;
  border-radius: 12px;
  width: 400px;
  max-width: 90vw;
  overflow: hidden;
}

.background-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #3a3a3a;
}

.background-modal-header h3 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.background-upload-area {
  padding: 20px;
}

.background-file-input {
  display: none;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  border: 2px dashed #444;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-placeholder:hover {
  border-color: #fe2c55;
  background: rgba(254, 44, 85, 0.1);
}

.upload-placeholder p {
  color: #888;
  margin: 8px 0 0 0;
  font-size: 14px;
}

.upload-hint {
  font-size: 12px !important;
  color: #666 !important;
}

.background-preview {
  margin-top: 16px;
  text-align: center;
}

.background-preview img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  object-fit: contain;
}

.bg-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: center;
}

.bg-confirm-btn,
.bg-cancel-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.bg-confirm-btn {
  background: #fe2c55;
  color: #fff;
}

.bg-confirm-btn:hover {
  background: #e6204a;
}

.bg-cancel-btn {
  background: #3a3a3a;
  color: #ccc;
}

.bg-cancel-btn:hover {
  background: #4a4a4a;
}

.remove-bg-btn {
  margin-top: 8px;
  padding: 8px 24px;
  background: #444;
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.remove-bg-btn:hover {
  background: #555;
}

.share-wrapper { position: relative; }
.share-popup {
  position: absolute; bottom: 100%; left: 0; margin-bottom: 8px;
  width: 220px; background: #2a2a2a; border-radius: 12px; overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.4); z-index: 100;
}
.share-popup-header { padding: 12px 16px; font-size: 14px; font-weight: 500; color: #fff; border-bottom: 1px solid #3a3a3a; display: flex; align-items: center; justify-content: space-between; }
.share-popup-close { background: none; border: none; color: #888; font-size: 16px; cursor: pointer; }
.share-popup-close:hover { color: #fff; }
.share-popup-list { max-height: 240px; overflow-y: auto; }
.share-no-friends { padding: 24px 16px; text-align: center; color: #888; font-size: 13px; }
.share-friend-item { display: flex; align-items: center; gap: 10px; padding: 10px 16px; cursor: pointer; transition: background 0.15s; }
.share-friend-item:hover { background: #3a3a3a; }
.share-friend-item img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.share-friend-item span { color: #fff; font-size: 13px; }
.share-success { padding: 10px 16px; text-align: center; color: #2ecc71; font-size: 13px; border-top: 1px solid #3a3a3a; }
</style>