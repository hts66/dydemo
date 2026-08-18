import axios from 'axios'

const IDLE_TIMEOUT_MS = 30 * 60 * 1000
const REFRESH_BEFORE_EXPIRY_MS = 5 * 60 * 1000
const ACTIVITY_THROTTLE_MS = 15 * 1000
const CHECK_INTERVAL_MS = 30 * 1000
const VIDEO_ACTIVITY_INTERVAL_MS = 60 * 1000
const LAST_ACTIVITY_KEY = 'authLastActivityAt'

let refreshPromise = null
let sessionTimer = null
let lastRecordedActivity = 0
let lastVideoActivity = 0
let expirationNotified = false

const hasSession = () => Boolean(localStorage.getItem('token'))

const decodeTokenPayload = (token) => {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    return JSON.parse(atob(padded))
  } catch (_) {
    return null
  }
}

const clearStoredSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  localStorage.removeItem(LAST_ACTIVITY_KEY)
}

export const notifySessionExpired = (reason = 'expired') => {
  if (expirationNotified) return
  expirationNotified = true
  clearStoredSession()
  window.dispatchEvent(new CustomEvent('auth-session-expired', { detail: { reason } }))
}

export const markAuthenticated = () => {
  expirationNotified = false
  const now = Date.now()
  lastRecordedActivity = now
  localStorage.setItem(LAST_ACTIVITY_KEY, String(now))
}

export const refreshAccessToken = async () => {
  if (refreshPromise) return refreshPromise

  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    notifySessionExpired('expired')
    throw new Error('没有可用的刷新令牌')
  }

  refreshPromise = axios
    .post('/api/auth/refresh', { refreshToken }, { timeout: 10000 })
    .then((response) => {
      const result = response.data
      if (result?.code !== 200 || !result.data?.token) {
        throw new Error(result?.message || '登录状态刷新失败')
      }
      localStorage.setItem('token', result.data.token)
      localStorage.setItem('refreshToken', result.data.refreshToken || refreshToken)
      if (result.data.user) localStorage.setItem('user', JSON.stringify(result.data.user))
      window.dispatchEvent(new CustomEvent('auth-token-refreshed', { detail: result.data }))
      return result.data.token
    })
    .catch((error) => {
      notifySessionExpired('expired')
      throw error
    })
    .finally(() => {
      refreshPromise = null
    })

  return refreshPromise
}

const refreshIfNeeded = () => {
  const token = localStorage.getItem('token')
  if (!token || !localStorage.getItem('refreshToken')) return
  const payload = decodeTokenPayload(token)
  if (!payload?.exp || payload.exp * 1000 - Date.now() <= REFRESH_BEFORE_EXPIRY_MS) {
    refreshAccessToken().catch(() => {})
  }
}

const checkIdleState = () => {
  if (!hasSession()) return
  const lastActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY))
  if (lastActivity && Date.now() - lastActivity >= IDLE_TIMEOUT_MS) notifySessionExpired('idle')
}

const recordActivity = (event) => {
  if (!hasSession()) return
  const now = Date.now()
  if (event?.type === 'timeupdate') {
    if (event.target?.paused || now - lastVideoActivity < VIDEO_ACTIVITY_INTERVAL_MS) return
    lastVideoActivity = now
  } else if (now - lastRecordedActivity < ACTIVITY_THROTTLE_MS) {
    return
  }

  const previousActivity = Number(localStorage.getItem(LAST_ACTIVITY_KEY))
  if (previousActivity && now - previousActivity >= IDLE_TIMEOUT_MS) {
    notifySessionExpired('idle')
    return
  }

  lastRecordedActivity = now
  localStorage.setItem(LAST_ACTIVITY_KEY, String(now))
  refreshIfNeeded()
}

export const startAuthSessionMonitor = () => {
  if (sessionTimer) return
  if (hasSession() && !localStorage.getItem(LAST_ACTIVITY_KEY)) markAuthenticated()
  ;['pointerdown', 'keydown', 'scroll', 'touchstart'].forEach((eventName) => {
    window.addEventListener(eventName, recordActivity, { passive: true })
  })
  document.addEventListener('timeupdate', recordActivity, true)
  document.addEventListener('visibilitychange', checkIdleState)
  sessionTimer = window.setInterval(checkIdleState, CHECK_INTERVAL_MS)
  checkIdleState()
  refreshIfNeeded()
}

export const stopAuthSessionMonitor = () => {
  if (!sessionTimer) return
  ;['pointerdown', 'keydown', 'scroll', 'touchstart'].forEach((eventName) => {
    window.removeEventListener(eventName, recordActivity)
  })
  document.removeEventListener('timeupdate', recordActivity, true)
  document.removeEventListener('visibilitychange', checkIdleState)
  window.clearInterval(sessionTimer)
  sessionTimer = null
}

export const clearAuthSession = clearStoredSession
