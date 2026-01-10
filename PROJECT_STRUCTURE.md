# 项目结构说明

## 总体架构

```
distributed_sys_hw3/
├── client/                 # 客户端仓库
│   ├── src/               # 客户端源代码
│   ├── Dockerfile         # 客户端Docker镜像
│   └── README.md          # 客户端文档
├── manager/               # 管理器仓库
│   ├── src/               # 管理器源代码
│   ├── Dockerfile         # 管理器Docker镜像
│   └── README.md          # 管理器文档
├── node/                  # 存储节点仓库
│   ├── src/               # 节点源代码
│   ├── Dockerfile         # 节点Docker镜像
│   └── README.md          # 节点文档
├── docker-compose.yml     # Docker编排文件
├── start.sh              # 系统启动脚本
├── demo.sh               # 演示脚本
└── README.md             # 主文档
```

## 仓库分离说明

### Client仓库 (`client/`)

**功能**: 数据分发和查询客户端

**核心文件**:
- `ClientMain.java` - 主入口
- `Client.java` - 数据分发逻辑
- `QueryClient.java` - 查询功能
- `TestDataGenerator.java` - 测试数据生成
- `StudentRecord.java` - 数据结构
- `DatReader.java` - 文件读取
- `NetSender.java` - 网络发送

**Docker镜像**: `distributed-client`

### Manager仓库 (`manager/`)

**功能**: 集群成员管理和协调

**核心文件**:
- `ManagerMain.java` - 主入口
- `MembershipService.java` - 成员管理服务
- `Manager.java` - 管理器核心
- `NetSender.java` - 网络发送
- `NetReceiver.java` - 网络接收

**Docker镜像**: `distributed-manager`

### Node仓库 (`node/`)

**功能**: 数据存储和查询处理

**核心文件**:
- `NodeMain.java` - 主入口
- `StorageNode.java` - 存储节点核心
- `StudentRecord.java` - 数据结构

**Docker镜像**: `distributed-node`

## Docker部署架构

### 网络配置

- **网络名称**: `distributed-network`
- **网络类型**: bridge
- **容器间通信**: 通过服务名称

### 服务配置

| 服务名 | 容器名 | 镜像 | 端口映射 | 功能 |
|--------|---------|------|----------|------|
| manager | distributed-manager | distributed-manager | 9001:9001 | 集群管理 |
| node1 | distributed-node1 | distributed-node | 8001:8001, 9001:9001 | 存储节点1 |
| node2 | distributed-node2 | distributed-node | 8002:8001, 9002:9001 | 存储节点2 |
| node3 | distributed-node3 | distributed-node | 8003:8001, 9003:9001 | 存储节点3 |
| client | distributed-client | distributed-client | - | 客户端工具 |

### 数据持久化

- `manager_logs`: 管理器日志
- `node1_data`: 节点1数据文件
- `node2_data`: 节点2数据文件  
- `node3_data`: 节点3数据文件
- `client_data`: 客户端测试数据

## 通信协议

### 数据分发协议 (Client → Node)

```
UDP数据包格式:
[块ID(4字节)][是否主存储(1字节)][数据长度(4字节)][数据内容]
```

### 查询协议 (Client ↔ Node)

```
请求: "QUERY:<学号>"
响应: "<学生记录字符串>" 或 "NOT_FOUND"
```

### 成员管理协议 (Manager ↔ Node)

```
心跳: "HEARTBEAT:<节点ID>:<IP>:<端口>"
加入: "JOIN:<节点ID>:<端口>"
离开: "LEAVE:<节点ID>"
成员列表请求: "MEMBERSHIP_REQUEST"
成员列表响应: "MEMBERSHIP_RESPONSE:..."
```

## 环境变量配置

### Client环境变量

- `STORAGE_NODES`: 存储节点配置
  - 格式: `host1:port1:id1,host2:port2:id2,...`
  - 默认: `node1:8001:1,node2:8001:2,node3:8001:3`

### Manager环境变量

- `LEADER_HOST`: Leader节点主机地址
- `LEADER_PORT`: Leader节点端口

### Node环境变量

- `NODE_ID`: 节点唯一标识符
- `NODE_PORT`: 节点监听端口  
- `STUDENT_ID`: 学号标识

## 使用流程

### 1. 系统启动

```bash
./start.sh
```

启动顺序:
1. 创建网络和卷
2. 构建所有镜像
3. 启动Manager (Leader)
4. 启动所有Node
5. 准备Client容器

### 2. 数据操作

```bash
# 生成测试数据
docker-compose exec client java ClientMain generate 2353250 1000 /data/test-data.dat

# 分发数据
docker-compose exec client java ClientMain distribute /data/test-data.dat

# 查询数据
docker-compose exec client java ClientMain query
```

### 3. 监控和调试

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f node1

# 进入容器调试
docker-compose exec node1 bash
```

### 4. 系统清理

```bash
# 停止服务
docker-compose down

# 删除数据
docker-compose down -v
```

## 扩展指南

### 添加新的存储节点

1. 在 `docker-compose.yml` 中添加新的node服务
2. 更新客户端的 `STORAGE_NODES` 环境变量
3. 重新启动系统

### 修改代码

1. 编辑对应仓库的源代码
2. 重新构建镜像: `docker-compose build service_name`
3. 重新启动服务: `docker-compose up -d service_name`

### 性能调优

- 调整JVM内存参数（Dockerfile中）
- 修改网络缓冲区大小
- 优化数据分块大小
- 调整心跳间隔

## 故障排除

### 常见问题

1. **端口冲突**: 修改 docker-compose.yml 中的端口映射
2. **容器启动失败**: 检查日志 `docker-compose logs service_name`
3. **网络连接问题**: 确认容器在同一网络中
4. **数据分发失败**: 确认所有节点正常运行
5. **查询无结果**: 检查数据是否成功分发到节点

### 调试技巧

- 使用 `docker-compose logs -f` 实时查看日志
- 使用 `docker-compose exec service_name bash` 进入容器调试
- 检查容器间网络连通性: `docker-compose exec client ping node1`