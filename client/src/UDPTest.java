import java.net.*;

public class UDPTest {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        String message = "MEMBERSHIP_REQUEST";
        byte[] data = message.getBytes();
        InetAddress address = InetAddress.getByName("manager");
        DatagramPacket packet = new DatagramPacket(data, data.length, address, 9000);
        socket.send(packet);
        System.out.println("发送请求到: " + address + ":9000");
        
        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(3000);
        try {
            socket.receive(response);
            String responseStr = new String(response.getData(), 0, response.getLength());
            System.out.println("收到响应: " + responseStr);
        } catch (Exception e) {
            System.out.println("超时或错误: " + e.getMessage());
        }
        socket.close();
    }
}