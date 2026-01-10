import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 分布式组成员管理服务 - Master/Leader方式
 */
public class MembershipService {
    private int nodeId;
    private int port;
    private boolean isLeader;
    private Set<NodeInfo> membershipList;
    private DatagramSocket socket;
    private Map<Integer, Long> lastHeartbeat;
    private ScheduledExecutorService scheduler;
    private boolean running = true;
    
    private static final long HEARTBEAT_INTERVAL = 5000; // 5秒
    private static final long FAILURE_TIMEOUT = 15000; // 15秒
    
    public static class NodeInfo {
        public int nodeId;
        public String ip;
        public int port;
        public long lastSeen;
        
        public NodeInfo(int nodeId, String ip, int port) {
            this.nodeId = nodeId;
            this.ip = ip;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            NodeInfo nodeInfo = (NodeInfo) obj;
            return nodeId == nodeInfo.nodeId;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(nodeId);
        }
        
        @Override
        public String toString() {
            return "Node{id=" + nodeId + ", ip=" + ip + ", port=" + port + "}";
        }
    }
    
    public MembershipService(int nodeId, int port, boolean isLeader) throws SocketException {
        this.nodeId = nodeId;
        this.port = port;
        this.isLeader = isLeader;
        this.membershipList = ConcurrentHashMap.newKeySet();
        this.lastHeartbeat = new ConcurrentHashMap<>();
        this.socket = new DatagramSocket(port);
        this.scheduler = Executors.newScheduledThreadPool(3);
        
        // 将自己加入成员列表
        NodeInfo selfNode = new NodeInfo(nodeId, "localhost", port);
        membershipList.add(selfNode);
        lastHeartbeat.put(nodeId, System.currentTimeMillis());
    }
    
