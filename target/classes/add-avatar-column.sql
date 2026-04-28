-- 为用户表添加头像字段
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);

-- 说明：avatar 字段用于存储用户头像的相对路径
-- 例如：/user/avatar/image/avatar_1_20260323120000.jpg
-- 前端使用时需要拼接 API 基础 URL
