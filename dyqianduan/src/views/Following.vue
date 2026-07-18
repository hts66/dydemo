<template>
  <div class="recommend-container">
    <SlideList api-type="following" />
  </div>
</template>

<script setup>
import SlideList from '../components/SlideList.vue'
</script>

<style scoped>
.recommend-container {
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #000;
}
</style>
