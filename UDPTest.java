import java.net.*;
import java.io.*;

public class UDPTest {
    public static void main(String[] args) {
        try {
            System.out.println("开始UDP测试...");
            
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000); // 3秒超时
            
            String message = "MEMBERSHIP_REQUEST";
            byte[] data = message.getBytes();
            
            System.out.println("解析manager地址...");
            InetAddress managerAddr = InetAddress.getByName("manager");
            System.out.println("Manager地址: " + managerAddr.getHostAddress());
            
            DatagramPacket packet = new DatagramPacket(data, data.length, managerAddr, 9000);
            
            System.out.println("发送请求: " + message);
            socket.send(packet);
            System.out.println("请求已发送");
            
            // 接收响应
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            
            System.out.println("等待响应...");
            socket.receive(response);
            
            String responseStr = new String(response.getData(), 0, response.getLength());
            System.out.println("收到响应: " + responseStr);
            
            socket.close();
            System.out.println("测试完成!");
            
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}