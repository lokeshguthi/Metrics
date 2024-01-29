package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Attendance {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Attendance> {

        @Override
        public Attendance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Attendance(rs.getString("sheet"), rs.getString("exercise"), rs.getString("studentid"),
                    rs.getBoolean("attended"));
        }
    }

    private String sheet;
    private String exercise;
    private String studentid;
    private boolean attended;

    public Attendance(String sheet, String exercise, String studentid, boolean attended) {
        this.sheet = sheet;
        this.exercise = exercise;
        this.studentid = studentid;
        this.attended = attended;
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

    public boolean isAttended() {
        return attended;
    }

    public void setAttended(boolean attended) {
        this.attended = attended;
    }
}
