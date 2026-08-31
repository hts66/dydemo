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

export const getFriends = (userId) => {
  return request.get(`/follows/friends/${userId}`)
}
