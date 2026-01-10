# 使用Eclipse Temurin OpenJDK镜像作为基础镜像
FROM eclipse-temurin:11-jdk

# 设置工作目录
WORKDIR /app

# 创建数据目录
RUN mkdir -p /data

# 复制源代码
COPY src/ /app/src/

# 编译Java代码
RUN javac -d /app/classes /app/src/*.java

# 设置环境变量
ENV STORAGE_NODES="node1:8001:1,node2:8002:2,node3:8003:3"

# 设置运行时的工作目录
WORKDIR /app/classes

# 默认命令
CMD ["java", "ClientMain"]