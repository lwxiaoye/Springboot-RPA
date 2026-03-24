-- 为用户表添加头像字段
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);

-- 说明：avatar 字段用于存储用户头像的 URL 路径
-- 例如：/api/user/avatar/image/avatar_1_20260323120000.jpg
