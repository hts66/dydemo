/**
 * 更新已上传抖音视频的标题
 *
 * 用法: node update_douyin.mjs
 *
 * 从 douyin_downloads/oss_urls.json 或 video_data.json 读取视频数据，
 * 通过 URL 模糊匹配找到数据库中的 works 记录，更新 title 和 description。
 */

import fs from 'fs'
import path from 'path'
import mysql from 'mysql2/promise'

// ========== 配置 ==========
const DOWNLOAD_DIR = path.resolve('./douyin_downloads')

const DB_CONFIG = {
  host: 'localhost',
  port: 3306,
  user: 'hts',
  password: 'HTshun666!',
  database: 'dy',
}

// ========== 读取数据源 ==========

// 优先读 oss_urls.json (包含 videoUrl → title 映射)
const ossFile = path.join(DOWNLOAD_DIR, 'oss_urls.json')
const rawFile = path.join(DOWNLOAD_DIR, 'video_data.json')

let videoList = []

if (fs.existsSync(ossFile)) {
  videoList = JSON.parse(fs.readFileSync(ossFile, 'utf-8'))
  console.log(`从 oss_urls.json 读取 ${videoList.length} 条记录`)
} else if (fs.existsSync(rawFile)) {
  videoList = JSON.parse(fs.readFileSync(rawFile, 'utf-8'))
  console.log(`从 video_data.json 读取 ${videoList.length} 条记录`)
} else {
  console.error('未找到 douyin_downloads/oss_urls.json 或 video_data.json')
  console.error('请先运行 douyin_scraper.mjs 抓取数据并运行 upload_douyin.mjs 上传')
  process.exit(1)
}

// 只保留有标题的
const withTitle = videoList.filter(v => v.title && v.title.trim())
console.log(`其中有标题的 ${withTitle.length} 个\n`)

// ========== 更新数据库 ==========

async function main() {
  const conn = await mysql.createConnection(DB_CONFIG)

  let updated = 0
  let skipped = 0

  for (let i = 0; i < withTitle.length; i++) {
    const v = withTitle[i]
    const title = v.title.trim().substring(0, 99)
    const desc = v.title.trim().substring(0, 499)
    const num = `[${i + 1}/${withTitle.length}]`

    // 通过 URL 模糊匹配找 work：用 videoId 匹配 url 字段
    const [rows] = await conn.execute(
      'SELECT id, title FROM works WHERE url LIKE ? LIMIT 1',
      [`%${v.videoId}%`]
    )

    if (rows.length === 0) {
      console.log(`${num} ⚠ 未找到 videoId=${v.videoId}`)
      skipped++
      continue
    }

    const work = rows[0]
    // 如果已有标题且不同，跳过（可选：覆盖模式去掉这个判断）
    if (work.title && work.title !== title) {
      // 已有标题 → 更新
    }

    await conn.execute(
      'UPDATE works SET title = ?, description = ? WHERE id = ?',
      [title, desc, work.id]
    )
    console.log(`${num} ✓ [id=${work.id}] ${title.substring(0, 30)}`)
    updated++
  }

  await conn.end()

  console.log(`\n═══════════════════════════`)
  console.log(`  更新完成!`)
  console.log(`  已更新: ${updated} 个`)
  console.log(`  未找到: ${skipped} 个`)
  console.log(`═══════════════════════════\n`)
}

main().catch(err => {
  console.error('执行失败:', err.message)
  process.exit(1)
})
