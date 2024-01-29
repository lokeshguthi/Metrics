package de.tukl.softech.exclaim.data;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Exercise {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Exercise> {

        @Override
        public Exercise mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Exercise(rs.getString("id"), rs.getString("lecture"), rs.getString("term"),
                    rs.getBoolean("registration_open"), GroupJoin.valueOf(rs.getString("group_join")));
        }
    }

    private String id;
    private String lecture;
    private String term;

    private boolean registrationOpen = false;
    private GroupJoin groupJoin = GroupJoin.NONE;

    public Exercise(String id, String lecture, String term, boolean registrationOpen, GroupJoin groupJoin) {
        this.id = id;
        this.lecture = lecture;
        this.term = term;
        this.registrationOpen = registrationOpen;
        this.groupJoin = groupJoin;
    }

    public Exercise(String id, String lecture, String term) {
        this.id = id;
        this.lecture = lecture;
        this.term = term;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLecture() {
        return lecture;
    }

    public void setLecture(String lecture) {
        this.lecture = lecture;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public void setRegistrationOpen(boolean registrationOpen) {
        this.registrationOpen = registrationOpen;
    }

    public GroupJoin getGroupJoin() {
        return groupJoin;
    }

    public void setGroupJoin(GroupJoin groupJoin) {
        this.groupJoin = groupJoin;
    }

    public enum GroupJoin {
        NONE("Keine"), GROUP("Gruppe"), PREFERENCES("Präferenz");

        private String displayName;

        GroupJoin(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
