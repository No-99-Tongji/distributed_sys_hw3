import java.net.*;

public class MessageSendTest {
    public static void main(String[] args) {
        try {
            System.out.println("测试各种消息...");
            
            DatagramSocket socket = new DatagramSocket();
            InetAddress managerAddr = InetAddress.getByName("manager");
            
            // 测试不同的消息
            String[] messages = {
                "MEMBERSHIP_REQUEST",
                "JOIN:100:9000", 
                "HEARTBEAT:100:localhost:9000",
                "TEST_MESSAGE"
            };
            
            for (String message : messages) {
                System.out.println("发送消息: " + message);
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, managerAddr, 9000);
                socket.send(packet);
                Thread.sleep(1000); // 等待1秒
            }
            
            socket.close();
            System.out.println("所有消息已发送");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}