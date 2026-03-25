import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Profile from '../views/Profile.vue'
import RoleManagement from '../views/RoleManagement.vue'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/',
        component: Layout,
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/DashboardContent.vue')
            }
        ]
    },
    {
        path: '/profile',
        name: 'Profile',
        component: Profile
    },
    {
        path: '/role-management',
        name: 'RoleManagement',
        component: RoleManagement
    },
    {
        path: '/main',
        name: 'Main',
        component: Layout
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/login'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from) => {
    return true
})

export default router
