package de.tukl.softech.exclaim.data;

import de.tukl.softech.exclaim.utils.TeamConverter;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Comment {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Comment> {

        @Override
        public Comment mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Comment(rs.getString("sheet"), rs.getString("exercise"),
                    TeamConverter.convertToTeam(rs.getString("team")), rs.getString("comment"),
                    rs.getBoolean("hidden"));
        }
    }

    private String sheet;
    private String exercise;
    private Team team;
    private String comment;
    private boolean hidden;

    public Comment(String sheet, String exercise, Team team, String comment, boolean hidden) {
        this.sheet = sheet;
        this.exercise = exercise;
        this.team = team;
        this.comment = comment;
        this.hidden = hidden;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
