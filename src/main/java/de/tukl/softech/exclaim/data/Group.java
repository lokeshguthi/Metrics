package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class Group {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Group> {

        @Override
        public Group mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Group(rs.getString("exerciseId"), rs.getString("groupId"), rs.getString("day"),
                    rs.getString("time"), rs.getString("location"), rs.getInt("max_size"));
        }
    }

    private String exerciseId;
    private String groupId;
    private String day;
    private String time;
    private String location;
    private int maxSize;

    private int size;
    private List<String> tutors;

    public Group(String exerciseId, String groupId, String day, String time, String location, int maxSize) {
        this.exerciseId = exerciseId;
        this.groupId = groupId;
        this.day = day;
        this.time = time;
        this.location = location;
        this.maxSize = maxSize;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getTutors() {
        return tutors;
    }

    public void setTutors(List<String> tutors) {
        this.tutors = tutors;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
