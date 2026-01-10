# 使用Eclipse Temurin OpenJDK镜像作为基础镜像
FROM eclipse-temurin:11-jdk

# 设置工作目录
WORKDIR /app

# 复制源代码
COPY src/ /app/src/

# 编译Java代码
RUN javac -d /app/classes /app/src/*.java

# 设置环境变量
ENV LEADER_HOST="manager"
ENV LEADER_PORT="9000"

# 设置运行时的工作目录
WORKDIR /app/classes

# 暴露端口
EXPOSE 9000

# 默认命令
CMD ["java", "MembershipMain", "0", "9000", "leader"]