#!/bin/bash

# 分布式存储系统实验脚本

echo "=== 分布式存储系统实验演示 ==="

# 检查系统是否运行
if ! docker-compose ps | grep -q "Up"; then
    echo "系统未启动，请先运行: ./start.sh"
    exit 1
fi

echo "1. 生成测试数据 (1000条记录)..."
docker-compose exec -T client java ClientMain generate 2353250 1000 /data/test-data.dat

echo ""
echo "2. 分发数据到分布式存储节点..."
docker-compose exec -T client java ClientMain distribute /data/test-data.dat

echo ""
echo "3. 等待数据分发完成..."
sleep 5

echo ""
echo "4. 检查各节点数据文件状态:"
echo "节点1:"
docker-compose exec -T node1 ls -la /data/

echo "节点2:"
docker-compose exec -T node2 ls -la /data/

echo "节点3:"
docker-compose exec -T node3 ls -la /data/

echo ""
echo "5. 测试查询功能 (查询学号 1000001):"
# 创建查询测试脚本
cat > temp_query.txt << EOF
1000001
1000010
1000100
quit
EOF

echo "正在查询学号 1000001, 1000010, 1000100..."
docker-compose exec -T client sh -c "java ClientMain query < /dev/stdin" < temp_query.txt

# 清理临时文件
rm -f temp_query.txt

echo ""
echo "=== 实验演示完成 ==="
echo ""
echo "你现在可以:"
echo "1. 手动查询数据: docker-compose exec client java ClientMain query"
echo "2. 查看节点日志: docker-compose logs node1"
echo "3. 停止系统: docker-compose down"