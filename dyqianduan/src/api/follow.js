import request from '../utils/request'

export const toggleFollow = (followeeId) => {
  return request.post(`/follows/${followeeId}`)
}

export const checkIsFollowing = (followeeId) => {
  return request.get(`/follows/${followeeId}`)
}

export const getFollowList = (userId) => {
  return request.get(`/follows/list/${userId}`)
}

export const getFollowingWorks = (userId) => {
  return request.get(`/works/following/${userId}`)
}
