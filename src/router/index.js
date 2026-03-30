import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Profile from '../views/Profile.vue'

const routes = [
    {
        path: '/',
        redirect: '/layout'
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    // 仪表板 - 独立页面，有自己的顶部导航，无侧边栏
    {
        path: '/layout',
        name: 'Layout',
        component: Layout
    },
    {
        path: '/profile',
        name: 'Profile',
        component: Profile
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/dashboard'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    next()
})

export default router