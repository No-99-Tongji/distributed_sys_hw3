# 分布式存储系统 - Docker版本

## 系统概述

这是一个容器化的分布式数据存储系统，支持动态节点发现和自动故障检测：

- **client/** - 客户端组件（数据分发和查询，支持动态节点发现）
- **manager/** - 管理组件（成员管理、集群协调和故障检测）
- **node/** - 存储节点组件（数据存储和查询处理）

## 主要功能

- ✅ **动态节点发现** - Client自动从Manager获取活跃的存储节点
- ✅ **故障自动检测** - Manager监控节点状态，自动移除故障节点
- ✅ **容错备份** - 数据自动备份到多个节点，支持故障恢复
- ✅ **负载均衡** - 智能选择存储节点，避免单点负载过重
- ✅ **容器化部署** - 完整的Docker环境，开箱即用

## 快速开始

### 1. 环境要求

- Docker 20.0+
- docker-compose 1.27+
- 至少 2GB 可用内存
- 至少 1GB 可用磁盘空间

### 2. 启动系统

```bash
# 给脚本执行权限
chmod +x start.sh demo.sh

# 启动整个分布式系统
./start.sh
```

### 3. 运行演示

```bash
# 运行完整的演示实验
./demo.sh
```

### 4. 手动操作

```bash
# 测试动态节点发现
docker-compose exec client java ClientMain test-manager

# 生成测试数据
docker-compose exec client java ClientMain generate 2353250 1000 /data/test-data.dat

# 分发数据（自动使用动态发现的节点）
docker-compose exec client java ClientMain distribute /data/test-data.dat

# 交互式查询
docker-compose exec client java ClientMain query
```

### 5. 测试动态节点发现

```bash
# 运行完整的动态发现测试
./test-dynamic-discovery.sh
```

## 文件结构

```
src/main/java/
├── Client.java              # 数据分发客户端
├── StorageNode.java         # 存储节点
├── QueryClient.java         # 查询客户端
├── MembershipService.java   # 成员管理服务
├── NetSender.java          # 网络发送器
├── NetReceiver.java        # 网络接收器
├── DatReader.java          # 数据文件读取器
├── DatWriter.java          # 数据文件写入器
├── StudentRecord.java      # 学生记录数据结构
├── TestDataGenerator.java  # 测试数据生成器
└── DistributedSystemMain.java # 主启动类

src/main/resources/
├── 2353250-hw2.dat         # 原始数据文件
├── 2353250-hw2.txt         # 可读格式数据文件
└── requirement.md          # 项目需求说明
```

## 动态节点发现机制

### 工作流程

1. **Client启动** → 发送 `MEMBERSHIP_REQUEST` 给Manager
2. **Manager响应** → 返回当前活跃的存储节点列表
3. **智能选择** → Client根据节点状态智能选择存储目标
4. **故障容错** → 如果Manager不可用，自动降级使用静态配置

### 优势对比

| 特性 | 静态配置 | 动态发现 |
|------|----------|----------|
| 节点故障感知 | ❌ 手动配置 | ✅ 自动检测 |
| 新节点发现 | ❌ 需要重启 | ✅ 实时发现 |
| 负载均衡 | ❌ 盲目分配 | ✅ 智能选择 |
| 容错能力 | ❌ 单点故障 | ✅ 自动恢复 |

### 环境变量配置

```bash
# Manager连接配置
MANAGER_HOST=manager        # Manager服务地址
MANAGER_PORT=9000          # Manager服务端口

# 备用静态配置（Manager不可用时使用）
STORAGE_NODES=node1:8001:1,node2:8002:2,node3:8003:3
```

## 数据格式

### 原始数据文件格式
每条学生记录占20字节：
- 学号 (int, 4字节)
- 语文成绩 (float, 4字节)
- 数学成绩 (float, 4字节)
- 英语成绩 (float, 4字节)
- 综合成绩 (float, 4字节)

### 存储节点文件格式

#### 数据文件 (`学号-hw3-n.dat`)
顺序存储所有接收到的数据块内容。

#### 索引文件 (`学号-hw3-n.idx`)
每行一个索引条目，格式：`<块ID>,<指针位置>,<数据大小>`

例如：
```
0,0,1048576
1,1048576,1048576
2,2097152,524288
```

## 网络协议

### 数据传输协议 (UDP)
```
数据包格式:
[块ID(4字节)][是否主存储(1字节)][数据长度(4字节)][数据内容]
```

### 查询协议 (UDP)
```
查询请求: "QUERY:<学号>"
查询响应: "<学生记录字符串>" 或 "NOT_FOUND"
```

### 成员管理协议 (UDP)
```
心跳: "HEARTBEAT:<节点ID>:<IP>:<端口>"
加入: "JOIN:<节点ID>:<端口>"
离开: "LEAVE:<节点ID>"
```

## 容错机制

1. **数据冗余**：每个数据块存储在两个节点上
2. **故障检测**：基于心跳的故障检测机制
3. **查询容错**：查询失败时自动尝试其他节点
4. **超时处理**：网络通信包含超时机制

## 性能特性

- **分块大小**：1024KB，优化网络传输效率
- **并发处理**：每个存储节点支持多线程处理
- **内存效率**：流式处理大文件，避免内存溢出
- **网络优化**：使用UDP协议减少网络开销

## 扩展功能

### 成员管理演示
```bash
# 启动Leader节点
java -cp target/classes DistributedSystemMain member 1 9001 leader

# 启动成员节点
java -cp target/classes DistributedSystemMain member 2 9002
java -cp target/classes DistributedSystemMain member 3 9003

# 在成员节点中执行加入命令
join localhost 9001
```

## 故障处理

### 常见问题

1. **端口被占用**
   - 修改启动命令中的端口号
   - 使用 `lsof -i :端口号` 查看端口使用情况

2. **数据文件不存在**
   - 先运行 `TestDataGenerator` 生成测试数据
   - 确保文件路径正确

3. **查询无结果**
   - 确保存储节点正在运行
   - 检查数据是否已成功分发

### 调试信息

各组件都包含详细的日志输出，便于调试：
- 数据分发进度
- 存储操作状态
- 查询执行路径
- 成员管理事件

## 项目特色

1. **完整的分布式架构**：实现了数据存储、查询、成员管理的完整功能
2. **高可用性设计**：数据冗余存储和故障检测机制
3. **模块化设计**：各组件独立，易于扩展和维护
4. **用户友好**：提供了完整的命令行界面和使用示例
5. **性能优化**：基于UDP的高效网络通信

---

**注意**：本系统为学术项目，主要用于学习分布式系统概念，生产环境使用需要进一步的安全性和可靠性改进。