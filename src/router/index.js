import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import RpaLayout from '../views/rpa/RpaLayout.vue'
import SystemLayout from '../views/sys/SystemLayout.vue'
import ProfilePage from '../views/sys/Profile.vue'
import UserManagement from '../views/sys/UserManagement.vue'
import RoleManagement from '../views/sys/RoleManagement.vue'
import ResourceManagement from '../views/sys/ResourceManagement.vue'

// 工作台首页
import HomeDashboard from '../views/HomeDashboard.vue'

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

// 队列管理、触发器管理、死信队列、机器人健康
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

// 智能录制器和AI助手页面
import Recorder from '../views/rpa/Recorder.vue'
import AiAssistant from '../views/rpa/AiAssistant.vue'

// 实时监控页面
import RealTimeMonitor from '../views/rpa/RealTimeMonitor.vue'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    // 独立首页 - 无侧边栏，登录后第一个进入的页面
    {
        path: '/',
        component: HomeDashboard
    },
    // RPA运营管理模块 - 使用 RpaLayout 布局
    {
        path: '/rpa',
        component: RpaLayout,
        redirect: '/rpa/tasks',
        children: [
            { path: 'monitor', name: 'RealTimeMonitor', component: RealTimeMonitor },
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
            { path: 'ai', name: 'AiCenter', component: AiCenter },
            { path: 'recording', name: 'RecordingCenter', component: RecordingCenter },
            { path: 'script', name: 'ScriptExecutor', component: ScriptExecutor },
            { path: 'masking', name: 'DataMasking', component: DataMasking },
            { path: 'locks', name: 'DistributedLock', component: DistributedLock },
            { path: 'recorder', name: 'Recorder', component: Recorder },
            { path: 'ai-assistant', name: 'AiAssistant', component: AiAssistant }
        ]
    },
    // 系统管理模块
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
    // 404 重定向
    {
        path: '/:pathMatch(.*)*',
        redirect: '/login'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由导航守卫
router.beforeEach((to, from) => {
    const publicRoutes = ['/login']
    const token = localStorage.getItem('token')

    if (publicRoutes.includes(to.path)) {
        return true
    }

    if (!token) {
        return '/login'
    }

    return true
})

export default router
