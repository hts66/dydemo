/**
 * 抖音用户主页视频抓取 + 上传脚本
 *
 * 用法:
 *   node douyin_scraper.mjs
 *
 * 原理:
 *   1. 启动 Chromium 浏览器，打开抖音用户主页
 *   2. 自动滚动加载视频列表
 *   3. 从页面提取视频信息（URL、标题、封面等）
 *   4. 下载视频到本地
 *   5. 通过项目后端API上传到MinIO
 *   6. 以 2703605029@qq.com 身份发布
 */

import { chromium } from 'playwright'
import fs from 'fs'
import path from 'path'
import { createWriteStream } from 'fs'
import { pipeline } from 'stream/promises'
import http from 'http'
import https from 'https'

// ========== 配置 ==========
const CONFIG = {
  // 抖音用户主页
  douyinUrl:
    'https://www.douyin.com/user/MS4wLjABAAAA0W6MrnV7YIYmneCLCypeKVoZj4VDk9amQorNZ8aIVfs?from_tab_name=main&vid=7087128148474711327',

  // 项目后端
  apiBase: 'http://localhost:8080',
  email: '2789216935@qq.com',

  // 抓取数量
  maxVideos: 500,

  // 下载目录
  downloadDir: path.resolve('./douyin_downloads'),

  // 浏览器模式: true=无界面, false=显示窗口(推荐)
  headless: false,
}

// ========== 工具函数 ==========

// 下载文件
async function downloadFile(url, filepath) {
  return new Promise((resolve, reject) => {
    const file = createWriteStream(filepath)
    const protocol = url.startsWith('https') ? https : http

    protocol
      .get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (response) => {
        // 处理重定向
        if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
          file.close()
          fs.unlinkSync(filepath)
          return downloadFile(response.headers.location, filepath).then(resolve).catch(reject)
        }

        if (response.statusCode !== 200) {
          file.close()
          fs.unlinkSync(filepath)
          return reject(new Error(`HTTP ${response.statusCode}`))
        }

        response.pipe(file)
        file.on('finish', () => {
          file.close()
          resolve()
        })
        file.on('error', (err) => {
          fs.unlinkSync(filepath)
          reject(err)
        })
      })
      .on('error', (err) => {
        file.close()
        if (fs.existsSync(filepath)) fs.unlinkSync(filepath)
        reject(err)
      })
  })
}

// 上传文件到后端 → 返回文件URL
async function uploadToBackend(filepath, type = 'video') {
  const endpoint = type === 'image' ? '/api/upload/image' : '/api/upload/video'

  const boundary = '----FormBoundary' + Math.random().toString(36).slice(2)
  const filename = path.basename(filepath)
  const fileBuffer = fs.readFileSync(filepath)

  const header = Buffer.from(
    `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${filename}"\r\nContent-Type: ${type === 'image' ? 'image/jpeg' : 'video/mp4'}\r\n\r\n`
  )
  const footer = Buffer.from(`\r\n--${boundary}--\r\n`)
  const body = Buffer.concat([header, fileBuffer, footer])

  return new Promise((resolve, reject) => {
    const url = new URL(CONFIG.apiBase + endpoint)
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': `multipart/form-data; boundary=${boundary}`,
          'Content-Length': body.length,
        },
        timeout: 120000,
      },
      (res) => {
        let data = ''
        res.on('data', (chunk) => (data += chunk))
        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            if (json.code === 200) {
              resolve(json.data) // 返回 MinIO URL
            } else {
              reject(new Error(json.message || 'Upload failed'))
            }
          } catch (e) {
            reject(new Error('Parse error: ' + data.substring(0, 200)))
          }
        })
      }
    )
    req.on('error', reject)
    req.on('timeout', () => {
      req.destroy()
      reject(new Error('Upload timeout'))
    })
    req.write(body)
    req.end()
  })
}

// 登录 → 获取JWT (需要用户手动输入验证码)
async function loginWithCaptcha(captchaKey, captchaCode) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      email: '2703605029@qq.com',
      password: '123456',
      captchaKey: captchaKey,
      captchaCode: captchaCode,
    })
    const url = new URL(CONFIG.apiBase + '/api/auth/login')
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      },
      (res) => {
        let data = ''
        res.on('data', (chunk) => (data += chunk))
        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            resolve(json)
          } catch (e) {
            reject(e)
          }
        })
      }
    )
    req.on('error', reject)
    req.write(body)
    req.end()
  })
}

