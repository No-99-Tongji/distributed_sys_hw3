import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

/**
 * 分布式查询客户端
 * 负责查询学生记录
 */
public class QueryClient {
    private List<Client.StorageNodeInfo> storageNodes;
    
    public QueryClient(List<Client.StorageNodeInfo> storageNodes) {
        this.storageNodes = storageNodes;
    }
    
    /**
     * 查询学生记录
     */
    public StudentRecord queryStudent(int studentId) {
        for (Client.StorageNodeInfo node : storageNodes) {
            try {
                StudentRecord record = queryFromNode(studentId, node);
                if (record != null) {
                    return record;
                }
            } catch (IOException e) {
                System.err.println("查询节点 " + node.nodeId + " 失败: " + e.getMessage());
            }
        }
        return null;
    }
    
    /**
     * 从指定节点查询
     */
    private StudentRecord queryFromNode(int studentId, Client.StorageNodeInfo node) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            // 发送查询请求
            String query = "QUERY:" + studentId;
            byte[] queryData = query.getBytes();
            
            InetAddress address = InetAddress.getByName(node.host);
            int queryPort = node.port + 1000; // 查询端口
            
            DatagramPacket queryPacket = new DatagramPacket(
                queryData, queryData.length, address, queryPort);
            socket.send(queryPacket);
            
            // 接收响应
            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            
            socket.setSoTimeout(5000); // 5秒超时
            socket.receive(responsePacket);
            
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            
            if ("NOT_FOUND".equals(response)) {
                return null;
            }
            
            return parseStudentRecord(response);
        }
    }
    
    /**
     * 解析学生记录字符串
     */
    private StudentRecord parseStudentRecord(String recordStr) {
        // 假设格式为: "学号: 123, 语文: 85.0, 数学: 90.0, 英语: 88.0, 综合: 87.5"
        try {
            String[] parts = recordStr.split(",");
            int studentId = Integer.parseInt(parts[0].split(":")[1].trim());
            float chinese = Float.parseFloat(parts[1].split(":")[1].trim());
            float math = Float.parseFloat(parts[2].split(":")[1].trim());
            float english = Float.parseFloat(parts[3].split(":")[1].trim());
            float comprehensive = Float.parseFloat(parts[4].split(":")[1].trim());
            
            return new StudentRecord(studentId, chinese, math, english, comprehensive);
        } catch (Exception e) {
            System.err.println("解析学生记录失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 启动交互式查询
     */
    public void startInteractiveQuery() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("分布式学生记录查询系统");
            System.out.println("可用存储节点数: " + storageNodes.size());
            
            while (true) {
                System.out.print("\n请输入学号 (输入 'quit' 退出): ");
                String input = scanner.nextLine().trim();
                
                if ("quit".equals(input)) {
                    break;
                }
                
                try {
                    int studentId = Integer.parseInt(input);
                    System.out.println("正在查询学号 " + studentId + "...");
                    
                    StudentRecord record = queryStudent(studentId);
                    
                    if (record != null) {
                        System.out.println("查询结果: " + record);
                    } else {
                        System.out.println("未找到学号 " + studentId + " 的记录");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("请输入有效的学号");
                }
            }
        }
        
        System.out.println("查询系统已退出");
    }
    
    public static void main(String[] args) {
        // 示例：配置存储节点
        List<Client.StorageNodeInfo> nodes = List.of(
            new Client.StorageNodeInfo("localhost", 8001, 1),
            new Client.StorageNodeInfo("localhost", 8002, 2),
            new Client.StorageNodeInfo("localhost", 8003, 3)
        );
        
        QueryClient client = new QueryClient(nodes);
        client.startInteractiveQuery();
    }
}