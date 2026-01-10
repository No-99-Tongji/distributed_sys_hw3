import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 存储节点类
 * 负责存储数据块并维护索引
 */
public class StorageNode {
    private int nodeId;
    private int port;
    private String studentId;
    private DatagramSocket socket;
    private File datFile;
    private File idxFile;
    private boolean running = true;
    
    // 索引项结构
    public static class IndexEntry {
        public int chunkId;
        public long pointer;
        public int size;
        
        public IndexEntry(int chunkId, long pointer, int size) {
            this.chunkId = chunkId;
            this.pointer = pointer;
            this.size = size;
        }
        
        @Override
        public String toString() {
            return chunkId + "," + pointer + "," + size;
        }
        
        public static IndexEntry fromString(String line) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                return new IndexEntry(
                    Integer.parseInt(parts[0]),
                    Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2])
                );
            }
            return null;
        }
    }
    
    public StorageNode(int nodeId, int port, String studentId) throws SocketException {
        this.nodeId = nodeId;
        this.port = port;
        this.studentId = studentId;
        this.socket = new DatagramSocket(port);
        
        // 创建数据和索引文件
        this.datFile = new File(studentId + "-hw3-" + nodeId + ".dat");
        this.idxFile = new File(studentId + "-hw3-" + nodeId + ".idx");
    }
    
    /**
     * 启动存储节点
     */
    public void start() {
        System.out.println("存储节点 " + nodeId + " 启动在端口 " + port);
        
        // 启动数据接收线程
        Thread dataThread = new Thread(this::handleDataPackets);
        dataThread.start();
        
        // 启动查询处理线程
        Thread queryThread = new Thread(this::handleQueries);
        queryThread.start();
        
        // 主线程处理用户输入
        handleUserInput();
    }
    
    /**
     * 处理数据包接收
     */
    private void handleDataPackets() {
        byte[] buffer = new byte[1024 * 1024 + 1024]; // 1MB + 头部空间
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                processDataPacket(packet);
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 处理查询请求
     */
    private void handleQueries() {
        // 启动查询服务器在另一个端口
        try (DatagramSocket querySocket = new DatagramSocket(port + 1000)) {
            byte[] buffer = new byte[1024];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    querySocket.receive(packet);
                    
                    processQuery(packet, querySocket);
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 处理数据包
     */
    private void processDataPacket(DatagramPacket packet) throws IOException {
        byte[] data = packet.getData();
        int length = packet.getLength();
        
        if (length < 9) { // 至少需要 chunkId(4) + isPrimary(1) + dataLength(4)
            return;
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        int chunkId = buffer.getInt();
        boolean isPrimary = buffer.get() == 1;
        int dataLength = buffer.getInt();
        
        byte[] chunkData = new byte[dataLength];
        buffer.get(chunkData);
        
        // 存储数据块
        storeChunk(chunkId, chunkData, isPrimary);
        
        System.out.println("收到数据块 " + chunkId + " (" + 
                          (isPrimary ? "主" : "备份") + ")，大小: " + dataLength + " 字节");
    }
    
    /**
     * 存储数据块
     */
    private synchronized void storeChunk(int chunkId, byte[] chunkData, boolean isPrimary) throws IOException {
        // 获取当前.dat文件大小作为指针
        long pointer = datFile.length();
        
        // 追加数据到.dat文件
        try (FileOutputStream fos = new FileOutputStream(datFile, true)) {
            fos.write(chunkData);
        }
        
        // 更新索引文件
        IndexEntry entry = new IndexEntry(chunkId, pointer, chunkData.length);
        try (FileWriter fw = new FileWriter(idxFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry.toString());
        }
    }
    
    /**
     * 处理查询请求
     */
    private void processQuery(DatagramPacket queryPacket, DatagramSocket querySocket) throws IOException {
        String query = new String(queryPacket.getData(), 0, queryPacket.getLength());
        
        if (query.startsWith("QUERY:")) {
            int targetStudentId = Integer.parseInt(query.substring(6));
            StudentRecord record = findStudentRecord(targetStudentId);
            
            String response = record != null ? record.toString() : "NOT_FOUND";
            byte[] responseData = response.getBytes();
            
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, responseData.length,
                queryPacket.getAddress(), queryPacket.getPort()
            );
            querySocket.send(responsePacket);
        }
    }
    
    /**
     * 查找学生记录
     */
    private StudentRecord findStudentRecord(int targetStudentId) throws IOException {
        List<IndexEntry> indices = loadIndices();
        
        try (RandomAccessFile raf = new RandomAccessFile(datFile, "r")) {
            for (IndexEntry entry : indices) {
                raf.seek(entry.pointer);
                byte[] chunkData = new byte[entry.size];
                raf.readFully(chunkData);
                
                // 解析学生记录
                List<StudentRecord> records = parseChunkData(chunkData);
                for (StudentRecord record : records) {
                    if (record.studentId == targetStudentId) {
                        return record;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 解析数据块为学生记录
     */
    private List<StudentRecord> parseChunkData(byte[] chunkData) {
        List<StudentRecord> records = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(chunkData);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        int recordSize = 20; // int(4) + 4*float(4)
        int recordCount = chunkData.length / recordSize;
        
        for (int i = 0; i < recordCount; i++) {
            if (buffer.remaining() >= recordSize) {
                int studentId = buffer.getInt();
                float chineseScore = buffer.getFloat();
                float mathScore = buffer.getFloat();
                float englishScore = buffer.getFloat();
                float comprehensiveScore = buffer.getFloat();
                
                records.add(new StudentRecord(studentId, chineseScore, mathScore, 
                                            englishScore, comprehensiveScore));
            }
        }
        
        return records;
    }
    
    /**
     * 加载索引文件
     */
    private List<IndexEntry> loadIndices() throws IOException {
        List<IndexEntry> indices = new ArrayList<>();
        
        if (idxFile.exists()) {
            try (Scanner scanner = new Scanner(idxFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        IndexEntry entry = IndexEntry.fromString(line);
                        if (entry != null) {
                            indices.add(entry);
                        }
                    }
                }
            }
        }
        
        return indices;
    }
    
    /**
     * 处理用户输入
     */
    private void handleUserInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                System.out.println("\n存储节点 " + nodeId + " 命令:");
                System.out.println("1. status - 显示状态");
                System.out.println("2. index - 显示索引");
                System.out.println("3. quit - 退出");
                System.out.print("请输入命令: ");
                
                String command = scanner.nextLine().trim();
                
                switch (command) {
                    case "status":
                        showStatus();
                        break;
                    case "index":
                        showIndex();
                        break;
                    case "quit":
                        shutdown();
                        return;
                    default:
                        System.out.println("无效命令");
                }
            }
        }
    }
    
    /**
     * 显示节点状态
     */
    private void showStatus() {
        System.out.println("节点ID: " + nodeId);
        System.out.println("端口: " + port);
        System.out.println("数据文件: " + datFile.getName() + " (大小: " + datFile.length() + " 字节)");
        System.out.println("索引文件: " + idxFile.getName() + " (存在: " + idxFile.exists() + ")");
    }
    
    /**
     * 显示索引信息
     */
    private void showIndex() {
        try {
            List<IndexEntry> indices = loadIndices();
            System.out.println("索引条目数: " + indices.size());
            for (IndexEntry entry : indices) {
                System.out.println("块ID: " + entry.chunkId + 
                                 ", 指针: " + entry.pointer + 
                                 ", 大小: " + entry.size);
            }
        } catch (IOException e) {
            System.err.println("读取索引失败: " + e.getMessage());
        }
    }
    
    /**
     * 关闭节点
     */
    public void shutdown() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        System.out.println("存储节点 " + nodeId + " 已关闭");
    }
    
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("用法: java StorageNode <nodeId> <port> <studentId>");
            return;
        }
        
        try {
            int nodeId = Integer.parseInt(args[0]);
            int port = Integer.parseInt(args[1]);
            String studentId = args[2];
            
            StorageNode node = new StorageNode(nodeId, port, studentId);
            node.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}