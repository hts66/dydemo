<template>
  <div class="base-video">
    <!-- Loading spinner -->
    <div v-if="loading" class="video-loading">
      <div class="loading-spinner"></div>
    </div>

    <!-- Thumbnail placeholder -->
    <img
      v-if="!loaded && thumbnail"
      :src="thumbnail"
      class="video-placeholder"
      alt=""
    />

    <!-- Video element -->
    <video
      ref="videoRef"
      :src="videoSrc"
      class="video-element"
      :loop="true"
      playsinline
      webkit-playsinline
      x5-playsinline
      :muted="isMuted"
      preload="none"
      @loadstart="onLoadStart"
      @loadedmetadata="onLoadedMetadata"
      @canplay="onCanPlay"
      @play="onPlay"
      @pause="onPause"
      @timeupdate="onTimeUpdate"
      @ended="onEnded"
      @error="onError"
    ></video>

    <!-- Center click area for play/pause toggle -->
    <div class="center-click-area" @click.stop="toggle"></div>

    <!-- Center play/pause icon -->
    <div v-if="!playing && loaded" class="center-play-icon">
      <svg viewBox="0 0 24 24" width="64" height="64" fill="#fff">
        <path d="M8 5v14l11-7z"/>
      </svg>
    </div>

    <!-- Sound toggle button -->
    <div class="sound-toggle" @click.stop="toggleMute">
      <svg v-if="isMuted" viewBox="0 0 24 24" width="24" height="24" fill="#fff">
        <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="#fff">
        <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
      </svg>
    </div>

    <!-- Bottom progress bar -->
    <div
      class="progress-bar-container"
      @click.stop="onProgressClick"
      @pointerdown.prevent="onProgressPointerDown"
    >
      <div class="progress-bar-track">
        <div
          class="progress-bar-fill"
          :style="{ width: progressPercent + '%' }"
        ></div>
        <div
          class="progress-bar-thumb"
          :style="{ left: progressPercent + '%' }"
        ></div>
      </div>
      <div class="progress-time">
        <span>{{ formatTime(currentTime) }}</span>
        <span>{{ formatTime(duration) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { on, off, emit, EVENT_KEY } from '../utils/bus'

const props = defineProps({
  item: {
    type: Object,
    default: () => ({})
  },
  isPlay: {
    type: Boolean,
    default: false
  }
})

const videoRef = ref(null)
const loading = ref(false)
const loaded = ref(false)
const playing = ref(false)
const isMuted = ref(false)  // 默认有声音
const autoplayBlocked = ref(false)  // 浏览器自动播放策略导致被静音
const currentTime = ref(0)
const duration = ref(0)
const isDraggingProgress = ref(false)
const wasPlayingBeforeDrag = ref(false)

let playToken = 0
let progressDragRAF = null
let unmuteClickHandler = null  // 自动播放被阻止时的一次性恢复声音监听

// Derive id and src from item prop
const videoId = computed(() => props.item?.id)
const videoSrc = computed(() => {
  const url = props.item?.url || ''
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return `/api/video/proxy?url=${encodeURIComponent(url)}`
  }
  return url
})
const thumbnail = computed(() => {
  return props.item?.thumbnail || props.item?.url?.replace('/videos/', '/images/').replace('.mp4', '.jpg') || ''
})

const progressPercent = computed(() => {
  if (duration.value <= 0) return 0
  return (currentTime.value / duration.value) * 100
})

const formatTime = (seconds) => {
  if (!seconds || !isFinite(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// 实际执行播放（尝试不静音，被阻止则回退静音）
const doPlay = (token) => {
  if (!videoRef.value || token !== playToken) return

  videoRef.value.play().catch(e => {
    if (token !== playToken) return
    // 浏览器阻止不静音自动播放时，回退到静音
    if (e.name === 'NotAllowedError' && !videoRef.value.muted) {
      videoRef.value.muted = true
      isMuted.value = true
      autoplayBlocked.value = true  // 标记为浏览器强制静音
      // 设置一次性交互监听：用户点击任意位置（音量按钮除外）自动恢复声音
      if (unmuteClickHandler) {
        document.removeEventListener('click', unmuteClickHandler, true)
      }
      unmuteClickHandler = (event) => {
        // 不干涉音量按钮点击，toggleMute 会自行处理
        if (event.target.closest('.sound-toggle')) return
        document.removeEventListener('click', unmuteClickHandler, true)
        unmuteClickHandler = null
        if (videoRef.value && playing.value) {
          videoRef.value.muted = false
          isMuted.value = false
          autoplayBlocked.value = false
        }
      }
      document.addEventListener('click', unmuteClickHandler, true)
      videoRef.value.play().catch(err => {
        if (token !== playToken) return
        console.error('播放失败:', err)
        loading.value = false
      })
    } else {
      console.error('播放失败:', e)
      loading.value = false
    }
  })
}

// Play video
const play = async () => {
  if (!videoRef.value) return

  const token = ++playToken
  videoRef.value.muted = isMuted.value
  loading.value = true

  // Reload if no src set yet
  if (!videoRef.value.src && videoSrc.value) {
    videoRef.value.src = videoSrc.value
  }

  const onErrorHandler = () => {
    if (token !== playToken) return
    loading.value = false
    console.error('视频加载失败')
  }

  videoRef.value.addEventListener('error', onErrorHandler, { once: true })

  if (videoRef.value.readyState < 2) {
    videoRef.value.load()
  }
  // 同步调用 doPlay，在用户手势上下文中立即尝试播放
  doPlay(token)
}

// Pause video
const pause = () => {
  videoRef.value?.pause()
  playing.value = false
}

// Stop and reset video
const stop = () => {
  if (videoRef.value) {
    videoRef.value.pause()
    videoRef.value.currentTime = 0
    videoRef.value.removeAttribute('src')
    videoRef.value.load()
    loaded.value = false
    playing.value = false
    loading.value = false
    currentTime.value = 0
    duration.value = 0
    isMuted.value = false  // 重置静音状态，下次播放时重新尝试非静音
    autoplayBlocked.value = false
  }
}

// Toggle play/pause
const toggle = () => {
  if (playing.value) {
    pause()
  } else {
    play()
  }
}

// Toggle mute/unmute
const toggleMute = () => {
  // 如果静音是浏览器自动播放策略导致的，点击音量按钮直接恢复声音
  if (autoplayBlocked.value && isMuted.value) {
    autoplayBlocked.value = false
    isMuted.value = false
  } else {
    isMuted.value = !isMuted.value
  }
  if (videoRef.value) {
    videoRef.value.muted = isMuted.value
  }
}

// Progress bar click (single click to seek)
const onProgressClick = (e) => {
  if (!videoRef.value || !duration.value) return
  const track = e.currentTarget.querySelector('.progress-bar-track')
  const rect = track.getBoundingClientRect()
  let percent = (e.clientX - rect.left) / rect.width
  percent = Math.max(0, Math.min(1, percent))
  const seekTime = percent * duration.value
  videoRef.value.currentTime = seekTime
  currentTime.value = seekTime
}

// Progress bar drag
const onProgressPointerDown = (e) => {
  if (!videoRef.value || !duration.value) return
  isDraggingProgress.value = true
  wasPlayingBeforeDrag.value = playing.value
  videoRef.value.pause()

  const track = e.currentTarget.querySelector('.progress-bar-track')
  const updateProgress = (clientX) => {
    const rect = track.getBoundingClientRect()
    let percent = (clientX - rect.left) / rect.width
    percent = Math.max(0, Math.min(1, percent))
    currentTime.value = percent * duration.value
  }

  updateProgress(e.clientX)

  const onMove = (ev) => {
    updateProgress(ev.clientX)
  }

  const onUp = () => {
    isDraggingProgress.value = false
    if (videoRef.value) {
      videoRef.value.currentTime = currentTime.value
      // 只有拖拽前在播放才恢复播放
      if (wasPlayingBeforeDrag.value) {
        videoRef.value.play().catch(() => {})
      }
    }
    document.removeEventListener('pointermove', onMove)
    document.removeEventListener('pointerup', onUp)
    document.removeEventListener('pointercancel', onUp)
  }

  document.addEventListener('pointermove', onMove)
  document.addEventListener('pointerup', onUp)
  document.addEventListener('pointercancel', onUp)
}

// Video event handlers
const onLoadStart = () => {
  loading.value = true
}

const onLoadedMetadata = () => {
  if (videoRef.value) {
    duration.value = videoRef.value.duration
  }
  loading.value = false
}

const onCanPlay = () => {
  loading.value = false
}

const onPlay = () => {
  playing.value = true
  loaded.value = true
  loading.value = false
}

const onPause = () => {
  playing.value = false
}

const onTimeUpdate = () => {
  if (!isDraggingProgress.value && videoRef.value) {
    currentTime.value = videoRef.value.currentTime
    duration.value = videoRef.value.duration || 0
  }
}

const onEnded = () => {
  if (videoRef.value) {
    videoRef.value.currentTime = 0
    videoRef.value.play().catch(() => {})
  }
}

const onError = () => {
  loading.value = false
  console.error('视频加载失败')
}

// Event bus handlers
const handleBroadcast = (data) => {
  if (data.id === videoId.value) {
    if (data.type === 'play') {
      play()
    } else {
      pause()
    }
  } else {
    pause()
  }
}

const handlePlay = (id) => {
  if (id === videoId.value) {
    play()
  } else {
    pause()
  }
}

const handleStop = (id) => {
  if (id === videoId.value) {
    stop()
  }
}

const handleToggle = (id) => {
  if (id === videoId.value) {
    toggle()
  }
}

// Watch isPlay prop
watch(() => props.isPlay, (newVal) => {
  if (newVal) {
    nextTick(() => play())
  } else {
    pause()
  }
})

watch(videoSrc, (newSrc) => {
  if (newSrc && videoRef.value) {
    videoRef.value.src = newSrc
    if (props.isPlay) {
      nextTick(() => play())
    }
  }
})

onMounted(() => {
  on(EVENT_KEY.SINGLE_CLICK_BROADCAST, handleBroadcast)
  on(EVENT_KEY.ITEM_PLAY, handlePlay)
  on(EVENT_KEY.ITEM_STOP, handleStop)
  on(EVENT_KEY.ITEM_TOGGLE, handleToggle)

  if (props.isPlay) {
    nextTick(() => play())
  }
})

onUnmounted(() => {
  off(EVENT_KEY.SINGLE_CLICK_BROADCAST, handleBroadcast)
  off(EVENT_KEY.ITEM_PLAY, handlePlay)
  off(EVENT_KEY.ITEM_STOP, handleStop)
  off(EVENT_KEY.ITEM_TOGGLE, handleToggle)

  if (unmuteClickHandler) {
    document.removeEventListener('click', unmuteClickHandler, true)
    unmuteClickHandler = null
  }

  if (progressDragRAF) {
    cancelAnimationFrame(progressDragRAF)
  }

  if (videoRef.value) {
    videoRef.value.pause()
    videoRef.value.src = ''
  }
})

defineExpose({ play, pause, stop, toggle })
</script>

<style scoped>
.base-video {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
  overflow: hidden;
}

.video-element {
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

.video-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 2;
}

.center-play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
  pointer-events: none;
  opacity: 0.8;
  width: 64px;
  height: 64px;
}

.center-play-icon svg {
  width: 100%;
  height: 100%;
}

/* Clickable center area for play/pause */
.center-click-area {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 60px; /* leave space for progress bar */
  z-index: 4;
}

/* Sound toggle */
.sound-toggle {
  position: absolute;
  bottom: 56px;
  right: 20px;
  z-index: 15;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 50%;
  cursor: pointer;
  transition: background 0.2s;
  pointer-events: auto;
}

.sound-toggle:hover {
  background: rgba(0, 0, 0, 0.6);
}

/* Progress bar */
.progress-bar-container {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
  padding: 0 16px 8px;
  cursor: pointer;
  touch-action: none;
}

.progress-bar-track {
  position: relative;
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: visible;
}

.progress-bar-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  transition: width 0.1s linear;
}

.progress-bar-thumb {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 14px;
  height: 14px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  opacity: 0;
  transition: opacity 0.2s;
}

.progress-bar-container:hover .progress-bar-thumb,
.progress-bar-container:active .progress-bar-thumb {
  opacity: 1;
}

.progress-bar-container:hover .progress-bar-track {
  height: 6px;
}

.progress-bar-container:hover .progress-bar-fill {
  background: #fe2c55;
}

.progress-time {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.progress-bar-container:hover .progress-time {
  opacity: 1;
}
</style>
