import java.util.*;
import java.io.*;
import java.net.*;

/**
 * 客户端主启动类
 */
public class ClientMain {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "distribute":
                    runDataDistribution(args);
                    break;
                case "query":
                    runQueryClient(args);
                    break;
                case "generate":
                    runTestDataGenerator(args);
                    break;
                case "test-manager":
                    testManagerConnection();
                    break;
                default:
                    System.err.println("未知命令: " + command);
                    printUsage();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 运行数据分发
     */
    private static void runDataDistribution(String[] args) throws Exception {
        String datFilePath = args.length > 1 ? args[1] : "/data/test-data.dat";
        
        // 首先尝试从Manager获取节点列表
        List<Client.StorageNodeInfo> nodes = getNodesFromManager();
        
        // 如果Manager不可用，降级使用静态配置
        if (nodes.isEmpty()) {
            System.out.println("Manager不可用，使用静态配置");
            String nodesEnv = System.getenv("STORAGE_NODES");
            nodes = parseStorageNodes(nodesEnv);
        }
        
        if (nodes.isEmpty()) {
            System.err.println("无法获取存储节点信息");
            return;
        }
        
        Client client = new Client(datFilePath, nodes);
        
        System.out.println("开始分发数据文件到分布式存储节点...");
        client.sendDatFileInChunks();
        System.out.println("数据分发完成");
    }
    
    /**
     * 运行查询客户端
     */
    private static void runQueryClient(String[] args) throws Exception {
        // 首先尝试从Manager获取节点列表
        List<Client.StorageNodeInfo> nodes = getNodesFromManager();
        
        // 如果Manager不可用，降级使用静态配置
        if (nodes.isEmpty()) {
            System.out.println("Manager不可用，使用静态配置");
            String nodesEnv = System.getenv("STORAGE_NODES");
            nodes = parseStorageNodes(nodesEnv);
        }
        
        if (nodes.isEmpty()) {
            System.err.println("无法获取存储节点信息");
            return;
        }
        
        QueryClient queryClient = new QueryClient(nodes);
        queryClient.startInteractiveQuery();
    }
    
    /**
     * 运行测试数据生成器
     */
    private static void runTestDataGenerator(String[] args) throws Exception {
        String studentId = args.length > 1 ? args[1] : "2353250";
        int numRecords = args.length > 2 ? Integer.parseInt(args[2]) : 1000;
        String outputPath = args.length > 3 ? args[3] : "/data/test-data.dat";
        
        TestDataGenerator.generateTestData(studentId, numRecords, outputPath);
    }
    
    /**
     * 测试与Manager的连接
     */
    private static void testManagerConnection() {
        System.out.println("正在测试与Manager的连接...");
        List<Client.StorageNodeInfo> nodes = getNodesFromManager();
        
        if (!nodes.isEmpty()) {
            System.out.println("✅ 成功连接到Manager，获取到 " + nodes.size() + " 个存储节点:");
            for (Client.StorageNodeInfo node : nodes) {
                System.out.println("  - 节点 " + node.nodeId + ": " + node.host + ":" + node.port);
            }
        } else {
            System.out.println("❌ 无法连接到Manager或没有可用的存储节点");
        }
    }
    
    /**
     * 从Manager获取活跃的存储节点列表
     */
    private static List<Client.StorageNodeInfo> getNodesFromManager() {
        String managerHost = System.getenv("MANAGER_HOST");
        String managerPortStr = System.getenv("MANAGER_PORT");
        
        if (managerHost == null) managerHost = "manager";
        int managerPort = managerPortStr != null ? Integer.parseInt(managerPortStr) : 9000;
        
        try {
            System.out.println("正在从Manager获取节点列表: " + managerHost + ":" + managerPort);
            
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(8000); // 增加到8秒超时
            
            // 发送节点列表请求
            String request = "MEMBERSHIP_REQUEST";
            byte[] requestData = request.getBytes();
            InetAddress managerAddress = InetAddress.getByName(managerHost);
            
            System.out.println("解析Manager地址: " + managerAddress.getHostAddress());
            
            DatagramPacket requestPacket = new DatagramPacket(
                requestData, requestData.length, managerAddress, managerPort);
            
            System.out.println("发送请求: " + request);
            socket.send(requestPacket);
            System.out.println("请求已发送，等待响应...");
            
            // 接收响应
            byte[] responseBuffer = new byte[2048];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);
            
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("收到响应: " + response);
            socket.close();
            
            return parseMembershipResponse(response);
            
        } catch (Exception e) {
            System.err.println("无法从Manager获取节点列表: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 解析Manager返回的成员列表响应
     */
    private static List<Client.StorageNodeInfo> parseMembershipResponse(String response) {
        List<Client.StorageNodeInfo> nodes = new ArrayList<>();
        
        if (!response.startsWith("MEMBERSHIP_RESPONSE")) {
            return nodes;
        }
        
        String[] parts = response.split(":");
        // 格式: MEMBERSHIP_RESPONSE:nodeId1:ip1:port1:nodeId2:ip2:port2:...
        for (int i = 1; i < parts.length; i += 3) {
            if (i + 2 < parts.length) {
                try {
                    int nodeId = Integer.parseInt(parts[i]);
                    String ip = parts[i + 1];
                    int port = Integer.parseInt(parts[i + 2]);
                    
                    // 过滤掉Manager自身，只返回存储节点
                    if (port != 9000) { // Manager使用9000端口
                        nodes.add(new Client.StorageNodeInfo(ip, port, nodeId));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("解析节点信息失败: " + parts[i] + "," + parts[i+1] + "," + parts[i+2]);
                }
            }
        }
        
        System.out.println("从Manager获取到 " + nodes.size() + " 个存储节点");
        return nodes;
    }
    
    /**
     * 解析存储节点配置
     */
    private static List<Client.StorageNodeInfo> parseStorageNodes(String nodesEnv) {
        List<Client.StorageNodeInfo> nodes = new ArrayList<>();
        
        if (nodesEnv == null || nodesEnv.trim().isEmpty()) {
            // 默认配置
            nodes.add(new Client.StorageNodeInfo("node1", 8001, 1));
            nodes.add(new Client.StorageNodeInfo("node2", 8002, 2));
            nodes.add(new Client.StorageNodeInfo("node3", 8003, 3));
            return nodes;
        }
        
        // 格式: node1:8001:1,node2:8002:2,node3:8003:3
        String[] nodeSpecs = nodesEnv.split(",");
        for (int i = 0; i < nodeSpecs.length; i++) {
            String[] parts = nodeSpecs[i].trim().split(":");
            if (parts.length == 3) {
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                int nodeId = Integer.parseInt(parts[2]);
                nodes.add(new Client.StorageNodeInfo(host, port, nodeId));
            }
        }
        
        return nodes;
    }
    
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("客户端使用说明");
        System.out.println("用法: java ClientMain <command> [options]");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  distribute [datFile]  - 分发数据到存储节点");
        System.out.println("  query                - 启动查询客户端");
        System.out.println("  generate [studentId] [numRecords] [outputPath] - 生成测试数据");
        System.out.println("  test-manager         - 测试与Manager的连接");
        System.out.println();
        System.out.println("环境变量:");
        System.out.println("  MANAGER_HOST - Manager服务地址 (默认: manager)");
        System.out.println("  MANAGER_PORT - Manager服务端口 (默认: 9000)");
        System.out.println("  STORAGE_NODES - 存储节点配置 (备用，格式: host1:port1:id1,host2:port2:id2,...)");
    }
}