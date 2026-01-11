import java.net.*;
import java.io.*;

public class MembershipTest {
    public static void main(String[] args) {
        try {
            System.out.println("开始MEMBERSHIP_REQUEST测试...");
            
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(10000); // 10秒超时
            
            String message = "MEMBERSHIP_REQUEST";
            byte[] data = message.getBytes();
            
            System.out.println("正在解析manager地址...");
            InetAddress managerAddr = InetAddress.getByName("manager");
            System.out.println("Manager地址解析成功: " + managerAddr.getHostAddress());
            
            DatagramPacket packet = new DatagramPacket(data, data.length, managerAddr, 9000);
            
            System.out.println("发送MEMBERSHIP_REQUEST消息: " + message);
            System.out.println("目标: " + managerAddr.getHostAddress() + ":9000");
            socket.send(packet);
            System.out.println("消息已发送，等待响应...");
            
            // 接收响应
            byte[] buffer = new byte[2048];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            
            socket.receive(response);
            
            String responseStr = new String(response.getData(), 0, response.getLength());
            System.out.println("收到响应: " + responseStr);
            System.out.println("响应来源: " + response.getAddress() + ":" + response.getPort());
            
            socket.close();
            System.out.println("测试成功完成!");
            
        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}