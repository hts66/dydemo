import request from '../utils/request'

// 获取视频列表（random=true + seed 实现随机推送，同一 seed 分页一致）
export const getWorks = (page = 1, size = 10, random = false, seed = 0) => {
  let url = `/works?page=${page}&size=${size}`
  if (random) url += `&random=true&seed=${seed}`
  return request.get(url)
}

// 获取单个视频
export const getWorkById = (id) => {
  return request.get(`/works/${id}`)
}

// 获取用户视频（分页）
export const getUserWorks = (userId, page = 1, size = 12) => {
  return request.get(`/works/user/${userId}?page=${page}&size=${size}`)
}

// 发布视频
export const publishWork = (data) => {
  return request.post('/works', data)
}

// 删除视频
export const deleteWork = (id) => {
  return request.delete(`/works/${id}`)
}

// 获取朋友视频列表（当前登录用户，userId 由后端从 token 取）
export const getFriendsWorks = () => {
  return request.get('/works/my/friends')
}

// 获取关注用户视频列表（当前登录用户，userId 由后端从 token 取）
export const getFollowingWorks = () => {
  return request.get('/works/my/following')
}

// 获取推荐视频列表
export const getRecommendWorks = (page = 1, size = 10) => {
  return request.get(`/works/recommend?page=${page}&size=${size}`)
}

// 获取热门视频列表
export const getHotWorks = (page = 1, size = 10) => {
  return request.get(`/works/hot?page=${page}&size=${size}`)
}

// 搜索视频（复用 getWorks 端点，传入 keyword 参数）
export const searchWorks = (keyword, page = 1, size = 12) => {
  return request.get(`/works?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
}

// 记录观看历史
export const recordWatchHistory = (data) => {
  return request.post('/works/watch', data)
}
