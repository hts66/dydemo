import request from '../utils/request'

export const getUserById = (id) => {
  return request.get(`/users/${id}`)
}

export const updateBackground = (background) => {
  return request.put('/users/background', { background })
}

export const updateAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.put('/auth/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updateBackgroundFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.put('/auth/user/background', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}