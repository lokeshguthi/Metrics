package de.tukl.softech.exclaim.data;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import org.springframework.lang.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Admission {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Admission> {

        @Override
        public Admission mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            return new Admission(rs.getString("exercise"), rs.getString("studentid"), rs.getString("message"));
        }
    }

    private String exercise;

    @CsvBindByName
    private String studentId;

    @CsvBindByName
    private String message;

    public Admission(String exercise, String studentId, String message) {
        this.exercise = exercise;
        this.studentId = studentId;
        this.message = message;
    }

    public Admission() {
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Admission{" +
                "exercise='" + exercise + '\'' +
                ", studentId='" + studentId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
