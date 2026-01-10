import java.util.*;

/**
 * 分布式存储系统主启动类
 */
public class DistributedSystemMain {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "client":
                    runClient(args);
                    break;
                case "storage":
                    runStorageNode(args);
                    break;
                case "query":
                    runQueryClient(args);
                    break;
                case "member":
                    runMembershipService(args);
                    break;
                case "demo":
                    runDemo();
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
     * 运行客户端（数据分发）
     */
    private static void runClient(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("用法: java DistributedSystemMain client <datFilePath> [节点配置]");
            return;
        }
        
        String datFilePath = args[1];
        
        // 配置存储节点
        List<Client.StorageNodeInfo> nodes = Arrays.asList(
            new Client.StorageNodeInfo("localhost", 8001, 1),
            new Client.StorageNodeInfo("localhost", 8002, 2),
            new Client.StorageNodeInfo("localhost", 8003, 3)
        );
        
        Client client = new Client(datFilePath, nodes);
        
        System.out.println("开始分发数据文件到分布式存储节点...");
        client.sendDatFileInChunks();
        System.out.println("数据分发完成");
    }
    
    /**
     * 运行存储节点
     */
    private static void runStorageNode(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("用法: java DistributedSystemMain storage <nodeId> <port> <studentId>");
            return;
        }
        
        int nodeId = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        String studentId = args[3];
        
        StorageNode node = new StorageNode(nodeId, port, studentId);
        node.start();
    }
    
    /**
     * 运行查询客户端
     */
    private static void runQueryClient(String[] args) throws Exception {
        List<Client.StorageNodeInfo> nodes = Arrays.asList(
            new Client.StorageNodeInfo("localhost", 8001, 1),
            new Client.StorageNodeInfo("localhost", 8002, 2),
            new Client.StorageNodeInfo("localhost", 8003, 3)
        );
        
        QueryClient queryClient = new QueryClient(nodes);
        queryClient.startInteractiveQuery();
    }
    
    /**
     * 运行成员管理服务
     */
    private static void runMembershipService(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("用法: java DistributedSystemMain member <nodeId> <port> [leader]");
            return;
        }
        
        int nodeId = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        boolean isLeader = args.length > 3 && "leader".equals(args[3]);
        
        MembershipService service = new MembershipService(nodeId, port, isLeader);
        service.start();
    }
    
    /**
     * 运行演示
     */
    private static void runDemo() {
        System.out.println("=== 分布式存储系统演示 ===");
        System.out.println();
        
        System.out.println("1. 启动存储节点:");
        System.out.println("   终端1: java DistributedSystemMain storage 1 8001 2353250");
        System.out.println("   终端2: java DistributedSystemMain storage 2 8002 2353250");
        System.out.println("   终端3: java DistributedSystemMain storage 3 8003 2353250");
        System.out.println();
        
        System.out.println("2. 分发数据:");
        System.out.println("   java DistributedSystemMain client src/main/resources/2353250-hw2.dat");
        System.out.println();
        
        System.out.println("3. 查询数据:");
        System.out.println("   java DistributedSystemMain query");
        System.out.println();
        
        System.out.println("4. 成员管理 (可选):");
        System.out.println("   Leader: java DistributedSystemMain member 1 9001 leader");
        System.out.println("   Member: java DistributedSystemMain member 2 9002");
        System.out.println();
        
        System.out.println("=== 功能特性 ===");
        System.out.println("✓ 数据按1024KB分块存储");
        System.out.println("✓ 随机分配主存储节点");
        System.out.println("✓ 自动复制到备份节点");
        System.out.println("✓ 维护数据块索引");
        System.out.println("✓ 分布式查询功能");
        System.out.println("✓ 成员管理和故障检测");
    }
    
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("分布式存储系统");
        System.out.println("用法: java DistributedSystemMain <command> [options]");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  client <datFile>     - 启动客户端，分发数据");
        System.out.println("  storage <id> <port> <studentId> - 启动存储节点");
        System.out.println("  query               - 启动查询客户端");
        System.out.println("  member <id> <port> [leader] - 启动成员管理服务");
        System.out.println("  demo                - 显示使用示例");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java DistributedSystemMain demo");
    }
}