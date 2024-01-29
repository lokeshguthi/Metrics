package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Assignment {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Assignment> {
        @Override
        public Assignment mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Assignment(rs.getString("id"), rs.getString("exercise"), rs.getString("sheet"),
                    rs.getString("label"), rs.getFloat("maxpoints"), rs.getBoolean("showstatistics"));
        }
    }

    private String id;
    private String exercise;
    private String sheet;
    private String label;

    private float maxpoints;
    private boolean statistics;

    public Assignment(String id, String exercise, String sheet, String label, float maxpoints, boolean statistics) {
        this.id = id;
        this.exercise = exercise;
        this.sheet = sheet;
        this.label = label;
        this.maxpoints = maxpoints;
        this.statistics = statistics;
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

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getMaxpoints() {
        return maxpoints;
    }

    public void setMaxpoints(float maxpoints) {
        this.maxpoints = maxpoints;
    }

    public boolean getStatistics() {
        return statistics;
    }

    public void setStatistics(boolean statistics) {
        this.statistics = statistics;
    }
}
