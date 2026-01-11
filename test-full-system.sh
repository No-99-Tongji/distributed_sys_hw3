#!/bin/bash

echo "=== 分布式存储系统完整测试 ==="
echo

# 检查容器状态
echo "1. 检查容器状态..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo

# 测试Manager连接
echo "2. 测试Manager连接和节点发现..."
docker exec distributed-client java ClientMain test-manager
echo

# 分发数据
echo "3. 分发数据到存储节点..."
docker exec distributed-client java ClientMain distribute
echo

# 等待数据存储完成
echo "4. 等待数据存储完成..."
sleep 3

# 检查每个节点的存储状态
echo "5. 检查存储节点数据..."
for node in node1 node2 node3; do
    echo "--- 存储节点 $node ---"
    docker exec distributed-$node find . -name "*.dat" -o -name "*.idx" | head -5
    docker exec distributed-$node sh -c "if [ -f '2353250-hw3-*.dat' ]; then ls -la *.dat *.idx 2>/dev/null || echo '数据文件未找到'; fi"
    echo
done

# 检查Manager的集群状态
echo "6. 检查Manager集群状态..."
echo "Manager日志 (最近15行):"
docker logs distributed-manager | tail -15
echo

echo "=== 测试完成 ==="