import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  { path: '/login', component: () => import('../views/auth/LoginView.vue'), meta: { guest: true } },
  { path: '/register', component: () => import('../views/auth/RegisterView.vue'), meta: { guest: true } },
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
      { path: 'verification', component: () => import('../views/user/VerificationView.vue'), meta: { auth: true } },
      { path: 'notifications', redirect: '/hall', meta: { auth: true } },
      { path: 'user/:id', component: () => import('../views/user/UserPublicProfile.vue') },

      { path: 'evaluation/:pickupId', component: () => import('../views/pickup/EvaluationView.vue'), meta: { auth: true } },

      { path: 'admin/verification', component: () => import('../views/admin/VerificationReview.vue'), meta: { auth: true, admin: true } },
      { path: 'transactions', component: () => import('../views/user/TransactionList.vue'), meta: { auth: true } },
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
  // 游客页面（登录/注册），已登录则重定向到首页
  if (to.meta.guest) {
    if (store.isLoggedIn) return next('/')
    return next()
  }
  // 需要登录但未登录
  if (to.meta.auth && !store.isLoggedIn) {
    return next('/login')
  }
  // 已登录但用户信息未加载
  if (store.isLoggedIn && !store.userInfo) {
    try {
      await store.fetchUserInfo()
    } catch {
      return next('/login')
    }
  }
  // 需要管理员权限但不是管理员
  if (to.meta.admin && !store.isAdmin) {
    return next('/')
  }
  next()
})

export default router
