import request from '../utils/request'

export const toggleLike = (workId) => {
  return request.post(`/likes/${workId}`)
}

export const isLiked = (workId) => {
  return request.get(`/likes/${workId}`)
}

export const getLikedWorks = (userId) => {
  return request.get(`/likes/list/${userId}`)
}
