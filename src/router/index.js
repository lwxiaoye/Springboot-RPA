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
import ProcessDesigner from '../views/rpa/ProcessDesigner.vue'
import Logs from '../views/rpa/Logs.vue'
import Notifications from '../views/rpa/Notifications.vue'
import DataQuery from '../views/rpa/DataQuery.vue'

// 新增页面
import QueueTrigger from '../views/rpa/QueueTrigger.vue'
import AuditLog from '../views/rpa/AuditLog.vue'
import CredentialVault from '../views/rpa/CredentialVault.vue'
import SystemSettings from '../views/rpa/SystemSettings.vue'
import ReportAnalytics from '../views/rpa/ReportAnalytics.vue'

// 新增独立页面（队列管理、触发器管理、死信队列、机器人健康）
import Queues from '../views/rpa/Queues.vue'
import Triggers from '../views/rpa/Triggers.vue'
import DeadLetterQueue from '../views/rpa/DeadLetterQueue.vue'
import RobotHealth from '../views/rpa/RobotHealth.vue'

// 企业级功能页面
import AiCenter from '../views/rpa/AiCenter.vue'
import RecordingCenter from '../views/rpa/RecordingCenter.vue'
import ScriptExecutor from '../views/rpa/ScriptExecutor.vue'
import DataMasking from '../views/rpa/DataMasking.vue'
import DistributedLock from '../views/rpa/DistributedLock.vue'

// 新增：智能录制器和AI助手页面
import Recorder from '../views/rpa/Recorder.vue'
import AiAssistant from '../views/rpa/AiAssistant.vue'

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
            { path: 'process-designer', name: 'ProcessDesigner', component: ProcessDesigner },
            { path: 'process-designer/:id', name: 'ProcessDesignerEdit', component: ProcessDesigner, props: true },
            { path: 'queues', name: 'Queues', component: Queues },
            { path: 'triggers', name: 'Triggers', component: Triggers },
            { path: 'dead-letter', name: 'DeadLetterQueue', component: DeadLetterQueue },
            { path: 'robot-health', name: 'RobotHealth', component: RobotHealth },
            { path: 'logs', name: 'Logs', component: Logs },
            { path: 'audit', name: 'AuditLog', component: AuditLog },
            { path: 'credentials', name: 'CredentialVault', component: CredentialVault },
            { path: 'reports', name: 'ReportAnalytics', component: ReportAnalytics },
            { path: 'notifications', name: 'Notifications', component: Notifications },
            { path: 'settings', name: 'SystemSettings', component: SystemSettings },
            { path: 'data-query', name: 'DataQuery', component: DataQuery },
            // 企业级功能
            { path: 'ai', name: 'AiCenter', component: AiCenter },
            { path: 'recording', name: 'RecordingCenter', component: RecordingCenter },
            { path: 'script', name: 'ScriptExecutor', component: ScriptExecutor },
            { path: 'masking', name: 'DataMasking', component: DataMasking },
            { path: 'locks', name: 'DistributedLock', component: DistributedLock },
            // 新增：智能录制器和AI助手
            { path: 'recorder', name: 'Recorder', component: Recorder },
            { path: 'ai-assistant', name: 'AiAssistant', component: AiAssistant }
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

router.beforeEach((to, from) => {
    // 公开的路由，不需要登录
    const publicRoutes = ['/login']
    
    // 获取 token
    const token = localStorage.getItem('token')
    
    // 如果访问的是公开路由，直接放行
    if (publicRoutes.includes(to.path)) {
        return true
    }
    
    // 如果访问的不是公开路由，且没有 token，跳转到登录页
    if (!token) {
        return '/login'
    }
    
    // 有 token，放行
    return true
})

export default router