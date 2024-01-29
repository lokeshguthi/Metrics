package de.tukl.softech.exclaim.data;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExamGrade {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<ExamGrade> {

        @Override
        public ExamGrade mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
            return new ExamGrade(rs.getString("exercise"), rs.getString("examid"),
                    rs.getString("grade"), rs.getFloat("min_points"));
        }
    }

    private String exercise;
    private String examId;
    private String grade;
    private float minPoints;

    public ExamGrade(String exercise, String examId, String grade, float minPoints) {
        this.exercise = exercise;
        this.examId = examId;
        this.grade = grade;
        this.minPoints = minPoints;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public float getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(float minPoints) {
        this.minPoints = minPoints;
    }
}
