import java.net.*;

public class ManagerTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Manager UDP 测试 ===");
            
            // 测试发送MEMBERSHIP_REQUEST
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            
            String message = "MEMBERSHIP_REQUEST";
            byte[] data = message.getBytes();
            InetAddress managerAddr = InetAddress.getByName("manager");
            
            System.out.println("发送消息: " + message);
            System.out.println("目标地址: " + managerAddr.getHostAddress() + ":9000");
            
            DatagramPacket packet = new DatagramPacket(data, data.length, managerAddr, 9000);
            socket.send(packet);
            System.out.println("✅ 消息已发送");
            
            // 等待响应
            System.out.println("等待响应...");
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            
            try {
                socket.receive(response);
                String responseStr = new String(response.getData(), 0, response.getLength());
                System.out.println("✅ 收到响应: " + responseStr);
                System.out.println("响应来源: " + response.getAddress() + ":" + response.getPort());
            } catch (Exception e) {
                System.out.println("❌ 未收到响应: " + e.getMessage());
            }
            
            socket.close();
            System.out.println("=== 测试完成 ===");
            
        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}