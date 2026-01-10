import java.io.*;
import java.util.List;
import java.util.Random;

public class Client {
    private DatReader datReader;
    private List<StorageNodeInfo> storageNodes;
    private Random random;
    
    public static class StorageNodeInfo {
        public String host;
        public int port;
        public int nodeId;
        
        public StorageNodeInfo(String host, int port, int nodeId) {
            this.host = host;
            this.port = port;
            this.nodeId = nodeId;
        }
    }

    public Client(String datFilePath, List<StorageNodeInfo> storageNodes) throws IOException {
        this.datReader = new DatReader(datFilePath);
        this.storageNodes = storageNodes;
        this.random = new Random();
    }

    public void sendDatFile() {
        try {
            List<StudentRecord> records = datReader.read();
            // 发送到第一个可用的存储节点
            if (!storageNodes.isEmpty()) {
                NetSender netSender = new NetSender(storageNodes.get(0).host, storageNodes.get(0).port);
                netSender.sendStudentRecords(records);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 按照1024KB为一组发送原始数据文件到分布式存储节点
     */
    public void sendDatFileInChunks() {
        try {
            byte[] fileData = readFileAsBytes(datReader.getFilePath());
            int chunkSize = 1024 * 1024; // 1024KB
            int totalChunks = (int) Math.ceil((double) fileData.length / chunkSize);
            
            System.out.println("开始分发数据文件，总大小: " + fileData.length + " 字节，分为 " + totalChunks + " 块");
            
            for (int i = 0; i < totalChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, fileData.length);
                byte[] chunkData = new byte[end - start];
                System.arraycopy(fileData, start, chunkData, 0, end - start);
                
                // 随机选择主存储节点
                int primaryNodeIndex = random.nextInt(storageNodes.size());
                StorageNodeInfo primaryNode = storageNodes.get(primaryNodeIndex);
                
                // 选择备份节点（下一个节点，循环）
                int backupNodeIndex = (primaryNodeIndex + 1) % storageNodes.size();
                StorageNodeInfo backupNode = storageNodes.get(backupNodeIndex);
                
                // 发送到主节点
                sendChunkToNode(i, chunkData, primaryNode, true);
                
                // 发送到备份节点  
                sendChunkToNode(i, chunkData, backupNode, false);
                
                System.out.println("数据块 " + i + " 已发送到节点 " + primaryNode.nodeId + "(主) 和节点 " + backupNode.nodeId + "(备份)");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 读取文件为字节数组
     */
    private byte[] readFileAsBytes(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
    
    /**
     * 发送数据块到指定存储节点
     */
    private void sendChunkToNode(int chunkId, byte[] chunkData, StorageNodeInfo node, boolean isPrimary) {
        try {
            NetSender sender = new NetSender(node.host, node.port);
            sender.sendDataChunk(chunkId, chunkData, isPrimary);
        } catch (IOException e) {
            System.err.println("发送数据块到节点 " + node.nodeId + " 失败: " + e.getMessage());
        }
    }
}
