import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import RpaLayout from '../views/rpa/RpaLayout.vue'
import SystemLayout from '../views/sys/SystemLayout.vue'  // 修改路径
import ProfilePage from '../views/sys/Profile.vue'        // 修改路径
import UserManagement from '../views/sys/UserManagement.vue'  // 修改路径
import RoleManagement from '../views/sys/RoleManagement.vue'  // 修改路径
import ResourceManagement from '../views/sys/ResourceManagement.vue'  // 修改路径

// RPA运营管理模块页面
import Tasks from '../views/rpa/Tasks.vue'
import Robots from '../views/rpa/Robots.vue'
import Processes from '../views/rpa/Processes.vue'
import Logs from '../views/rpa/Logs.vue'
import Notifications from '../views/rpa/Notifications.vue'
import DataCollect from '../views/rpa/DataCollect.vue'
import DataParse from '../views/rpa/DataParse.vue'
import DataProcess from '../views/rpa/DataProcess.vue'
import DataQuery from '../views/rpa/DataQuery.vue'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    // 仪表板 - 独立页面，有自己的顶部导航，无侧边栏
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: Dashboard
    },
    // RPA运营管理模块 - 使用 RpaLayout 布局
    {
        path: '/rpa',
        component: RpaLayout,
        redirect: '/rpa/tasks',
        children: [
            { path: 'tasks', name: 'Tasks', component: Tasks },
            { path: 'robots', name: 'Robots', component: Robots },
            { path: 'processes', name: 'Processes', component: Processes },
            { path: 'logs', name: 'Logs', component: Logs },
            { path: 'notifications', name: 'Notifications', component: Notifications },
            { path: 'data-collect', name: 'DataCollect', component: DataCollect },
            { path: 'data-parse', name: 'DataParse', component: DataParse },
            { path: 'data-process', name: 'DataProcess', component: DataProcess },
            { path: 'data-query', name: 'DataQuery', component: DataQuery }
        ]
    },
    // 系统管理模块 - 使用 SystemLayout 布局
    {
        path: '/system',
        component: SystemLayout,
        redirect: '/system/profile',
        children: [
            { path: 'profile', name: 'Profile', component: ProfilePage },
            { path: 'users', name: 'UserManagement', component: UserManagement },
            { path: 'roles', name: 'RoleManagement', component: RoleManagement },
            { path: 'resources', name: 'ResourceManagement', component: ResourceManagement }
        ]
    },
    {
        path: '/',
        redirect: '/dashboard'
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