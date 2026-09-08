// WebSocket 实时聊天连接管理：心跳保活 + 断线自动重连
// 接收推送通过 onWsMessage 注册回调；发送消息仍走 HTTP（复用落库逻辑）

let ws = null
let reconnectTimer = null
let heartbeatTimer = null
const listeners = new Set()

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send('ping')
    }
  }, 30000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

export function connectWs() {
  const token = localStorage.getItem('token')
  if (!token) return
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${protocol}://${location.host}/api/ws/chat?token=${token}`)

  ws.onopen = () => {
    startHeartbeat()
    // 通知上层连接已建立（用于断线重连后补拉消息）
    listeners.forEach((fn) => {
      try { fn({ type: 'ws_open' }) } catch (e) { console.error(e) }
    })
  }

  ws.onmessage = (event) => {
    if (event.data === 'pong') return
    try {
      const data = JSON.parse(event.data)
      listeners.forEach((fn) => {
        try { fn(data) } catch (e) { console.error(e) }
      })
    } catch (_) {
      // 忽略非 JSON 消息
    }
  }

  ws.onclose = () => {
    stopHeartbeat()
    // 通知上层连接已断开（用于界面显示重连状态）
    listeners.forEach((fn) => {
      try { fn({ type: 'ws_close' }) } catch (e) { console.error(e) }
    })
    // 3 秒后自动重连
    if (!reconnectTimer) {
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null
        connectWs()
      }, 3000)
    }
  }

  ws.onerror = () => {
    try { ws && ws.close() } catch (_) { /* 已关闭 */ }
  }
}

/**
 * 注册消息监听回调
 * @returns 取消监听函数
 */
export function onWsMessage(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
