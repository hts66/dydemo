import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/featured',
    },
    {
      path: '/recommend',
      redirect: '/featured',
    },
    {
      path: '/featured',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Featured',
          component: () => import('../views/Featured.vue'),
        },
      ],
    },
    {
      path: '/my',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'My',
          component: () => import('../views/My.vue'),
        },
      ],
    },
    {
      path: '/friends',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Friends',
          component: () => import('../views/Friends.vue'),
        },
      ],
    },
    {
      path: '/following',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Following',
          component: () => import('../views/Following.vue'),
        },
      ],
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/Register.vue'),
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/Profile.vue'),
    },
    {
      path: '/upload',
      name: 'Upload',
      component: () => import('../views/UploadVideo.vue'),
    },
  ],
})

export default router