    /**
     * 启动成员管理服务
     */
    public void start() {
        System.out.println("启动成员管理服务 - 节点 " + nodeId + 
                          (isLeader ? " (Leader)" : " (Member)"));
        
        // 启动消息处理线程
        Thread messageThread = new Thread(this::handleMessages);
        messageThread.start();
        
        // 启动心跳发送
        scheduler.scheduleAtFixedRate(this::sendHeartbeats, 0, 
                                    HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        
        if (isLeader) {
            // Leader启动故障检测
            scheduler.scheduleAtFixedRate(this::detectFailures, 
                                        FAILURE_TIMEOUT, FAILURE_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        
        // 启动用户交互
        handleUserCommands();
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessages() {
        byte[] buffer = new byte[1024];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                processMessage(message, packet.getAddress(), packet.getPort());
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 处理消息
     */
    private void processMessage(String message, InetAddress senderIP, int senderPort) {
        String[] parts = message.split(":");
        if (parts.length < 2) return;
        
        String msgType = parts[0];
        
        switch (msgType) {
            case "JOIN":
                handleJoinRequest(parts, senderIP, senderPort);
                break;
            case "HEARTBEAT":
                handleHeartbeat(parts);
                break;
            case "LEAVE":
                handleLeaveRequest(parts);
                break;
            case "MEMBERSHIP_REQUEST":
                handleMembershipRequest(senderIP, senderPort);
                break;
            case "MEMBERSHIP_RESPONSE":
                handleMembershipResponse(parts);
                break;
        }
    }
    
    /**
     * 处理加入请求
     */
    private void handleJoinRequest(String[] parts, InetAddress senderIP, int senderPort) {
        if (!isLeader) return;
        
        try {
            int newNodeId = Integer.parseInt(parts[1]);
            String ip = senderIP.getHostAddress();
            int port = Integer.parseInt(parts[2]);
            
            NodeInfo newNode = new NodeInfo(newNodeId, ip, port);
            membershipList.add(newNode);
            lastHeartbeat.put(newNodeId, System.currentTimeMillis());
            
            System.out.println("节点 " + newNodeId + " 加入集群");
            
            // 发送当前成员列表给新节点
            sendMembershipList(senderIP, senderPort);
            
            // 通知其他节点
            broadcastMembershipUpdate();
        } catch (NumberFormatException e) {
            System.err.println("无效的加入请求: " + String.join(":", parts));
        }
    }
    
    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(String[] parts) {
        try {
            int senderId = Integer.parseInt(parts[1]);
            lastHeartbeat.put(senderId, System.currentTimeMillis());
            
            // 如果是新节点，加入成员列表
            if (membershipList.stream().noneMatch(node -> node.nodeId == senderId)) {
                String ip = parts.length > 2 ? parts[2] : "localhost";
                int port = parts.length > 3 ? Integer.parseInt(parts[3]) : (8000 + senderId);
                
                NodeInfo newNode = new NodeInfo(senderId, ip, port);
                membershipList.add(newNode);
                System.out.println("通过心跳发现新节点: " + senderId);
            }
        } catch (NumberFormatException e) {
            System.err.println("无效的心跳消息: " + String.join(":", parts));
        }
    }
    
    /**
     * 处理离开请求
     */
    private void handleLeaveRequest(String[] parts) {
        try {
            int leavingNodeId = Integer.parseInt(parts[1]);
            membershipList.removeIf(node -> node.nodeId == leavingNodeId);
            lastHeartbeat.remove(leavingNodeId);
            
            System.out.println("节点 " + leavingNodeId + " 主动离开集群");
            
            if (isLeader) {
                broadcastMembershipUpdate();
            }
        } catch (NumberFormatException e) {
            System.err.println("无效的离开请求: " + String.join(":", parts));
        }
    }
    
    /**
     * 处理成员列表请求
     */
    private void handleMembershipRequest(InetAddress senderIP, int senderPort) {
        if (isLeader) {
            sendMembershipList(senderIP, senderPort);
        }
    }
    
    /**
     * 处理成员列表响应
     */
    private void handleMembershipResponse(String[] parts) {
        // 解析成员列表
        membershipList.clear();
        for (int i = 1; i < parts.length; i += 3) {
            if (i + 2 < parts.length) {
                try {
                    int id = Integer.parseInt(parts[i]);
                    String ip = parts[i + 1];
                    int port = Integer.parseInt(parts[i + 2]);
                    
                    NodeInfo node = new NodeInfo(id, ip, port);
                    membershipList.add(node);
                    lastHeartbeat.put(id, System.currentTimeMillis());
                } catch (NumberFormatException e) {
                    System.err.println("解析成员信息失败: " + parts[i] + "," + parts[i+1] + "," + parts[i+2]);
                }
            }
        }
        
        System.out.println("收到成员列表更新，当前成员数: " + membershipList.size());
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeats() {
        String heartbeat = "HEARTBEAT:" + nodeId + ":localhost:" + port;
        
        for (NodeInfo member : membershipList) {
            if (member.nodeId != nodeId) {
                sendMessage(heartbeat, member.ip, member.port);
            }
        }
    }
    
    /**
     * 检测故障节点
     */
    private void detectFailures() {
        if (!isLeader) return;
        
        long currentTime = System.currentTimeMillis();
        List<Integer> failedNodes = new ArrayList<>();
        
        for (NodeInfo member : membershipList) {
            if (member.nodeId != nodeId) {
                Long lastSeen = lastHeartbeat.get(member.nodeId);
                if (lastSeen == null || (currentTime - lastSeen) > FAILURE_TIMEOUT) {
                    failedNodes.add(member.nodeId);
                }
            }
        }
        
        // 移除故障节点
        for (int failedNodeId : failedNodes) {
            membershipList.removeIf(node -> node.nodeId == failedNodeId);
            lastHeartbeat.remove(failedNodeId);
            System.out.println("检测到节点 " + failedNodeId + " 故障，已从集群中移除");
        }
        
        if (!failedNodes.isEmpty()) {
            broadcastMembershipUpdate();
        }
    }
    
    /**
     * 发送成员列表
     */
    private void sendMembershipList(InetAddress targetIP, int targetPort) {
        StringBuilder sb = new StringBuilder("MEMBERSHIP_RESPONSE");
        
        for (NodeInfo member : membershipList) {
            sb.append(":").append(member.nodeId)
              .append(":").append(member.ip)
              .append(":").append(member.port);
        }
        
        sendMessage(sb.toString(), targetIP.getHostAddress(), targetPort);
    }
    
    /**
     * 广播成员列表更新
     */
    private void broadcastMembershipUpdate() {
        for (NodeInfo member : membershipList) {
            if (member.nodeId != nodeId) {
                sendMembershipList(null, member.port);
            }
        }
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(String message, String ip, int port) {
        try {
            byte[] data = message.getBytes();
            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 加入集群
     */
    public void joinCluster(String leaderIP, int leaderPort) {
        if (isLeader) {
            System.out.println("Leader节点无需加入集群");
            return;
        }
        
        String joinRequest = "JOIN:" + nodeId + ":" + port;
        sendMessage(joinRequest, leaderIP, leaderPort);
        
        // 请求成员列表
        sendMessage("MEMBERSHIP_REQUEST", leaderIP, leaderPort);
    }
    
    /**
     * 离开集群
     */
    public void leaveCluster() {
        String leaveMessage = "LEAVE:" + nodeId;
        
        for (NodeInfo member : membershipList) {
            if (member.nodeId != nodeId) {
                sendMessage(leaveMessage, member.ip, member.port);
            }
        }
        
        System.out.println("已发送离开消息");
    }
    
    /**
     * 显示当前成员列表
     */
    public void showMembership() {
        System.out.println("\\n当前集群成员 (" + membershipList.size() + " 个):");
        for (NodeInfo member : membershipList) {
            Long lastSeen = lastHeartbeat.get(member.nodeId);
            String status = lastSeen != null ? 
                "活跃 (上次心跳: " + (System.currentTimeMillis() - lastSeen) + "ms前)" : "未知";
            
            System.out.println("  " + member + " - " + status);
        }
    }
    
    /**
     * 处理用户命令
     */
    private void handleUserCommands() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\\n成员管理服务命令:");
            System.out.println("join <leader_ip> <leader_port> - 加入集群");
            System.out.println("leave - 离开集群");
            System.out.println("members - 显示成员列表");
            System.out.println("quit - 退出服务");
            
            while (running) {
                System.out.print("\\n[Node-" + nodeId + (isLeader ? "-Leader" : "") + "] > ");
                String input = scanner.nextLine().trim();
                String[] parts = input.split(" ");
                
                switch (parts[0]) {
                    case "join":
                        if (parts.length == 3) {
                            joinCluster(parts[1], Integer.parseInt(parts[2]));
                        } else {
                            System.out.println("用法: join <leader_ip> <leader_port>");
                        }
                        break;
                    case "leave":
                        leaveCluster();
                        break;
                    case "members":
                        showMembership();
                        break;
                    case "quit":
                        shutdown();
                        return;
                    default:
                        System.out.println("未知命令: " + parts[0]);
                }
            }
        }
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        running = false;
        
        if (!isLeader) {
            leaveCluster();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (socket != null) {
            socket.close();
        }
        
        System.out.println("成员管理服务已关闭");
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法: java MembershipService <nodeId> <port> [leader]");
            return;
        }
        
        try {
            int nodeId = Integer.parseInt(args[0]);
            int port = Integer.parseInt(args[1]);
            boolean isLeader = args.length > 2 && "leader".equals(args[2]);
            
            MembershipService service = new MembershipService(nodeId, port, isLeader);
            service.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}