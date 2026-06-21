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
