import java.net.*;
import java.io.*;

public class JoinTest {
    public static void main(String[] args) {
        try {
            System.out.println("开始JOIN测试...");
            
            DatagramSocket socket = new DatagramSocket();
            
            String message = "JOIN:99:8099";
            byte[] data = message.getBytes();
            
            InetAddress managerAddr = InetAddress.getByName("manager");
            DatagramPacket packet = new DatagramPacket(data, data.length, managerAddr, 9000);
            
            System.out.println("发送JOIN消息: " + message);
            socket.send(packet);
            
            socket.close();
            System.out.println("JOIN消息已发送");
            
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}