import { computed, onBeforeUnmount, ref } from 'vue'

const DEFAULT_COOLDOWN_SECONDS = 60

export const getRetryAfterSeconds = (error: any) => {
  const retryAfter = Number(
    error?.response?.data?.data?.retryAfter ??
      error?.response?.data?.retryAfter,
  )

  return Number.isFinite(retryAfter) && retryAfter > 0
    ? Math.ceil(retryAfter)
    : 0
}

export const useVerificationCodeCooldown = (
  defaultSeconds = DEFAULT_COOLDOWN_SECONDS,
) => {
  const sending = ref(false)
  const codeCountdown = ref(0)
  const codeBtnDisabled = computed(
    () => sending.value || codeCountdown.value > 0,
  )

  let countdownTimer: ReturnType<typeof setInterval> | undefined

  const stopTimer = () => {
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }

  const startCooldown = (seconds = defaultSeconds) => {
    stopTimer()

    const safeSeconds = Math.max(1, Math.ceil(seconds))
    const endAt = Date.now() + safeSeconds * 1000
    codeCountdown.value = safeSeconds

    countdownTimer = setInterval(() => {
      codeCountdown.value = Math.max(
        0,
        Math.ceil((endAt - Date.now()) / 1000),
      )

      if (codeCountdown.value === 0) stopTimer()
    }, 250)
  }

  const beginSending = () => {
    // This guard also blocks direct/programmatic calls before Vue updates the DOM.
    if (codeBtnDisabled.value) return false

    sending.value = true
    startCooldown()
    return true
  }

  const markSent = () => {
    sending.value = false
  }

  const markFailed = (retryAfter = 0) => {
    sending.value = false

    if (retryAfter > 0) {
      startCooldown(retryAfter)
      return
    }

    stopTimer()
    codeCountdown.value = 0
  }

  onBeforeUnmount(stopTimer)

  return {
    codeBtnDisabled,
    codeCountdown,
    sending,
    beginSending,
    markSent,
    markFailed,
  }
}
