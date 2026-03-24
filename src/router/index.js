import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import RoleManagement from '../views/RoleManagement.vue'
import Profile from '../views/Profile.vue'

const routes = [
    {
        path: '/',
        redirect: '/role-management'
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/role-management',
        name: 'RoleManagement',
        component: RoleManagement
    },
    // 添加404页面重定向
    {
        path: '/:pathMatch(.*)*',
        redirect: '/role-management'
    },
    {
        path: '/profile',
        name: 'Profile',
        component: Profile
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 简化路由守卫，避免使用 next 回调
router.beforeEach((to, from) => {
    // 可以在这里添加登录判断逻辑
    return true
})

export default router