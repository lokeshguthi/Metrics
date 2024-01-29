package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Sheet {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Sheet> {

        @Override
        public Sheet mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Sheet(rs.getString("id"), rs.getString("exercise"), rs.getString("label"));
        }
    }

    private String id;
    private String exercise;
    private String label;

    public Sheet(String id, String exercise, String label) {
        this.id = id;
        this.exercise = exercise;
        this.label = label;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
