<template>
  <div
    ref="containerRef"
    class="slide-container"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
  >
    <!-- Slide wrapper with CSS transform for animation -->
    <div
      class="slide-wrapper"
      :style="wrapperStyle"
    >
      <div
        v-for="item in visibleItems"
        :key="item[uniqueId] || item.id"
        class="slide-item"
        :data-real-index="item._realIndex"
      >
        <slot
          :item="item"
          :index="item._realIndex"
          :isActive="item._realIndex === currentIndex"
        />
      </div>
    </div>

    <!-- Top pull-refresh indicator -->
    <div v-if="isPullingDown && currentIndex === 0" class="pull-indicator top" :style="{ height: pullDistance + 'px' }">
      <div class="pull-content">
        <div class="pull-spinner" v-if="isRefreshing"></div>
        <svg v-else viewBox="0 0 24 24" width="24" height="24" fill="#fff" :style="{ transform: `rotate(${pullDistance > 60 ? 180 : 0}deg)` }">
          <path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/>
        </svg>
        <span>{{ pullDistance > 60 ? '释放刷新' : '下拉刷新' }}</span>
      </div>
    </div>

    <!-- Bottom load-more indicator -->
    <div v-if="isLoadingMore" class="load-more-indicator">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- No more data -->
    <div v-if="!hasMore && list.length > 0 && currentIndex >= list.length - 2" class="no-more-indicator">
      <span>— 没有更多了 —</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { emit, EVENT_KEY } from '../utils/bus'

const props = defineProps({
  list: {
    type: Array,
    default: () => []
  },
  active: {
    type: Number,
    default: 0
  },
  uniqueId: {
    type: String,
    default: 'id'
  },
  // How many items to render (odd number recommended, centered on current)
  virtualTotal: {
    type: Number,
    default: 5
  },
  // Whether there's more data to load
  hasMore: {
    type: Boolean,
    default: true
  },
  // Whether currently loading more
  isLoadingMore: {
    type: Boolean,
    default: false
  },
  // Whether currently refreshing
  isRefreshing: {
    type: Boolean,
    default: false
  }
})

const emitEvents = defineEmits(['update:index', 'loadMore', 'refresh'])

// ── Refs ──────────────────────────────────────────────
const containerRef = ref(null)
const currentIndex = ref(props.active)
const offsetY = ref(0)
const isAnimating = ref(false)

// Pointer state
const pointerStartY = ref(0)
const pointerStartX = ref(0)
const pointerCurrentY = ref(0)
const pointerStartTime = ref(0)
const isDragging = ref(false)
const hasMoved = ref(false)
const pullDistance = ref(0)
const isPullingDown = ref(false)

let animationRAF = null
let lastWheelTime = 0
const WHEEL_COOLDOWN = 600 // 滚轮切换冷却时间(ms)

// ★ 计算当前视频在 wrapper 中的正确偏移量
//    visibleItems 从 localStart 开始，当前视频位于 (currentIndex - localStart) * sh 的位置
//    例如 currentIndex=3, localStart=1 → 视频3在wrapper的第2个位置 → offsetY = -(3-1)*sh = -2sh
const correctOffset = () => -(currentIndex.value - localStart.value) * screenHeight()

// ── Virtual list computation ──────────────────────────
const halfVirtual = computed(() => Math.floor(props.virtualTotal / 2))

const localStart = computed(() => {
  const total = props.virtualTotal
  let start = currentIndex.value - halfVirtual.value
  if (start < 0) start = 0
  const maxStart = Math.max(0, props.list.length - total)
  if (start > maxStart) start = maxStart
  return start
})

const visibleItems = computed(() => {
  if (!props.list.length) return []
  const start = localStart.value
  const end = start + props.virtualTotal
  return props.list.slice(start, end).map((item, i) => ({
    ...item,
    _realIndex: start + i
  }))
})

// CSS transform for the slide wrapper
const wrapperStyle = computed(() => ({
  transform: `translateY(${offsetY.value}px)`,
  transition: isAnimating.value ? 'transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94)' : 'none'
}))

// ── Gesture: Pointer Events ───────────────────────────
const screenHeight = () => window.innerHeight

const onPointerDown = (e) => {
  if (isAnimating.value) return

  pointerStartY.value = e.clientY
  pointerStartX.value = e.clientX
  pointerCurrentY.value = e.clientY
  pointerStartTime.value = Date.now()
  isDragging.value = true
  hasMoved.value = false
  pullDistance.value = 0
  isPullingDown.value = false

  if (animationRAF) {
    cancelAnimationFrame(animationRAF)
    animationRAF = null
  }
}

