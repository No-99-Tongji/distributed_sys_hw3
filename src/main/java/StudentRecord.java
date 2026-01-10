public class StudentRecord {
    public int studentId;
    public float chineseScore;
    public float mathScore;
    public float englishScore;
    public float comprehensiveScore;
    
    public StudentRecord(int studentId, float chineseScore, float mathScore, 
                        float englishScore, float comprehensiveScore) {
        this.studentId = studentId;
        this.chineseScore = chineseScore;
        this.mathScore = mathScore;
        this.englishScore = englishScore;
        this.comprehensiveScore = comprehensiveScore;
    }
    
    @Override
    public String toString() {
        return String.format("学号: %d, 语文: %.1f, 数学: %.1f, 英语: %.1f, 综合: %.1f",
                studentId, chineseScore, mathScore, englishScore, comprehensiveScore);
    }
}