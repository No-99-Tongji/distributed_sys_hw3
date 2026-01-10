import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.ByteArrayOutputStream;

public class NetReceiver {
    private int port;
    NetReceiver(int listeningPort) {
        port = listeningPort;
    }

private List<StudentRecord> parseRecords(DatagramPacket packet) {
    List<StudentRecord> records = new ArrayList<>();
    byte[] receivedData = packet.getData();
    int actualLength = packet.getLength();  // 实际数据长度
    
    // 每条记录大小：int(4字节) + float(4字节) * 4 = 20字节
    int recordSize = 20;  // 4 + 4 * 4
    
    if (actualLength % recordSize != 0) {
        System.err.println("警告: 数据长度(" + actualLength + 
                          ")不是记录大小的整数倍(" + recordSize + ")");
    }
    
    // 使用ByteBuffer解析，设置字节序
    ByteBuffer buffer = ByteBuffer.wrap(receivedData, 0, actualLength);
    buffer.order(ByteOrder.BIG_ENDIAN);  // 或 LITTLE_ENDIAN，需与发送方一致
    
    int recordCount = actualLength / recordSize;
    
    for (int i = 0; i < recordCount; i++) {
        try {
            int studentId = buffer.getInt();               // 4字节
            float chineseScore = buffer.getFloat();        // 4字节
            float mathScore = buffer.getFloat();           // 4字节
            float englishScore = buffer.getFloat();        // 4字节
            float comprehensiveScore = buffer.getFloat();  // 4字节
            
            StudentRecord record = new StudentRecord(
                studentId, chineseScore, mathScore, 
                englishScore, comprehensiveScore
            );
            records.add(record);
            
        } catch (BufferUnderflowException e) {
            System.err.println("解析第 " + i + " 条记录时数据不足");
            break;
        }
    }
    
    return records;
}

    public List<StudentRecord> listen() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            // 准备接收数据
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            // 接收数据（阻塞方法）
            socket.receive(packet);
            return parseRecords(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 接收单个原始数据包（包含序列号的分块数据）
     * 返回完整的数据包数据，用于转发
     */
    public byte[] listenRawPacket() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1500]; // UDP最大安全大小
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            // 接收数据包
            socket.receive(packet);
            
            // 返回实际接收到的数据
            byte[] actualData = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, actualData, 0, packet.getLength());
            
            return actualData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 监听并接收分块数据，直到收到结束标记
     */
    public List<StudentRecord> listenChunks() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            Map<Integer, byte[]> chunks = new TreeMap<>(); // 使用TreeMap保持顺序
            boolean isComplete = false;
            int expectedTotalChunks = -1;
            
            System.out.println("开始接收分块数据...");
            
            while (!isComplete) {
                byte[] buffer = new byte[1500]; // UDP最大安全大小
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                // 接收数据包
                socket.receive(packet);
                
                byte[] data = packet.getData();
                int length = packet.getLength();
                
                if (length < 4) {
                    System.err.println("接收到的数据包太小，忽略");
                    continue;
                }
                
                // 解析序列号
                ByteBuffer seqBuffer = ByteBuffer.wrap(data, 0, 4);
                seqBuffer.order(ByteOrder.BIG_ENDIAN);
                int sequenceNumber = seqBuffer.getInt();
                
                // 检查是否为结束标记
                if (length == 8) {
                    ByteBuffer endBuffer = ByteBuffer.wrap(data, 4, 4);
                    endBuffer.order(ByteOrder.BIG_ENDIAN);
                    int marker = endBuffer.getInt();
                    
                    if (marker == -1) {
                        // 这是结束标记
                        expectedTotalChunks = sequenceNumber;
                        System.out.println("收到结束标记，期望总块数: " + expectedTotalChunks);
                        
                        // 检查是否已收齐所有数据块
                        if (chunks.size() == expectedTotalChunks) {
                            isComplete = true;
                        }
                        continue;
                    }
                }
                
                // 存储数据块（去掉序列号）
                byte[] chunkData = new byte[length - 4];
                System.arraycopy(data, 4, chunkData, 0, length - 4);
                chunks.put(sequenceNumber, chunkData);
                
                System.out.println("收到第 " + sequenceNumber + " 块，大小: " + chunkData.length + " 字节");
                
                // 如果已知总块数且收齐了所有块，则完成
                if (expectedTotalChunks != -1 && chunks.size() == expectedTotalChunks) {
                    isComplete = true;
                }
            }
            
            // 重组数据
            byte[] completeData = reassembleChunks(chunks);
            System.out.println("数据重组完成，总大小: " + completeData.length + " 字节");
            
            // 解析重组后的数据
            return parseDataAsRecords(completeData);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
    /**
     * 重组数据块
     */
    private byte[] reassembleChunks(Map<Integer, byte[]> chunks) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for (Map.Entry<Integer, byte[]> entry : chunks.entrySet()) {
            baos.write(entry.getValue(), 0, entry.getValue().length);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 解析重组后的原始数据为StudentRecord列表
     */
    private List<StudentRecord> parseDataAsRecords(byte[] data) {
        List<StudentRecord> records = new ArrayList<>();
        
        // 每条记录大小：int(4字节) + float(4字节) * 4 = 20字节
        int recordSize = 20;
        
        if (data.length % recordSize != 0) {
            System.err.println("警告: 数据长度(" + data.length + 
                              ")不是记录大小的整数倍(" + recordSize + ")");
        }
        
        // 使用ByteBuffer解析
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        int recordCount = data.length / recordSize;
        
        for (int i = 0; i < recordCount; i++) {
            try {
                int studentId = buffer.getInt();
                float chineseScore = buffer.getFloat();
                float mathScore = buffer.getFloat();
                float englishScore = buffer.getFloat();
                float comprehensiveScore = buffer.getFloat();
                
                StudentRecord record = new StudentRecord(
                    studentId, chineseScore, mathScore, 
                    englishScore, comprehensiveScore
                );
                records.add(record);
                
            } catch (BufferUnderflowException e) {
                System.err.println("解析第 " + i + " 条记录时数据不足");
                break;
            }
        }
        
        System.out.println("成功解析 " + records.size() + " 条学生记录");
        return records;
    }
}