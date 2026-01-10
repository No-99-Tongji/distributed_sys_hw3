#!/bin/bash

echo "===================================="
echo "测试动态节点发现功能"
echo "===================================="

echo
echo "1. 启动分布式系统..."
docker-compose up -d

echo
echo "2. 等待服务启动..."
sleep 10

echo
echo "3. 测试Client与Manager的连接..."
docker exec distributed-client java ClientMain test-manager

echo
echo "4. 测试数据分发（使用动态节点发现）..."
echo "生成测试数据..."
docker exec distributed-client java ClientMain generate 2353250 100 /data/test-data.dat

echo "分发数据..."
docker exec distributed-client java ClientMain distribute /data/test-data.dat

echo
echo "5. 检查Manager状态..."
echo "Manager日志:"
docker logs distributed-manager --tail 10

echo
echo "6. 检查存储节点状态..."
for i in 1 2 3; do
    echo "节点$i 日志:"
    docker logs distributed-node$i --tail 5
done

echo
echo "===================================="
echo "测试完成"
echo "===================================="
echo
echo "如果要清理环境，请运行: docker-compose down -v"