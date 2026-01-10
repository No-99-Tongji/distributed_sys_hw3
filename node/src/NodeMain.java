/**
 * 存储节点主启动类
 */
public class NodeMain {
    
    public static void main(String[] args) {
        try {
            // 从环境变量或命令行参数获取配置
            int nodeId = args.length > 0 ? Integer.parseInt(args[0]) : 
                        Integer.parseInt(System.getenv().getOrDefault("NODE_ID", "1"));
            
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 
                      Integer.parseInt(System.getenv().getOrDefault("NODE_PORT", "8001"));
            
            String studentId = args.length > 2 ? args[2] : 
                             System.getenv().getOrDefault("STUDENT_ID", "2353250");
            
            System.out.println("启动存储节点:");
            System.out.println("  节点ID: " + nodeId);
            System.out.println("  端口: " + port);
            System.out.println("  学号: " + studentId);
            
            StorageNode node = new StorageNode(nodeId, port, studentId);
            
            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("正在关闭存储节点...");
                node.shutdown();
            }));
            
            node.start();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}