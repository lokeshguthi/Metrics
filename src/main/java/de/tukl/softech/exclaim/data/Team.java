package de.tukl.softech.exclaim.data;

import de.tukl.softech.exclaim.utils.NaturalOrderComparator;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.springframework.lang.NonNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

public class Team implements Comparable<Team> {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Team> {

        @Override
        public Team mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            String groupId = rs.getString("groupid");
            String teamId = rs.getString("teamid");
            return new Team(groupId, teamId);
        }
    }

    public final static Comparator<Team> comparator =
            Comparator.comparing(Team::getGroup, Comparator.nullsFirst(NaturalOrderComparator.instance))
                    .thenComparing(Team::getTeam, Comparator.nullsFirst(NaturalOrderComparator.instance));
    private String group;
    private String team;

    public Team(String group, String team) {
        this.group = group;
        this.team = team;
    }

    /*
    Needed in order to be able to deserialize JSON responses from STATS
     */
    public Team() {
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Team team1 = (Team) o;

        if (group != null ? !group.equals(team1.group) : team1.group != null) return false;
        return team != null ? team.equals(team1.team) : team1.team == null;
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (team != null ? team.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Gruppe " + group + ", Team " + team;
    }

    @Override
    public int compareTo(Team team) {
        return comparator.compare(this, team);
    }
}
