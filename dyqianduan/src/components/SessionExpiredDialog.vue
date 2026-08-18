<script setup>
defineProps({
  open: { type: Boolean, default: false },
  reason: { type: String, default: 'expired' },
})

defineEmits(['cancel', 'confirm'])
</script>

<template>
  <Teleport to="body">
    <Transition name="session-dialog">
      <div v-if="open" class="session-backdrop" role="presentation">
        <section
          class="session-card"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="session-dialog-title"
          aria-describedby="session-dialog-description"
        >
          <div class="session-status" aria-hidden="true">
            <span class="session-status-hand"></span>
          </div>
          <p class="session-eyebrow">SESSION ENDED</p>
          <h2 id="session-dialog-title">登录状态已过期</h2>
          <p id="session-dialog-description" class="session-description">
            {{ reason === 'idle' ? '由于您长时间未操作，请重新登录。' : '当前登录凭证已失效，请重新登录后继续操作。' }}
          </p>
          <div class="session-actions">
            <button class="session-button session-button-muted" type="button" @click="$emit('cancel')">
              暂不登录
            </button>
            <button class="session-button session-button-primary" type="button" autofocus @click="$emit('confirm')">
              重新登录
            </button>
          </div>
          <p class="session-footnote">暂不登录仍可浏览公开视频</p>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.session-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2147483647;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(4, 5, 10, 0.78);
  backdrop-filter: blur(12px);
}

.session-card {
  width: min(430px, 100%);
  padding: 34px 34px 26px;
  color: #f7f7fa;
  text-align: center;
  background: #171820;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.55), 0 0 0 1px rgba(254, 44, 85, 0.05);
}

.session-status {
  position: relative;
  width: 58px;
  height: 58px;
  margin: 0 auto 20px;
  border: 2px solid rgba(254, 44, 85, 0.76);
  border-radius: 50%;
  box-shadow: inset 0 0 22px rgba(254, 44, 85, 0.08), 0 0 26px rgba(37, 244, 238, 0.08);
}

.session-status::before,
.session-status-hand {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 2px;
  height: 16px;
  background: #25f4ee;
  border-radius: 999px;
  transform-origin: 50% 1px;
}

.session-status::before { transform: translate(-50%, -1px) rotate(180deg); }
.session-status-hand { transform: translate(-1px, -1px) rotate(128deg); }

.session-eyebrow {
  margin-bottom: 9px;
  color: #8d8f9b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.session-description {
  max-width: 320px;
  margin: 14px auto 25px;
  color: #b7b8c2;
  font-size: 14px;
  line-height: 1.7;
}

.session-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.session-button {
  min-height: 44px;
  padding: 0 18px;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 650;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 160ms ease, background-color 160ms ease, box-shadow 160ms ease;
}

.session-button:hover { transform: translateY(-1px); }
.session-button:focus-visible { outline: 2px solid #25f4ee; outline-offset: 3px; }
.session-button-muted { background: #292a34; }
.session-button-muted:hover { background: #343641; }
.session-button-primary { background: #fe2c55; box-shadow: 0 8px 22px rgba(254, 44, 85, 0.22); }
.session-button-primary:hover { background: #ff4165; }

.session-footnote { margin: 17px 0 0; color: #727480; font-size: 12px; }

.session-dialog-enter-active,
.session-dialog-leave-active { transition: opacity 180ms ease; }
.session-dialog-enter-active .session-card,
.session-dialog-leave-active .session-card { transition: transform 180ms ease, opacity 180ms ease; }
.session-dialog-enter-from,
.session-dialog-leave-to { opacity: 0; }
.session-dialog-enter-from .session-card,
.session-dialog-leave-to .session-card { opacity: 0; transform: translateY(10px) scale(0.98); }

@media (max-width: 480px) {
  .session-card { padding: 30px 22px 24px; }
  .session-actions { grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  .session-dialog-enter-active,
  .session-dialog-leave-active,
  .session-dialog-enter-active .session-card,
  .session-dialog-leave-active .session-card,
  .session-button { transition: none; }
}
</style>
