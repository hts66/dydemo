/**
 * 媒体地址统一出口。
 *
 * 库里存的是 MinIO 绝对地址（http://localhost:9000/dydemo/videos/xxx.mp4），
 * 这里转成同源相对路径 /media/videos/xxx.mp4，由 vite（开发）或 nginx（生产）
 * 反代到对象存储，浏览器直连，后端不参与媒体传输。
 *
 * 换域名、换 OSS、挂 CDN 时只改 MEDIA_BASE。
 */
const MEDIA_BASE = '/media/'

/** MinIO 里的对象地址，形如 http(s)://<host>/dydemo/<key> */
const MINIO_OBJECT = /^https?:\/\/[^/]+\/dydemo\/(.+)$/

/**
 * @param {string} raw 数据库里存的原始地址
 * @returns {string} 可直接放进 :src / :poster 的地址
 */
export function mediaUrl(raw) {
  if (!raw) return ''
  const m = raw.match(MINIO_OBJECT)
  // 非 MinIO 的外站地址原样返回：img/video 不加 crossorigin 时不触发 CORS 检查
  return m ? MEDIA_BASE + m[1] : raw
}
