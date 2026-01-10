import java.io.*;
import java.util.*;

/**
 * 测试数据生成器
 * 生成学生成绩数据用于测试分布式系统
 */
public class TestDataGenerator {
    
    /**
     * 生成测试数据
     */
    public static void generateTestData(String studentId, int numRecords, String outputPath) throws IOException {
        List<StudentRecord> records = new ArrayList<>();
        Random random = new Random();
        
        System.out.println("生成 " + numRecords + " 条测试数据...");
        
        for (int i = 1; i <= numRecords; i++) {
            // 生成随机成绩 (60-100分)
            float chinese = 60 + random.nextFloat() * 40;
            float math = 60 + random.nextFloat() * 40;
            float english = 60 + random.nextFloat() * 40;
            float comprehensive = 60 + random.nextFloat() * 40;
            
            StudentRecord record = new StudentRecord(
                1000000 + i,  // 学号从1000001开始
                chinese, math, english, comprehensive
            );
            
            records.add(record);
        }
        
        // 写入.dat文件
        writeRecords(records, outputPath);
        
        System.out.println("测试数据生成完成:");
        System.out.println("  二进制文件: " + outputPath + " (" + new File(outputPath).length() + " 字节)");
        System.out.println("  记录数: " + numRecords);
        System.out.println("  每条记录: 20字节 (int + 4*float)");
    }
    
    /**
     * 写入学生记录到文件
     */
    private static void writeRecords(List<StudentRecord> records, String filePath) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(filePath))) {
            for (StudentRecord record : records) {
                dos.writeInt(record.studentId);
                dos.writeFloat(record.chineseScore);
                dos.writeFloat(record.mathScore);
                dos.writeFloat(record.englishScore);
                dos.writeFloat(record.comprehensiveScore);
            }
        }
    }
}