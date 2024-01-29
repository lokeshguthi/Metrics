package de.tukl.softech.exclaim.data;

import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ExamTask {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<ExamTask> {

        @Override
        public ExamTask mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
            return new ExamTask(rs.getString("exercise"), rs.getString("examid"),
                    rs.getString("id"), rs.getFloat("max_points"));
        }
    }

    private String exercise;
    private String examId;
    private String id;
    private float maxPoints;

    public ExamTask(String exercise, String examId, String id, float maxPoints) {
        this.exercise = exercise;
        this.examId = examId;
        this.id = id;
        this.maxPoints = maxPoints;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public float getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(float maxPoints) {
        this.maxPoints = maxPoints;
    }
}
