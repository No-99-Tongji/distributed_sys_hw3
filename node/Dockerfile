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
ENV NODE_ID="1"
ENV NODE_PORT="8001"
ENV STUDENT_ID="2353250"

# 设置运行时的工作目录
WORKDIR /app/classes

# 暴露端口 (数据端口和查询端口)
EXPOSE 8001 9001

# 默认命令
CMD ["java", "NodeMain"]