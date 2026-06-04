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
      { path: 'pickup/:id', component: () => import('../views/pickup/PickupDetail.vue') },
      { path: 'pickup/publish', component: () => import('../views/pickup/PickupPublish.vue'), meta: { auth: true } },
      { path: 'my-pickups', component: () => import('../views/pickup/MyPickups.vue'), meta: { auth: true } },
      { path: 'profile', component: () => import('../views/user/ProfileView.vue'), meta: { auth: true } },
      { path: 'verification', component: () => import('../views/user/VerificationView.vue'), meta: { auth: true } },
      { path: 'notifications', component: () => import('../views/notification/NotificationList.vue'), meta: { auth: true } },
      { path: 'user/:id', component: () => import('../views/user/UserPublicProfile.vue') },
      { path: 'admin/verification', component: () => import('../views/admin/VerificationReview.vue'), meta: { auth: true, admin: true } },
      { path: 'transactions', component: () => import('../views/user/TransactionList.vue'), meta: { auth: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const store = useUserStore()
  if (to.meta.guest) {
    if (store.isLoggedIn()) return next('/')
    return next()
  }
  if (to.meta.auth && !store.isLoggedIn()) {
    return next('/login')
  }
  if (store.isLoggedIn() && !store.userInfo) {
    await store.fetchUserInfo()
  }
  if (to.meta.admin && !store.isAdmin()) {
    return next('/')
  }
  next()
})

export default router
