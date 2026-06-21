import request from '../utils/request'

export const getComments = (workId) => {
  return request.get(`/comments/${workId}`)
}

export const addComment = (workId, content) => {
  return request.post('/comments', { workId, content })
}
