/**
 * 批量上传已下载的抖音视频到 MinIO 并发布
 * 用法: node upload_douyin.mjs [userId]
 *   userId: 发布者用户ID，默认为 13
 */
import fs from 'fs'
import path from 'path'
import { execSync } from 'child_process'

const DOWNLOAD_DIR = './douyin_downloads'
const API = 'http://localhost:8080'
const EMAIL = '2703605029@qq.com'
const USER_ID = parseInt(process.argv[2]) || 13

// 读取视频元数据
const rawData = JSON.parse(fs.readFileSync(path.join(DOWNLOAD_DIR, 'video_data.json'), 'utf-8'))
console.log(`共 ${rawData.length} 个视频待上传\n`)

const results = []

for (let i = 0; i < rawData.length; i++) {
  const v = rawData[i]
  const num = `[${i + 1}/${rawData.length}]`
  // 标题：保留原始标题，空则置空（不在前端显示"无标题"）
  const title = (v.title || '').trim().substring(0, 99)
  const display = title || '(无标题)'

  console.log(`${num} ${display}`)

  // 上传视频
  const videoFile = path.join(DOWNLOAD_DIR, `${v.videoId}.mp4`)
  let videoUrl = ''

  if (fs.existsSync(videoFile)) {
    try {
      // Windows 上优先使用 curl.exe，避免 PowerShell 的 curl 别名 (Invoke-WebRequest)
      const curlCmd = process.platform === 'win32' ? 'curl.exe' : 'curl'
      const absPath = path.resolve(videoFile)
      const cmd = `${curlCmd} -s -X POST ${API}/api/upload/video -F "file=@${absPath}"`
      const raw = execSync(cmd, { encoding: 'utf-8', windowsHide: true })
      const res = JSON.parse(raw)
      if (res.code === 200) {
        videoUrl = res.data
        console.log(`  视频: ✓ ${videoUrl.substring(0, 60)}...`)
      } else {
        console.log(`  视频: ✗ ${res.message || JSON.stringify(res)}`)
      }
    } catch (e) {
      console.log(`  视频: ✗ ${e.message}`)
      if (e.stderr) console.log(`  详情: ${e.stderr}`)
    }
  }

  // 上传封面
  const coverFile = path.join(DOWNLOAD_DIR, `${v.videoId}_cover.jpg`)
  let coverUrl = v.cover || ''

  if (fs.existsSync(coverFile)) {
    try {
      const curlCmd = process.platform === 'win32' ? 'curl.exe' : 'curl'
      const absPath = path.resolve(coverFile)
      const cmd = `${curlCmd} -s -X POST ${API}/api/upload/image -F "file=@${absPath}"`
      const res = JSON.parse(execSync(cmd, { encoding: 'utf-8', windowsHide: true }))
      if (res.code === 200) {
        coverUrl = res.data
        console.log(`  封面: ✓`)
      }
    } catch (_) {}
  }

  results.push({ videoId: v.videoId, title: title, videoUrl, coverUrl })
}

// 保存上传结果
const uploadResultFile = path.join(DOWNLOAD_DIR, 'oss_urls.json')
fs.writeFileSync(uploadResultFile, JSON.stringify(results, null, 2))
console.log(`\n上传完成! ${results.filter(r => r.videoUrl).length}/${results.length} 成功`)
console.log(`结果: ${uploadResultFile}`)

// 生成可直接导入的SQL（无标题则不填假标题）
const sqlLines = ['-- 抖音视频种子数据', 'INSERT INTO works (user_id, type, url, thumbnail, title, description, likes_count, comments_count, views) VALUES']
const valueLines = results.filter(r => r.videoUrl).map((r, i) => {
  const url = r.videoUrl.replace(/'/g, "\\'")
  const thumb = (r.coverUrl || '').replace(/'/g, "\\'")
  const title = (r.title || '').replace(/'/g, "\\'").substring(0, 99)
  // description 使用标题内容作为视频文案
  const desc = (r.title || '').replace(/'/g, "\\'").substring(0, 499)
  return `(${USER_ID}, 2, '${url}', '${thumb}', '${title}', '${desc}', ${Math.floor(Math.random()*50)}, ${Math.floor(Math.random()*10)}, ${Math.floor(Math.random()*500)})`
})
sqlLines.push(valueLines.join(',\n') + ';')

const sqlFile = path.join(DOWNLOAD_DIR, 'import.sql')
fs.writeFileSync(sqlFile, sqlLines.join('\n'))
console.log(`SQL文件: ${sqlFile}`)
console.log('\n执行导入: mysql -u hts -pHTshun666! -h localhost --default-character-set=utf8mb4 dy < douyin_downloads/import.sql')
