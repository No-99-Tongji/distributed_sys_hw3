import java.io.*;
import java.util.List;

public class DatWriter {
    private List<StudentRecord> records;
    private String datFilePath;

    DatWriter(List<StudentRecord> records, String datFilePath) {
        this.records = records;
        this.datFilePath = datFilePath;
    }

    public void write() throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(datFilePath));
        for (StudentRecord record : records) {
            dos.writeInt(record.studentId);
            dos.writeFloat(record.chineseScore);
            dos.writeFloat(record.mathScore);
            dos.writeFloat(record.englishScore);
            dos.writeFloat(record.comprehensiveScore);
        }
        dos.close();
    }
}
