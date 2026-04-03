import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import RpaLayout from '../views/rpa/RpaLayout.vue'
import SystemLayout from '../views/sys/SystemLayout.vue'
import ProfilePage from '../views/sys/Profile.vue'
import UserManagement from '../views/sys/UserManagement.vue'
import RoleManagement from '../views/sys/RoleManagement.vue'
import ResourceManagement from '../views/sys/ResourceManagement.vue'

// RPA运营管理模块页面
import Tasks from '../views/rpa/Tasks.vue'
import Robots from '../views/rpa/Robots.vue'
import RobotDetail from '../views/rpa/RobotDetail.vue'
import Processes from '../views/rpa/Processes.vue'
import ProcessDetail from '../views/rpa/ProcessDetail.vue'
import Logs from '../views/rpa/Logs.vue'
import Notifications from '../views/rpa/Notifications.vue'
import DataQuery from '../views/rpa/DataQuery.vue'

// 新增页面
import QueueTrigger from '../views/rpa/QueueTrigger.vue'
import AuditLog from '../views/rpa/AuditLog.vue'
import CredentialVault from '../views/rpa/CredentialVault.vue'
import SystemSettings from '../views/rpa/SystemSettings.vue'
import ReportAnalytics from '../views/rpa/ReportAnalytics.vue'

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
        redirect: '/dashboard',
        children: [
            { path: 'tasks', name: 'Tasks', component: Tasks },
            { path: 'robots', name: 'Robots', component: Robots },
            { path: 'robot/:id', name: 'RobotDetail', component: RobotDetail, props: true },
            { path: 'processes', name: 'Processes', component: Processes },
            { path: 'process/:id', name: 'ProcessDetail', component: ProcessDetail, props: true },
            { path: 'queue', name: 'QueueTrigger', component: QueueTrigger },
            { path: 'logs', name: 'Logs', component: Logs },
            { path: 'audit', name: 'AuditLog', component: AuditLog },
            { path: 'credentials', name: 'CredentialVault', component: CredentialVault },
            { path: 'reports', name: 'ReportAnalytics', component: ReportAnalytics },
            { path: 'notifications', name: 'Notifications', component: Notifications },
            { path: 'settings', name: 'SystemSettings', component: SystemSettings },
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
        redirect: '/login'
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

router.beforeEach((to, from, next) => {
    next()
})

export default router