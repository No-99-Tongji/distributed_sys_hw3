import java.net.*;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            System.out.println("测试DNS解析...");
            InetAddress addr = InetAddress.getByName("manager");
            System.out.println("Manager IP: " + addr.getHostAddress());
            
            System.out.println("创建UDP socket...");
            DatagramSocket socket = new DatagramSocket();
            
            System.out.println("发送测试消息...");
            String message = "TEST";
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, 9000);
            socket.send(packet);
            System.out.println("消息已发送到: " + addr + ":9000");
            
            socket.close();
            System.out.println("测试完成");
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}