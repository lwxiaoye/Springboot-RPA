-- 修复用户表中的头像路径格式
-- 将旧的 /api/user/avatar/image/xxx 格式转换为 /user/avatar/image/xxx 格式

-- 更新所有以 /api/ 开头的头像路径
UPDATE users 
SET avatar = REPLACE(avatar, '/api/user/avatar/image/', '/user/avatar/image/')
WHERE avatar LIKE '/api/user/avatar/image/%';

-- 验证更新结果
SELECT id, username, avatar FROM users WHERE avatar IS NOT NULL;
