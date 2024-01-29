package de.tukl.softech.exclaim.transferdata;

import org.springframework.lang.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExerciseRights {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<ExerciseRights> {

        @Override
        public ExerciseRights mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            ExerciseRights exerciseRights = new ExerciseRights();
            exerciseRights.exerciseId = rs.getString("exerciseid");
            exerciseRights.groupId = rs.getString("groupid");
            exerciseRights.teamId = rs.getString("teamid");
            exerciseRights.role = Role.valueOf(rs.getString("role"));
            return exerciseRights;
        }
    }

    public enum Role {
        student, tutor, assistant
    }

    private Role role;
    private String exerciseId;
    private String groupId;
    private String teamId;

    private int userId;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