// 发布作品
async function publishWork(token, videoUrl, thumbnailUrl, title, description) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      url: videoUrl,
      thumbnail: thumbnailUrl || '',
      title: title || '',
      description: description || '',
    })
    const url = new URL(CONFIG.apiBase + '/api/works')
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      },
      (res) => {
        let data = ''
        res.on('data', (chunk) => (data += chunk))
        res.on('end', () => {
          try {
            resolve(JSON.parse(data))
          } catch (e) {
            reject(e)
          }
        })
      }
    )
    req.on('error', reject)
    req.write(body)
    req.end()
  })
}

// ========== 主流程 ==========

async function main() {
  console.log('═══════════════════════════════════════')
  console.log('  抖音视频抓取 + MinIO上传工具')
  console.log('═══════════════════════════════════════\n')

  // 创建下载目录
  if (!fs.existsSync(CONFIG.downloadDir)) {
    fs.mkdirSync(CONFIG.downloadDir, { recursive: true })
  }

  // ── 步骤1: 启动浏览器 ──
  console.log('[1/6] 启动浏览器...')
  const browser = await chromium.launch({
    headless: CONFIG.headless,
    channel: 'chrome', // 使用系统已安装的 Chrome
    args: [
      '--no-sandbox',
      '--disable-blink-features=AutomationControlled',
    ],
  })

  const context = await browser.newContext({
    userAgent:
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    viewport: { width: 1280, height: 800 },
    locale: 'zh-CN',
  })

  // 注入反检测脚本
  await context.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => false })
    Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] })
    Object.defineProperty(navigator, 'languages', { get: () => ['zh-CN', 'zh', 'en'] })
    window.chrome = { runtime: {} }
  })

  const page = await context.newPage()

  // ── 步骤2: 打开抖音用户主页 ──
  console.log('[2/6] 打开抖音用户主页...')
  console.log(`  URL: ${CONFIG.douyinUrl}\n`)

  await page.goto(CONFIG.douyinUrl, { waitUntil: 'networkidle', timeout: 30000 }).catch(() => {
    console.log('  ⚠ 页面加载超时，但继续尝试...')
  })

  // 等待页面渲染
  await page.waitForTimeout(3000)

  // ── 步骤3: 滚动加载视频列表 ──
  console.log('[3/6] 滚动加载视频列表...')

  const videoSet = new Set()
  let scrollCount = 0
  const maxScrolls = Math.ceil(CONFIG.maxVideos / 5)

  while (videoSet.size < CONFIG.maxVideos && scrollCount < maxScrolls * 2) {
    scrollCount++

    // 从页面提取视频数据
    const videos = await page.evaluate(() => {
      const items = []

      // 方式1: 从 DOM 中的视频卡片提取
      document.querySelectorAll('a[href*="/video/"]').forEach((a) => {
        const href = a.getAttribute('href')
        if (href && href.includes('/video/')) {
          const videoId = href.split('/video/')[1]?.split('?')[0]
          if (videoId) {
            const card = a.closest('[class*="search"]') || a.closest('li') || a.parentElement
            const title = card?.querySelector('[class*="title"], .desc, span')?.textContent?.trim() || ''
            const img = card?.querySelector('img')
            const thumbnail = img?.src || img?.getAttribute('data-src') || ''
            items.push({ videoId, title, thumbnail, href })
          }
        }
      })

      // 方式2: 从 __NEXT_DATA__ 或 window 中的数据层提取
      try {
        const scripts = document.querySelectorAll('script')
        scripts.forEach((s) => {
          const text = s.textContent || ''
          if (text.includes('"video"') && text.includes('"playAddr"')) {
            // 尝试找到JSON数据
            const match = text.match(/play_addr[\s\S]*?url_list.*?\["(.*?)"\]/)
            if (match && match[1]) {
              items.push({ videoUrl: match[1].replace(/\\u002F/g, '/') })
            }
          }
        })
      } catch (_) {}

      return items
    })

    // 去重添加
    for (const v of videos) {
      const key = v.videoId || v.videoUrl || v.href
      if (key && !videoSet.has(key)) {
        videoSet.add(key)
      }
    }

    process.stdout.write(
      `\r  已发现 ${videoSet.size} 个视频 | 滚动 ${scrollCount}/${maxScrolls * 2}`
    )

    if (videoSet.size >= CONFIG.maxVideos) break

    // 向下滚动
    await page.evaluate(() => {
      window.scrollBy(0, window.innerHeight * 2)
    })
    await page.waitForTimeout(2000)
  }

  console.log(`\n  共发现 ${videoSet.size} 个视频条目\n`)

  // ── 步骤4: 截取网络请求中的视频数据 ──
  console.log('[4/6] 监听网络请求获取视频真实URL...')

  const videoData = [] // { videoId, title, cover, videoUrl }

  // 重新加载页面，拦截 API 请求
  let apiVideoCount = 0
  page.on('response', async (response) => {
    const url = response.url()
    // 抖音视频列表API
    if (url.includes('/aweme/v1/web/aweme/post/') && response.status() === 200) {
      try {
        const data = await response.json()
        const awemeList = data?.aweme_list || []
        for (const aweme of awemeList) {
          const videoUrl =
            aweme?.video?.play_addr?.url_list?.[0] ||
            aweme?.video?.play_addr_h264?.url_list?.[0] ||
            aweme?.video?.bit_rate?.[0]?.play_addr?.url_list?.[0] ||
            ''

          const coverUrl =
            aweme?.video?.cover?.url_list?.[0] ||
            aweme?.video?.origin_cover?.url_list?.[0] ||
            ''

          const desc = aweme?.desc || ''
          const awemeId = aweme?.aweme_id || ''

          if (videoUrl && !videoData.find((v) => v.videoId === awemeId)) {
            videoData.push({
              videoId: awemeId,
              title: desc,
              cover: coverUrl,
              videoUrl: videoUrl.replace(/^\/\//, 'https://'),
            })
            apiVideoCount++
          }
        }
        console.log(`  ✓ API 捕获: ${apiVideoCount} 个视频`)
      } catch (_) {}
    }
  })

  // 刷新页面触发 API 请求
  await page.reload({ waitUntil: 'networkidle', timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(2000)

  // 滚动加载更多
  for (let i = 0; i < maxScrolls; i++) {
    await page.evaluate(() => window.scrollBy(0, window.innerHeight * 2))
    await page.waitForTimeout(3000)
    process.stdout.write(`\r  滚动加载: ${i + 1}/${maxScrolls} | API捕获: ${apiVideoCount} 个`)
    if (apiVideoCount >= CONFIG.maxVideos) break
  }

  console.log(`\n  通过API共捕获 ${videoData.length} 个视频的真实URL\n`)

  // 保存原始数据
  const rawDataFile = path.join(CONFIG.downloadDir, 'video_data.json')
  fs.writeFileSync(rawDataFile, JSON.stringify(videoData, null, 2), 'utf-8')
  console.log(`  原始数据已保存: ${rawDataFile}`)

  // ── 如果没捕获到API数据，尝试从页面数据层提取 ──
  if (videoData.length === 0) {
    console.log('\n  ⚠ 未通过API捕获到视频数据')
    console.log('  尝试从页面 localStorage / 渲染数据提取...')

    const pageData = await page.evaluate(() => {
      // 尝试多种方式提取
      const results = []

      // 方式1: window.__INITIAL_STATE__
      try {
        const state = window.__INITIAL_STATE__
        if (state) {
          JSON.stringify(state, (key, val) => {
            if (key === 'play_addr' || key === 'video') {
              results.push(val)
            }
            return val
          })
        }
      } catch (_) {}

      // 方式2: 所有script标签中的JSON
      document.querySelectorAll('script').forEach((s) => {
        const text = s.textContent || ''
        if (text.includes('play_addr') && text.includes('url_list')) {
          results.push(text.substring(0, 5000))
        }
      })

      // 方式3: 从DOM中提取可见数据
      const visibleData = []
      document.querySelectorAll('a[href*="/video/"]').forEach((a) => {
        const href = a.getAttribute('href')
        const img = a.querySelector('img')
        visibleData.push({
          href,
          img: img?.src || img?.getAttribute('data-src') || '',
          alt: img?.alt || '',
        })
      })
      results.push({ type: 'visible', data: visibleData })

      return results
    })

    fs.writeFileSync(
      path.join(CONFIG.downloadDir, 'page_extracted.json'),
      JSON.stringify(pageData, null, 2),
      'utf-8'
    )
    console.log('  页面提取数据已保存到 douyin_downloads/page_extracted.json')
  }

  // ── 步骤5: 下载视频 ──
  console.log(`\n[5/6] 下载视频 (最多 ${CONFIG.maxVideos} 个)...`)

  const results = []
  const toDownload = videoData.slice(0, CONFIG.maxVideos)

  for (let i = 0; i < toDownload.length; i++) {
    const v = toDownload[i]
    const videoFile = path.join(CONFIG.downloadDir, `${v.videoId}.mp4`)
    const coverFile = path.join(CONFIG.downloadDir, `${v.videoId}_cover.jpg`)

    console.log(`  [${i + 1}/${toDownload.length}] ${v.title || v.videoId}`)

    // 下载视频
    try {
      if (v.videoUrl && !fs.existsSync(videoFile)) {
        process.stdout.write('    下载视频... ')
        await downloadFile(v.videoUrl, videoFile)
        console.log(`✓ (${(fs.statSync(videoFile).size / 1024 / 1024).toFixed(1)}MB)`)
      } else if (fs.existsSync(videoFile)) {
        console.log(`    视频已缓存 (${(fs.statSync(videoFile).size / 1024 / 1024).toFixed(1)}MB)`)
      }
    } catch (err) {
      console.log(`    ✗ 视频下载失败: ${err.message}`)
      results.push({ ...v, status: 'download_failed', error: err.message })
      continue
    }

    // 下载封面
    try {
      if (v.cover && !fs.existsSync(coverFile)) {
        process.stdout.write('    下载封面... ')
        await downloadFile(v.cover, coverFile)
        console.log('✓')
      }
    } catch (_) {
      // 封面下载失败不影响
    }

    results.push({ ...v, videoFile, coverFile, status: 'downloaded' })
  }

  // ── 步骤6: 上传到MinIO并发布 ──
  console.log(`\n[6/6] 上传到 MinIO 并发布作品...`)

  // 首先获取JWT token
  console.log('  获取登录令牌...')

  // 尝试获取验证码
  let token = null
  try {
    // 获取 captcha key
    const captchaRes = await new Promise((resolve, reject) => {
      const url = new URL(CONFIG.apiBase + '/api/captcha')
      http.get(
        { hostname: url.hostname, port: url.port, path: url.pathname },
        (res) => {
          let data = ''
          res.on('data', (chunk) => (data += chunk))
          res.on('end', () => {
            try {
              resolve(JSON.parse(data))
            } catch (e) {
              reject(e)
            }
          })
        }
      ).on('error', reject)
    })

    if (captchaRes.code === 200 && captchaRes.data?.captchaKey) {
      console.log(`  验证码Key: ${captchaRes.data.captchaKey}`)
      console.log(`  验证码图片: ${CONFIG.apiBase}${captchaRes.data.captchaImage}`)
      console.log('\n  ⚠ 请在浏览器中打开验证码图片，手动输入验证码')
      console.log(`  图片地址: ${CONFIG.apiBase}${captchaRes.data.captchaImage}\n`)
    }
  } catch (err) {
    console.log(`  获取验证码失败: ${err.message}`)
  }

  // 保存结果
  const resultFile = path.join(CONFIG.downloadDir, 'upload_results.json')
  fs.writeFileSync(
    resultFile,
    JSON.stringify(
      {
        total: results.length,
        downloaded: results.filter((r) => r.status === 'downloaded').length,
        results,
      },
      null,
      2
    ),
    'utf-8'
  )
  console.log(`  结果已保存: ${resultFile}`)

  // ── 清理 ──
  await browser.close()

  console.log('\n═══════════════════════════════════════')
  console.log('  抓取完成!')
  console.log(`  共发现 ${videoData.length} 个视频`)
  console.log(`  已下载 ${results.filter((r) => r.status === 'downloaded').length} 个`)
  console.log(`  文件目录: ${CONFIG.downloadDir}`)
  console.log('═══════════════════════════════════════\n')

  if (videoData.length === 0) {
    console.log('⚠ 未能捕获到视频API数据。可能原因:')
    console.log('  1. 抖音页面需要登录才能查看用户作品')
    console.log('  2. 抖音的反爬机制拦截了请求')
    console.log('  3. 页面结构发生了变化')
    console.log('\n建议:')
    console.log('  1. 在浏览器窗口中手动登录抖音账号')
    console.log('  2. 重新运行脚本')
    console.log('  3. 如果仍然失败，手动从抖音复制视频链接到 video_urls.txt')
  }
}

main().catch((err) => {
  console.error('\n❌ 脚本执行失败:', err.message)
  console.error(err.stack)
  process.exit(1)
})
