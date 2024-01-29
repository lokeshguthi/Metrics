package de.tukl.softech.exclaim.transferdata;

import de.tukl.softech.exclaim.data.Team;
import org.springframework.lang.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentInfo {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<StudentInfo> {

        @Override
        public StudentInfo mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            StudentInfo studentInfo = new StudentInfo();
            studentInfo.id = rs.getString("studentid");
            studentInfo.lastname = rs.getString("lastname");
            studentInfo.firstname = rs.getString("firstname");
            String groupId = rs.getString("groupid");
            String teamId = rs.getString("teamid");
            studentInfo.team = new Team(groupId, teamId);
            studentInfo.email = rs.getString("email");
            return studentInfo;
        }
    }

    private String id;
    private String lastname;
    private String firstname;
    private String email;
    private Team team;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String nameJson() {
        return "\"" + lastname + ", " + firstname  + "\"";
    }

}
