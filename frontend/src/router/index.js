import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', redirect: to => ({ path: '/hall', query: { ...to.query, auth: 'login' } }) },
  { path: '/register', redirect: to => ({ path: '/hall', query: { ...to.query, auth: 'register' } }) },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    children: [
      { path: '', redirect: '/hall' },
      { path: 'hall', component: () => import('../views/pickup/PickupHall.vue') },

      { path: 'publish', component: () => import('../views/pickup/PickupPublish.vue'), meta: { auth: true } },

      { path: 'pickup/:id', component: () => import('../views/pickup/PickupDetail.vue') },

      { path: 'my-pickups', component: () => import('../views/pickup/MyPickups.vue'), meta: { auth: true } },

      { path: 'profile', component: () => import('../views/user/ProfileView.vue'), meta: { auth: true } },
      { path: 'verification', redirect: '/profile', meta: { auth: true } },
      { path: 'notifications', redirect: '/hall', meta: { auth: true } },
      { path: 'user/:id', component: () => import('../views/user/UserPublicProfile.vue') },

      { path: 'evaluation/:pickupId', component: () => import('../views/pickup/EvaluationView.vue'), meta: { auth: true } },

      { path: 'admin/verification', component: () => import('../views/admin/VerificationReview.vue'), meta: { auth: true, admin: true } },
      { path: 'transactions', redirect: '/points', meta: { auth: true } },
      { path: 'points', component: () => import('../views/user/PaymentDetail.vue'), meta: { auth: true } },
      { path: 'my-evaluations', component: () => import('../views/user/MyEvaluations.vue'), meta: { auth: true } },
      { path: 'payment/:id', redirect: '/points' }
    ]
  },
  // 404 页面：对所有用户可见，不添加 guest 限制
  {
    path: '/:pathMatch(.*)*',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const store = useUserStore()
  // 需要登录但未登录
  if (to.meta.auth && !store.isLoggedIn) {
    return next({ path: '/hall', query: { auth: 'login', redirect: to.fullPath } })
  }
  // 已登录但用户信息未加载
  if (store.isLoggedIn && !store.userInfo) {
    try {
      await store.fetchUserInfo()
    } catch {
      return next({ path: '/hall', query: { auth: 'login', redirect: to.fullPath } })
    }
  }
  // 需要管理员权限但不是管理员
  if (to.meta.admin && !store.isAdmin) {
    return next('/')
  }
  next()
})

export default router
