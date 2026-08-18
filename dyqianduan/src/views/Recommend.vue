<template>
  <div class="recommend-container">
    <SlideList ref="slideListRef" api-type="recommend" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import SlideList from '../components/SlideList.vue'

const slideListRef = ref<InstanceType<typeof SlideList> | null>(null)

onMounted(() => {})

onUnmounted(() => {})
</script>

<style scoped>
.recommend-container {
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #000;
}
</style>