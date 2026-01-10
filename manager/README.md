# Manager仓库

这是分布式存储系统的管理组件，负责成员管理和集群协调。

## 功能

- **成员管理**: Master/Leader方式的集群成员管理
- **故障检测**: 基于心跳的故障检测机制
- **数据转发**: 数据包路由和转发

## 使用方法

### Docker方式运行

1. 构建镜像:
```bash
docker build -t distributed-manager .
```

2. 启动Leader节点:
```bash
docker run -p 9001:9001 --name manager --network distributed-network distributed-manager
```

3. 启动成员节点:
```bash
docker run -e LEADER_HOST=manager -e LEADER_PORT=9001 --network distributed-network distributed-manager java ManagerMain membership 2 9002
```

### 环境变量

- `LEADER_HOST`: Leader节点主机地址
- `LEADER_PORT`: Leader节点端口

## 文件结构

- `ManagerMain.java` - 主入口类
- `MembershipService.java` - 成员管理服务
- `Manager.java` - 管理器核心组件
- `NetSender.java` - 网络发送组件
- `NetReceiver.java` - 网络接收组件

## 协议

### 成员管理协议

- `HEARTBEAT:<nodeId>:<ip>:<port>` - 心跳消息
- `JOIN:<nodeId>:<port>` - 加入集群请求
- `LEAVE:<nodeId>` - 离开集群通知
- `MEMBERSHIP_REQUEST` - 请求成员列表
- `MEMBERSHIP_RESPONSE:...` - 成员列表响应