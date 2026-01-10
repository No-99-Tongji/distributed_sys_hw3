# 客户端仓库

这是分布式存储系统的客户端组件，负责数据分发和查询功能。

## 功能

- **数据分发**: 将数据文件按1024KB分块分发到存储节点
- **查询服务**: 支持按学号查询学生记录
- **测试数据生成**: 生成测试数据用于实验

## 使用方法

### Docker方式运行

1. 构建镜像:
```bash
docker build -t distributed-client .
```

2. 生成测试数据:
```bash
docker run -v $(pwd)/data:/data distributed-client java ClientMain generate 2353250 1000 /data/test-data.dat
```

3. 分发数据:
```bash
docker run -v $(pwd)/data:/data --network distributed-network distributed-client java ClientMain distribute /data/test-data.dat
```

4. 查询数据:
```bash
docker run -it --network distributed-network distributed-client java ClientMain query
```

### 环境变量

- `STORAGE_NODES`: 存储节点配置，格式为 `host1:port1:id1,host2:port2:id2`

## 文件结构

- `ClientMain.java` - 主入口类
- `Client.java` - 数据分发客户端
- `QueryClient.java` - 查询客户端
- `TestDataGenerator.java` - 测试数据生成器
- `StudentRecord.java` - 学生记录数据结构
- `DatReader.java` - 数据文件读取器
- `NetSender.java` - 网络发送组件