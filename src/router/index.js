import { createRouter, createWebHistory } from 'vue-router'

// 公开页面
import Login from '../views/Login.vue'

// 懒加载所有页面组件
const Dashboard = () => import('../views/Dashboard.vue')
const RpaLayout = () => import('../views/rpa/RpaLayout.vue')
const SystemLayout = () => import('../views/sys/SystemLayout.vue')
const ProfilePage = () => import('../views/sys/Profile.vue')
const UserManagement = () => import('../views/sys/UserManagement.vue')
const RoleManagement = () => import('../views/sys/RoleManagement.vue')
const ResourceManagement = () => import('../views/sys/ResourceManagement.vue')

// RPA运营管理模块页面
const Tasks = () => import('../views/rpa/Tasks.vue')
const Robots = () => import('../views/rpa/Robots.vue')
const RobotDetail = () => import('../views/rpa/RobotDetail.vue')
const Processes = () => import('../views/rpa/Processes.vue')
const ProcessDetail = () => import('../views/rpa/ProcessDetail.vue')
const ProcessDesigner = () => import('../views/rpa/ProcessDesigner.vue')
const Logs = () => import('../views/rpa/Logs.vue')
const Notifications = () => import('../views/rpa/Notifications.vue')
const CollaborationHub = () => import('../views/rpa/CollaborationHub.vue')
const DataQuery = () => import('../views/rpa/DataQuery.vue')

// 新增页面
const QueueTrigger = () => import('../views/rpa/QueueTrigger.vue')
const AuditLog = () => import('../views/rpa/AuditLog.vue')
const CredentialVault = () => import('../views/rpa/CredentialVault.vue')
const SystemSettings = () => import('../views/rpa/SystemSettings.vue')
const ReportAnalytics = () => import('../views/rpa/ReportAnalytics.vue')

// 新增独立页面
const Queues = () => import('../views/rpa/Queues.vue')
const Triggers = () => import('../views/rpa/Triggers.vue')
const DeadLetterQueue = () => import('../views/rpa/DeadLetterQueue.vue')
const RobotHealth = () => import('../views/rpa/RobotHealth.vue')

// 企业级功能页面
const AiCenter = () => import('../views/rpa/AiCenter.vue')
const RecordingCenter = () => import('../views/rpa/RecordingCenter.vue')
const ScriptExecutor = () => import('../views/rpa/ScriptExecutor.vue')
const DataMasking = () => import('../views/rpa/DataMasking.vue')
const DistributedLock = () => import('../views/rpa/DistributedLock.vue')

// 智能录制器和AI助手页面
const Recorder = () => import('../views/rpa/Recorder.vue')
const AiAssistant = () => import('../views/rpa/AiAssistant.vue')

// 实时监控页面
const RealTimeMonitor = () => import('../views/rpa/RealTimeMonitor.vue')

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    // 仪表板 - 使用实时监控页面作为首页
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: RealTimeMonitor
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
            { path: 'collaboration', name: 'CollaborationHub', component: CollaborationHub },
            { path: 'settings', name: 'SystemSettings', component: SystemSettings },
            { path: 'data-query', name: 'DataQuery', component: DataQuery },
            // 企业级功能
            { path: 'ai', name: 'AiCenter', component: AiCenter },
            { path: 'recording', name: 'RecordingCenter', component: RecordingCenter },
            { path: 'script', name: 'ScriptExecutor', component: ScriptExecutor },
            { path: 'masking', name: 'DataMasking', component: DataMasking },
            { path: 'locks', name: 'DistributedLock', component: DistributedLock },
            // 智能录制器
            { path: 'recorder', name: 'Recorder', component: Recorder },
            // 工作台 - 使用实时监控页面
            { path: 'workbench', name: 'Workbench', component: RealTimeMonitor }
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