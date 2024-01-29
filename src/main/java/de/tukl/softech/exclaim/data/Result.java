package de.tukl.softech.exclaim.data;

import de.tukl.softech.exclaim.utils.TeamConverter;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Result {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Result> {

        @Override
        public Result mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Result(rs.getString("assignment"), rs.getString("sheet"), rs.getString("exercise"),
                    TeamConverter.convertToTeam(rs.getString("team")), rs.getFloat("points"));
        }

    }
    
    private String assignment;
    private String sheet;
    private String exercise;
    private Team team;
    private float points;
    
    public Result(String assignment, String sheet, String exercise, Team team, float points) {
        this.assignment = assignment;
        this.sheet = sheet;
        this.exercise = exercise;
        this.team = team;
        this.points = points;
    }

    public String getAssignment() {

        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public float getPoints() {
        return points;
    }

    public void setPoints(float points) {
        this.points = points;
    }

    public AssignmentAndTeam getAssignmentAndTeam() {
        return new AssignmentAndTeam(assignment, team);
    }
}