const onPointerMove = (e) => {
  if (!isDragging.value || isAnimating.value) return

  const deltaY = e.clientY - pointerStartY.value
  const deltaX = e.clientX - pointerStartX.value

  // Detect horizontal scroll: if horizontal exceeds vertical early, cancel
  if (!hasMoved.value && Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
    isDragging.value = false
    return
  }

  // Small dead zone (prevent click-jitter from being treated as swipe)
  if (Math.abs(deltaY) < 10) return

  hasMoved.value = true
  e.preventDefault()

  pointerCurrentY.value = e.clientY

  // Pull-down refresh at first item
  if (currentIndex.value === 0 && deltaY > 0) {
    isPullingDown.value = true
    pullDistance.value = Math.min(deltaY * 0.4, 100)
    // ★ 基于当前正确位置 + 下拉距离（currentIndex=0时correctOffset()=0，等价）
    offsetY.value = correctOffset() + pullDistance.value
    return
  }

  // ★ 关键修复：拖拽位移必须基于当前视频的正确位置叠加
  //    例如在视频2：correctOffset = -2sh，向上拖200px → offset = -2sh-200
  //    如果直接用deltaY会导致画面跳到视频0附近，表现为"向上滑却向下走"
  offsetY.value = correctOffset() + deltaY
}

const onPointerUp = (_e) => {
  if (!isDragging.value || isAnimating.value) {
    isDragging.value = false
    return
  }

  isDragging.value = false

  if (!hasMoved.value) {
    // ★ 纯点击无拖拽 → 不做任何位置变化，保持当前视频
    //    不能用 offsetY=0，因为新系统中 offsetY 可能为 -100vh 等非零值
    //    点击只触发视频暂停/播放（由 BaseVideo 的 click 事件处理）
    return
  }

  const deltaY = pointerCurrentY.value - pointerStartY.value
  const distance = Math.abs(deltaY)
  const direction = deltaY > 0 ? 'up' : 'down' // up = swipe down (next), down = swipe up (prev)
  const elapsedTime = Date.now() - pointerStartTime.value
  const sh = screenHeight()

  // Pull refresh: release at top with enough distance
  if (isPullingDown.value && currentIndex.value === 0) {
    // ★ 下拉刷新后回弹到当前视频位置（不是硬编码的0）
    if (pullDistance.value > 60) {
      animateOffsetTo(correctOffset(), () => {
        pullDistance.value = 0
        isPullingDown.value = false
        emitEvents('refresh')
      })
    } else {
      animateOffsetTo(correctOffset(), () => {
        pullDistance.value = 0
        isPullingDown.value = false
      })
    }
    return
  }

  // Threshold: swipe distance > 1/3 screen OR swipe time < 150ms
  const distanceThreshold = sh / 3
  const isFastSwipe = elapsedTime < 200 && distance > 60
  const isLongSwipe = distance > distanceThreshold
  const isSuccess = isFastSwipe || isLongSwipe

  if (isSuccess) {
    // deltaY > 0 means finger moved down = next item (index - 1, i.e. direction 'up' in our terms)
    // deltaY < 0 means finger moved up = previous item (index + 1)
    const targetDirection = deltaY > 0 ? -1 : 1
    switchToIndex(currentIndex.value + targetDirection)
  } else {
    // ★ 滑动不够 → 回弹到当前视频（不是视频0）
    animateOffsetTo(correctOffset())
  }
}

// ── Wheel event for desktop ───────────────────────────
const onWheel = (e) => {
  e.preventDefault()
  if (isAnimating.value) return

  const now = Date.now()
  if (now - lastWheelTime < WHEEL_COOLDOWN) return
  lastWheelTime = now

  const direction = e.deltaY > 0 ? 1 : -1
  switchToIndex(currentIndex.value + direction)
}

