package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Deltapoints {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Deltapoints> {

        @Override
        public Deltapoints mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Deltapoints(rs.getString("sheet"), rs.getString("exercise"), rs.getString("studentid"),
                    rs.getFloat("delta"), rs.getString("reason"));
        }
    }

    private String sheet;
    private String exercise;
    private String studentid;
    private float delta;
    private String reason;

    public Deltapoints(String sheet, String exercise, String studentid, float delta, String reason) {
        this.sheet = sheet;
        this.exercise = exercise;
        this.studentid = studentid;
        this.delta = delta;
        this.reason = reason;
    }

    public String getSheet() {

        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getStudentid() {
        return studentid;
    }

    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    public float getDelta() {
        return delta;
    }

    public void setDelta(float delta) {
        this.delta = delta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
