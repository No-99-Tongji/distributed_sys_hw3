import java.net.*;
import java.io.IOException;

public class NetReceiver {
    private int port;
    
    NetReceiver(int listeningPort) {
        port = listeningPort;
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
}