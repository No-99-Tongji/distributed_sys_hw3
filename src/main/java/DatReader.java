import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DatReader {
    
    private String filePath;
    
    public DatReader(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * 获取文件路径
     */
    public String getFilePath() {
        return this.filePath;
    }
    
    public List<StudentRecord> read() throws IOException {
        List<StudentRecord> records = new ArrayList<>();
        DataInputStream dis = new DataInputStream(new FileInputStream(filePath));
        try {
            while (true) {
                int studentId = dis.readInt();
                float chineseScore = dis.readFloat();
                float mathScore = dis.readFloat();
                float englishScore = dis.readFloat();
                float comprehensiveScore = dis.readFloat();
                records.add(new StudentRecord(studentId, chineseScore, mathScore, englishScore, comprehensiveScore));
            }
        } catch (EOFException e) {
            // End of file reached
        } finally {
            dis.close();
        }
        return records;
    }
}
