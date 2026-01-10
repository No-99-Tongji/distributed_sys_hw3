import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class NetSender {
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;
    
    /**
     * 构造函数
     * @param targetHost 目标主机地址
     * @param targetPort 目标端口
     */
    public NetSender(String targetHost, int targetPort) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket();
        this.targetAddress = InetAddress.getByName(targetHost);
        this.targetPort = targetPort;
    }
    
    /**
     * 发送学生记录列表
     * 格式：每条记录20字节 = int studentId(4) + 4个float成绩(每个4字节)
     */
    public void sendStudentRecords(List<StudentRecord> records) throws IOException {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 创建数据
        byte[] data = createPacketData(records);
        
        // 创建并发送数据包
        DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
        socket.send(packet);
    }
    
    /**
     * 创建数据包数据
     */
    private byte[] createPacketData(List<StudentRecord> records) {
        // 每条记录: int(4字节) + 4*float(每个4字节) = 20字节
        int recordSize = 4 + 4 * 4;  // 20字节
        int totalSize = records.size() * recordSize;
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.BIG_ENDIAN);  // 网络字节序
        
        for (StudentRecord record : records) {
            buffer.putInt(record.studentId);
            buffer.putFloat(record.chineseScore);
            buffer.putFloat(record.mathScore);
            buffer.putFloat(record.englishScore);
            buffer.putFloat(record.comprehensiveScore);
        }
        
        return buffer.array();
    }
    
    /**
     * 发送数据块到存储节点
     */
    public void sendDataChunk(int chunkId, byte[] chunkData, boolean isPrimary) throws IOException {
        // 创建数据包：chunkId(4字节) + isPrimary(1字节) + 数据长度(4字节) + 数据
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4 + chunkData.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        buffer.putInt(chunkId);
        buffer.put((byte) (isPrimary ? 1 : 0));
        buffer.putInt(chunkData.length);
        buffer.put(chunkData);
        
        byte[] packetData = buffer.array();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, targetAddress, targetPort);
        socket.send(packet);
    }
    
    /**
     * 转发原始数据包
     * @param rawData 原始数据包数据（包含序列号的分块数据）
     */
    public void forwardRawPacket(byte[] rawData) throws IOException {
        if (rawData == null || rawData.length == 0) {
            return;
        }
        
        // 直接转发原始数据包
        DatagramPacket packet = new DatagramPacket(rawData, rawData.length, targetAddress, targetPort);
        socket.send(packet);
    }

    /**
     * 关闭socket
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}