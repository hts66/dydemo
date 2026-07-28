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
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Recommend',
          component: () => import('../views/Recommend.vue'),
        },
      ],
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
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('../views/ForgotPassword.vue'),
    },
    {
      path: '/profile/:userId',
      name: 'Profile',
      component: () => import('../views/Profile.vue'),
    },
    {
      path: '/upload',
      name: 'Upload',
      component: () => import('../views/UploadVideo.vue'),
    },
    {
      path: '/search',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Search',
          component: () => import('../views/Search.vue'),
        },
      ],
    },
  ],
})

export default router
