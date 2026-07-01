import request from '../utils/request'

// 获取视频列表
export const getWorks = (page = 1, size = 10) => {
  return request.get(`/works?page=${page}&size=${size}`)
}

// 获取单个视频
export const getWorkById = (id) => {
  return request.get(`/works/${id}`)
}

// 获取用户视频
export const getUserWorks = (userId) => {
  return request.get(`/works/user/${userId}`)
}

// 发布视频
export const publishWork = (data) => {
  return request.post('/works', data)
}

// 删除视频
export const deleteWork = (id) => {
  return request.delete(`/works/${id}`)
}

// 获取朋友视频列表
export const getFriendsWorks = (userId) => {
  return request.get(`/works/friends/${userId}`)
}

// 获取关注用户视频列表
export const getFollowingWorks = (userId) => {
  return request.get(`/works/following/${userId}`)
}

// 获取推荐视频列表
export const getRecommendWorks = (page = 1, size = 10) => {
  return request.get(`/works/recommend?page=${page}&size=${size}`)
}

// 获取热门视频列表
export const getHotWorks = (page = 1, size = 10) => {
  return request.get(`/works/hot?page=${page}&size=${size}`)
}

// 记录观看历史
export const recordWatchHistory = (data) => {
  return request.post('/works/watch', data)
}
