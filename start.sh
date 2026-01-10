#!/bin/bash

# 分布式存储系统启动脚本

echo "=== 分布式存储系统启动脚本 ==="

# 检查Docker和docker-compose是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker未安装或未在PATH中"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "错误: docker-compose未安装或未在PATH中"
    exit 1
fi

# 创建必要的目录
mkdir -p data logs

# 构建并启动所有服务
echo "正在构建镜像..."
docker-compose build

echo "正在启动服务..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动完成..."
sleep 10

# 检查服务状态
echo "=== 服务状态 ==="
docker-compose ps

echo ""
echo "=== 系统已启动完成 ==="
echo ""
echo "可用的操作:"
echo "1. 生成测试数据:"
echo "   docker-compose exec client java ClientMain generate 2353250 1000 /data/test-data.dat"
echo ""
echo "2. 分发数据到存储节点:"
echo "   docker-compose exec client java ClientMain distribute /data/test-data.dat"
echo ""
echo "3. 查询数据:"
echo "   docker-compose exec client java ClientMain query"
echo ""
echo "4. 查看节点日志:"
echo "   docker-compose logs node1"
echo "   docker-compose logs node2"
echo "   docker-compose logs node3"
echo ""
echo "5. 停止系统:"
echo "   docker-compose down"
echo ""
echo "6. 完全清理 (删除数据):"
echo "   docker-compose down -v"