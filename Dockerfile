# RPA系统后端 - Docker镜像
# 基于JDK 17镜像，支持Linux和Windows容器

# ===== 多阶段构建 =====

# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 设置Maven镜像加速（可选）
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories || true

# 构建项目（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests

# ===== 阶段2: 运行阶段 =====
FROM eclipse-temurin:17-jre-jammy

# 维护者信息
LABEL maintainer="RPA System"
LABEL description="Enterprise RPA Platform Backend"

# 设置工作目录
WORKDIR /app

# 创建必要目录
RUN mkdir -p /app/logs /app/recordings /app/templates /app/data

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 设置环境变量
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=docker
ENV TZ=Asia/Shanghai

# 暴露端口
EXPOSE 5173

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5173/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]