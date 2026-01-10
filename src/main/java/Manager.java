import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Manager {
    public class Membership {
        public int id;
        public String ip;
        public int port;
    }
    private List<Membership> membershipList;
    private NetReceiver receiver;
    private List<NetSender> senders;
    private String listeningPort;

    public Manager(List<Membership> membershipList, String listeningPort) throws Exception {
        this.listeningPort = listeningPort;
        this.membershipList = membershipList;
        this.senders = new ArrayList<>(); // 初始化senders列表
        for (Membership member : membershipList) {
            senders.add(new NetSender(member.ip, member.port));
        }
        receiver = new NetReceiver(Integer.parseInt(listeningPort));
    }

    public void startListening() {
        new Thread(() -> {
            while (true) {
                try {
                    // 接收单个原始数据包
                    byte[] rawPacket = receiver.listenRawPacket();
                    
                    if (rawPacket.length == 0) {
                        continue; // 跳过空数据包
                    }
                    
                    // 随机选择一个下游节点
                    Random random = new Random();
                    int randomNum = random.nextInt(1000);
                    int index = randomNum % senders.size();
                    NetSender sender = senders.get(index);
                    
                    // 转发原始数据包
                    sender.forwardRawPacket(rawPacket);
                    
                    // 备份转发（可选）
                    NetSender backoffSender = senders.get((index + 1) % senders.size());
                    backoffSender.forwardRawPacket(rawPacket);
                    
                    // 解析并打印日志（可选，用于调试）
                    logPacketInfo(rawPacket);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * 记录数据包信息（用于调试）
     */
    private void logPacketInfo(byte[] rawPacket) {
        if (rawPacket.length >= 4) {
            // 解析序列号
            int sequenceNumber = ((rawPacket[0] & 0xFF) << 24) |
                               ((rawPacket[1] & 0xFF) << 16) |
                               ((rawPacket[2] & 0xFF) << 8) |
                               (rawPacket[3] & 0xFF);
            
            // 检查是否为结束标记
            if (rawPacket.length == 8) {
                int marker = ((rawPacket[4] & 0xFF) << 24) |
                           ((rawPacket[5] & 0xFF) << 16) |
                           ((rawPacket[6] & 0xFF) << 8) |
                           (rawPacket[7] & 0xFF);
                if (marker == -1) {
                    System.out.println("转发结束标记，总块数: " + sequenceNumber);
                    return;
                }
            }
            
            System.out.println("转发第 " + sequenceNumber + " 块，大小: " + (rawPacket.length - 4) + " 字节");
        }
    }
}
