import request from '../utils/request'

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 60000,
  })
}

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 60000,
  })
}

export const uploadVideo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/video', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 120000,
  })
}

export const cleanupFiles = (urls) => {
  return request.post('/upload/cleanup', urls, {
    headers: {
      'Content-Type': 'application/json',
    },
  })
}
