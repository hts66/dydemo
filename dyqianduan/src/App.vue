<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const handleBeforeUnload = () => {
  sessionStorage.setItem('refreshFrom', route.path)
}

onMounted(() => {
  userStore.loadUserFromStorage()

  // 如果在推荐界面刷新浏览器，自动跳转到精选界面
  const refreshFrom = sessionStorage.getItem('refreshFrom')
  sessionStorage.removeItem('refreshFrom')
  if (refreshFrom === '/recommend') {
    const navEntry = performance.getEntriesByType('navigation')[0]
    if (navEntry && navEntry.type === 'reload') {
      router.replace('/featured')
    }
  }

  window.addEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
