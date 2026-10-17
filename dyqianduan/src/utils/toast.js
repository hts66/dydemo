// 全局轻量 Toast 提示
// 用法: showToast('该视频不存在')

let toastEl = null
let hideTimer = null

export function showToast(message, duration = 2000) {
  if (!toastEl) {
    toastEl = document.createElement('div')
    toastEl.className = 'global-toast'
    toastEl.style.cssText = `
      position: fixed;
      bottom: 120px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(0, 0, 0, 0.75);
      color: #fff;
      padding: 10px 20px;
      border-radius: 8px;
      font-size: 14px;
      z-index: 9999;
      pointer-events: none;
      opacity: 0;
      transition: opacity 0.3s ease;
      max-width: 80vw;
      text-align: center;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    `
    document.body.appendChild(toastEl)
  }

  toastEl.textContent = message
  toastEl.style.opacity = '1'

  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    toastEl.style.opacity = '0'
  }, duration)
}
