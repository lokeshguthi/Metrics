package de.tukl.softech.exclaim.data;

import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Exam {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Exam> {

        @Override
        public Exam mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
            return new Exam(rs.getString("id"), rs.getString("exercise"), rs.getString("label"),
                    new DateTime(rs.getTimestamp("date")), rs.getString("location"),
                    rs.getBoolean("registration_open"), rs.getBoolean("show_results"));
        }
    }

    private String id;
    private String exercise;
    private String label;
    private DateTime date;
    private String location;

    private boolean registrationOpen;
    private boolean showResults;

    public Exam(String id, String exercise, String label) {
        this.id = id;
        this.exercise = exercise;
        this.label = label;
    }

    public Exam(String id, String exercise, String label, DateTime date, String location, boolean registrationOpen, boolean showResults) {
        this.id = id;
        this.exercise = exercise;
        this.label = label;
        this.date = date;
        this.location = location;
        this.registrationOpen = registrationOpen;
        this.showResults = showResults;
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

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(boolean registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }
}
