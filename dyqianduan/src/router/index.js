import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../components/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/recommend',
    },
    {
      path: '/recommend',
      name: 'Recommend',
      component: MainLayout,
      children: [
        {
          path: '',
          component: () => import('../views/Recommend.vue'),
        },
      ],
    },
    {
      path: '/featured',
      name: 'Featured',
      component: MainLayout,
      children: [
        {
          path: '',
          component: () => import('../views/Featured.vue'),
        },
      ],
    },
    {
      path: '/my',
      name: 'My',
      component: MainLayout,
      children: [
        {
          path: '',
          component: () => import('../views/My.vue'),
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
