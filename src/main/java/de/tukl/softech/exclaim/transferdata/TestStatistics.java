package de.tukl.softech.exclaim.transferdata;

import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

public class TestStatistics {

    private int testsPassed;
    private int teamPlace;
    Map<Integer, Long> passedTests;

    public TestStatistics() {
    }

    public TestStatistics(int testsPassed, int teamPlace, Map<Integer, Long> passedTests) {
        this.testsPassed = testsPassed;
        this.teamPlace = teamPlace;
        this.passedTests = passedTests;
    }

    public int getTestsPassed() {
        return testsPassed;
    }

    public void setTestsPassed(int testsPassed) {
        this.testsPassed = testsPassed;
    }

    public int getTeamPlace() {
        return teamPlace;
    }

    public void setTeamPlace(int teamPlace) {
        this.teamPlace = teamPlace;
    }

    public Map<Integer, Long> getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(Map<Integer, Long> passedTests) {
        this.passedTests = passedTests;
    }

    public static class TestPassed {

        private Team team;
        private int passed;
        private DateTime snapshot;

        public static class RowMapper implements org.springframework.jdbc.core.RowMapper<TestPassed> {

            @Override
            public TestPassed mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
                TestPassed testPassed = new TestPassed();

                testPassed.setTeam(TeamConverter.convertToTeam(rs.getString("team")));
                Timestamp snapshot = rs.getTimestamp("snapshot");
                testPassed.setSnapshot(snapshot == null ? null : new DateTime(snapshot.getTime()));

                testPassed.setPassed(rs.getInt("tests_passed"));

                return testPassed;
            }
        }

        public TestPassed() {
        }

        public Team getTeam() {
            return team;
        }

        public void setTeam(Team team) {
            this.team = team;
        }

        public int getPassed() {
            return passed;
        }

        public void setPassed(int passed) {
            this.passed = passed;
        }

        public DateTime getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(DateTime snapshot) {
            this.snapshot = snapshot;
        }
    }


}
