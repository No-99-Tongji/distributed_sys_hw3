import java.util.List;

public class Server {
    private int port;
    private NetReceiver receiver;
    public Server(int listeningPort) {
        this.port = listeningPort;
        this.receiver = new NetReceiver(port);
    }
    public void start() {
        System.out.println("服务器正在端口 " + port + " 上监听...");
        while (true) {
            try {
                List<StudentRecord> records = receiver.listen();
                for (StudentRecord record : records) {
                    System.out.println(record);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
