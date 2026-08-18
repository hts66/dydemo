<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import SessionExpiredDialog from './components/SessionExpiredDialog.vue'
import { startAuthSessionMonitor, stopAuthSessionMonitor } from './utils/authSession'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const sessionDialogOpen = ref(false)
const sessionExpiredReason = ref('expired')

const handleBeforeUnload = () => {
  sessionStorage.setItem('refreshFrom', route.path)
}

const handleSessionExpired = (event) => {
  userStore.logout()
  sessionExpiredReason.value = event.detail?.reason || 'expired'
  sessionDialogOpen.value = true
}

const handleTokenRefreshed = (event) => {
  const data = event.detail
  if (data?.token) userStore.token = data.token
  if (data?.refreshToken) userStore.refreshToken = data.refreshToken
  if (data?.user) userStore.user = data.user
}

const cancelRelogin = () => {
  sessionDialogOpen.value = false
}

const confirmRelogin = () => {
  sessionDialogOpen.value = false
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

onMounted(() => {
  userStore.loadUserFromStorage()
  window.addEventListener('auth-session-expired', handleSessionExpired)
  window.addEventListener('auth-token-refreshed', handleTokenRefreshed)
  startAuthSessionMonitor()

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

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('auth-session-expired', handleSessionExpired)
  window.removeEventListener('auth-token-refreshed', handleTokenRefreshed)
  stopAuthSessionMonitor()
})
</script>

<template>
  <router-view />
  <SessionExpiredDialog
    :open="sessionDialogOpen"
    :reason="sessionExpiredReason"
    @cancel="cancelRelogin"
    @confirm="confirmRelogin"
  />
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
