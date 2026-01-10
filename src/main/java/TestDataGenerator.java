import java.io.*;
import java.util.*;

/**
 * 测试数据生成器
 * 生成学生成绩数据用于测试分布式系统
 */
public class TestDataGenerator {
    
    public static void main(String[] args) {
        try {
            String studentId = args.length > 0 ? args[0] : "2353250";
            int numRecords = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
            
            generateTestData(studentId, numRecords);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 生成测试数据
     */
    public static void generateTestData(String studentId, int numRecords) throws IOException {
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
        String datFileName = "src/main/resources/" + studentId + "-hw2.dat";
        DatWriter writer = new DatWriter(records, datFileName);
        writer.write();
        
        // 写入.txt文件（可读格式）
        String txtFileName = "src/main/resources/" + studentId + "-hw2.txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(txtFileName))) {
            pw.println("学号,语文,数学,英语,综合");
            for (StudentRecord record : records) {
                pw.printf("%d,%.1f,%.1f,%.1f,%.1f%n",
                    record.studentId, record.chineseScore, record.mathScore,
                    record.englishScore, record.comprehensiveScore);
            }
        }
        
        System.out.println("测试数据生成完成:");
        System.out.println("  二进制文件: " + datFileName + " (" + new File(datFileName).length() + " 字节)");
        System.out.println("  文本文件: " + txtFileName);
        System.out.println("  记录数: " + numRecords);
        System.out.println("  每条记录: 20字节 (int + 4*float)");
    }
    
    /**
     * 验证生成的数据
     */
    public static void verifyData(String datFileName) {
        try {
            DatReader reader = new DatReader(datFileName);
            List<StudentRecord> records = reader.read();
            
            System.out.println("数据验证结果:");
            System.out.println("  读取记录数: " + records.size());
            
            if (!records.isEmpty()) {
                System.out.println("  第一条记录: " + records.get(0));
                System.out.println("  最后一条记录: " + records.get(records.size() - 1));
            }
            
            // 验证文件大小
            File file = new File(datFileName);
            long expectedSize = records.size() * 20L; // 每条记录20字节
            System.out.println("  文件大小: " + file.length() + " 字节");
            System.out.println("  期望大小: " + expectedSize + " 字节");
            System.out.println("  大小匹配: " + (file.length() == expectedSize));
            
        } catch (Exception e) {
            System.err.println("验证数据失败: " + e.getMessage());
        }
    }
}