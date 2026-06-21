import request from '../utils/request'

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export const uploadVideo = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/video', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
