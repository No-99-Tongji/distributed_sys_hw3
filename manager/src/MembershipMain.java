/**
 * 成员管理服务启动类
 */
public class MembershipMain {
    
    public static void main(String[] args) {
        try {
            runMembershipService(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 运行成员管理服务
     */
    private static void runMembershipService(String[] args) throws Exception {
        int nodeId = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9001;
        boolean isLeader = args.length > 2 && "leader".equals(args[2]);
        
        System.out.println("启动成员管理服务:");
        System.out.println("  节点ID: " + nodeId);
        System.out.println("  端口: " + port);
        System.out.println("  角色: " + (isLeader ? "Leader" : "Member"));
        
        MembershipService service = new MembershipService(nodeId, port, isLeader);
        
        // 如果不是Leader，尝试加入集群
        if (!isLeader) {
            String leaderHost = System.getenv("LEADER_HOST");
            String leaderPortStr = System.getenv("LEADER_PORT");
            
            if (leaderHost != null && leaderPortStr != null) {
                int leaderPort = Integer.parseInt(leaderPortStr);
                System.out.println("尝试加入Leader: " + leaderHost + ":" + leaderPort);
                service.joinCluster(leaderHost, leaderPort);
            }
        }
        
        service.start();
    }
}