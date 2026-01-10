# 存储节点仓库

这是分布式存储系统的存储节点组件，负责数据存储和查询处理。

## 功能

- **数据存储**: 接收并存储数据块
- **索引管理**: 维护数据块索引信息
- **查询处理**: 处理学生记录查询请求
- **状态监控**: 实时显示节点状态

## 使用方法

### Docker方式运行

1. 构建镜像:
```bash
docker build -t distributed-node .
```

2. 启动存储节点:
```bash
# 节点1
docker run -d --name node1 -p 8001:8001 -p 9001:9001 \
  -e NODE_ID=1 -e NODE_PORT=8001 -e STUDENT_ID=2353250 \
  -v node1_data:/data --network distributed-network \
  distributed-node

# 节点2
docker run -d --name node2 -p 8002:8001 -p 9002:9001 \
  -e NODE_ID=2 -e NODE_PORT=8001 -e STUDENT_ID=2353250 \
  -v node2_data:/data --network distributed-network \
  distributed-node

# 节点3
docker run -d --name node3 -p 8003:8001 -p 9003:9001 \
  -e NODE_ID=3 -e NODE_PORT=8001 -e STUDENT_ID=2353250 \
  -v node3_data:/data --network distributed-network \
  distributed-node
```

### 环境变量

- `NODE_ID`: 节点唯一标识符
- `NODE_PORT`: 节点监听端口
- `STUDENT_ID`: 学号标识

## 文件结构

- `NodeMain.java` - 主入口类
- `StorageNode.java` - 存储节点核心实现
- `StudentRecord.java` - 学生记录数据结构

## 数据文件

每个节点维护两个文件：

### 数据文件 (`学号-hw3-节点ID.dat`)
- 顺序存储接收到的数据块
- 二进制格式存储

### 索引文件 (`学号-hw3-节点ID.idx`)
- 每行一个索引条目
- 格式: `<块ID>,<指针位置>,<数据大小>`

## 端口说明

- **数据端口**: 接收数据块 (默认: 8001)
- **查询端口**: 处理查询请求 (数据端口 + 1000)

## 查询协议

- **请求**: `QUERY:<学号>`
- **响应**: 学生记录字符串或 `NOT_FOUND`