// ── Switch video ──────────────────────────────────────
const switchToIndex = (targetIndex) => {
  if (isAnimating.value) return

  // ★ 立刻锁定滚轮，防止触控板同时发送 pointer + wheel 导致双击跳
  lastWheelTime = Date.now()

  // ★ 边界：已在顶部，不能再往上
  if (targetIndex < 0) {
    animateOffsetTo(correctOffset())
    return
  }

  // ★ 边界：已在底部，不能再往下（触发加载更多）
  if (targetIndex >= props.list.length) {
    animateOffsetTo(correctOffset())
    if (props.hasMore) {
      emitEvents('loadMore')
    }
    return
  }

  isAnimating.value = true

  const sh = screenHeight()
  const oldLocalStart = localStart.value

  // 从当前位置开始动画（不是从0）
  const currentOffset = -(currentIndex.value - oldLocalStart) * sh
  const targetOffset = -(targetIndex - oldLocalStart) * sh

  offsetY.value = currentOffset
  nextTick(() => {
    offsetY.value = targetOffset
  })

  // After transition duration, reset position and update index
  setTimeout(() => {
    isAnimating.value = false
    lastWheelTime = Date.now()  // 阻止动画后的惯性滚轮事件

    // 先停止当前视频
    const prevItem = props.list[currentIndex.value]
    if (prevItem) {
      emit(EVENT_KEY.ITEM_STOP, prevItem.id)
    }

    currentIndex.value = targetIndex

    // ★ 设置wrapper偏移到当前视频的正确位置
    offsetY.value = correctOffset()

    // Emit index update for v-model
    emitEvents('update:index', targetIndex)

    // 再播放新视频
    const item = props.list[targetIndex]
    if (item) {
      nextTick(() => {
        emit(EVENT_KEY.SINGLE_CLICK_BROADCAST, { id: item.id, type: 'play' })
      })
      emit(EVENT_KEY.CURRENT_ITEM, { index: targetIndex, item })
    }

    // Prefetch: load more when approaching end
    if (targetIndex >= props.list.length - halfVirtual.value - 1) {
      if (props.hasMore) {
        emitEvents('loadMore')
      }
    }
  }, 300) // matches CSS transition duration
}

// ── Spring-back animation ─────────────────────────────
const animateOffsetTo = (target, callback) => {
  if (animationRAF) {
    cancelAnimationFrame(animationRAF)
    animationRAF = null
  }

  const startOffset = offsetY.value
  const diff = target - startOffset
  const duration = 250
  const startTime = performance.now()

  const animate = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    // Ease-out cubic
    const eased = 1 - Math.pow(1 - progress, 3)
    offsetY.value = startOffset + diff * eased

    if (progress < 1) {
      animationRAF = requestAnimationFrame(animate)
    } else {
      animationRAF = null
      offsetY.value = target
      if (callback) callback()
    }
  }

  animationRAF = requestAnimationFrame(animate)
}

// ── Play current video ────────────────────────────────
const playCurrentVideo = () => {
  const item = props.list[currentIndex.value]
  if (item) {
    emit(EVENT_KEY.SINGLE_CLICK_BROADCAST, { id: item.id, type: 'play' })
  }
}

const stopCurrentVideo = () => {
  const item = props.list[currentIndex.value]
  if (item) {
    emit(EVENT_KEY.ITEM_STOP, item.id)
  }
}

// ── Watchers ──────────────────────────────────────────
watch(() => props.active, (newVal) => {
  if (newVal !== currentIndex.value) {
    currentIndex.value = newVal
  }
})

watch(() => props.list.length, () => {
  // If list was empty and now has items, play first
  nextTick(() => {
    if (props.list.length > 0 && currentIndex.value === 0) {
      playCurrentVideo()
    }
  })
})

watch(currentIndex, (newIndex) => {
  const item = props.list[newIndex]
  if (item) {
    emit(EVENT_KEY.CURRENT_ITEM, { index: newIndex, item })
  }
})

// ── Lifecycle ─────────────────────────────────────────
onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('wheel', onWheel, { passive: false })
  }

  if (props.list.length > 0) {
    const item = props.list[currentIndex.value]
    if (item) {
      emit(EVENT_KEY.CURRENT_ITEM, { index: currentIndex.value, item })
    }
    nextTick(() => playCurrentVideo())
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('wheel', onWheel)
  }

  stopCurrentVideo()

  if (animationRAF) {
    cancelAnimationFrame(animationRAF)
  }
})

// ── Expose ────────────────────────────────────────────
defineExpose({
  currentIndex,
  switchToIndex,
  playCurrentVideo,
  stopCurrentVideo
})
</script>

<style scoped>
.slide-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
  background: #000;
  touch-action: none;
  user-select: none;
  -webkit-user-select: none;
}

.slide-wrapper {
  position: relative;
  will-change: transform;
}

.slide-item {
  width: 100%;
  height: 100vh;
  position: relative;
  background: #000;
  overflow: hidden;
}

/* Pull refresh indicator */
.pull-indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 16px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.8), transparent);
  overflow: hidden;
  pointer-events: none;
}

.pull-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.pull-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* Load more indicator */
.load-more-indicator {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 100;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
}

.no-more-indicator {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
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
