-- RPA系统数据库初始化脚本
-- 执行时机：容器首次启动时自动执行

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS rpa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rpa;

-- 设置时区
SET time_zone = '+08:00';

-- 初始管理员账号（用户可自行修改）
-- 用户名: admin / 密码: admin123（加密后）

-- =========================================
-- 初始化完成
-- =========================================