import request from '../utils/request'

export const getChatMessages = (otherUserId) => {
  return request.get(`/messages/chat/${otherUserId}`)
}

export const sendMessage = (receiverId, content) => {
  return request.post('/messages', { receiverId, content })
}

export const saveBotMessage = (content) => {
  return request.post('/messages/bot', { content })
}