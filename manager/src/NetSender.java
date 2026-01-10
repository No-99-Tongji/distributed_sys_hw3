import java.io.*;
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