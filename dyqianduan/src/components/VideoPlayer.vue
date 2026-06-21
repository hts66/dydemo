<template>
  <div class="video-player-wrapper" @click="togglePlay">
    <video
      ref="videoRef"
      class="video-js vjs-big-play-centered"
      playsinline
      webkit-playsinline
      x5-playsinline
      preload="auto"
      :poster="thumbnail"
      :src="src"
    ></video>
    <div v-if="!isPlaying" class="play-overlay">
      <span class="play-icon">▶</span>
    </div>
    <div class="video-info" @click.stop>
      <div class="author-info">
        <img :src="avatar || defaultAvatar" class="author-avatar" />
        <span class="author-name">{{ authorName }}</span>
      </div>
      <h3 class="video-title">{{ title }}</h3>
      <p class="video-desc">{{ description }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  src: {
    type: String,
    required: true
  },
  thumbnail: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  authorName: {
    type: String,
    default: ''
  },
  avatar: {
    type: String,
    default: ''
  }
})

const videoRef = ref(null)
const isPlaying = ref(false)
const defaultAvatar = 'https://via.placeholder.com/40'

onMounted(() => {
  const video = videoRef.value
  if (video) {
    video.muted = true
    video.loop = true
  }
})

const togglePlay = () => {
  const video = videoRef.value
  if (!video) return
  if (video.paused) {
    video.play()
    isPlaying.value = true
  } else {
    video.pause()
    isPlaying.value = false
  }
}

defineExpose({
  play: () => {
    const video = videoRef.value
    if (video) {
      video.play()
      isPlaying.value = true
    }
  },
  pause: () => {
    const video = videoRef.value
    if (video) {
      video.pause()
      isPlaying.value = false
    }
  },
  getVideoElement: () => videoRef.value
})
</script>

<style scoped>
.video-player-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  cursor: pointer;
  overflow: hidden;
}

.video-player-wrapper video {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  background: rgba(0, 0, 0, 0.3);
  z-index: 2;
}

.play-icon {
  font-size: 60px;
  color: white;
  opacity: 0.8;
}

.video-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  color: white;
  z-index: 3;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
}

.video-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 4px;
}

.video-desc {
  font-size: 13px;
  opacity: 0.9;
  line-height: 1.4;
}
</style>
