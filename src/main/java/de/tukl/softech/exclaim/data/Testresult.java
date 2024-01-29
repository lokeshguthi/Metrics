package de.tukl.softech.exclaim.data;

import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Testresult {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Testresult> {

        @Override
        public Testresult mapRow(ResultSet rs, int rowNum) throws SQLException {
            Testresult res = new Testresult();

            res.setExercise(rs.getString("exercise"));
            res.setSheet(rs.getString("sheet"));
            res.setTeam(TeamConverter.convertToTeam(rs.getString("team")));
            res.setAssignment(rs.getString("assignment"));
            res.setRequestnr(rs.getInt("requestnr"));
            res.setRetries(rs.getInt("retries"));

            Timestamp time_request = rs.getTimestamp("time_request");
            res.setTimeRequest(time_request == null ? null : new DateTime(time_request.getTime()));

            Timestamp snapshot = rs.getTimestamp("snapshot");
            res.setSnapshot(snapshot == null ? null : new DateTime(snapshot.getTime()));

            Timestamp time_started = rs.getTimestamp("time_started");
            res.setTimeStarted(time_started == null ? null : new DateTime(time_started.getTime()));

            Timestamp time_done = rs.getTimestamp("time_done");
            res.setTimeDone(time_done == null ? null : new DateTime(time_done.getTime()));

            res.setCompiled(rs.getBoolean("compiled"));
            res.setInternalError(rs.getBoolean("internal_error"));
            res.setTestsPassed(rs.getInt("tests_passed"));
            res.setTestsTotal(rs.getInt("tests_total"));
            res.setMissingFiles(rs.getBoolean("missing_files"));
            res.setIllegalFiles(rs.getBoolean("illegal_files"));
            /* Maps a database record to the java model object */
            res.setCodeCommentsNumber(rs.getInt("comments_number"));
            res.setLinesOfCodeNumber(rs.getInt("loc_number"));

            Clob result = rs.getClob("result");
            if (result != null) {
                res.setResult(result.getSubString(1, Math.toIntExact(result.length())));
            }

            return res;
        }
    }

    public static class RowMapperShort implements org.springframework.jdbc.core.RowMapper<Testresult> {

        @Override
        public Testresult mapRow(ResultSet rs, int rowNum) throws SQLException {
            Testresult res = new Testresult();

            res.setExercise(rs.getString("exercise"));
            res.setSheet(rs.getString("sheet"));
            res.setTeam(TeamConverter.convertToTeam(rs.getString("team")));
            res.setAssignment(rs.getString("assignment"));
            res.setCompiled(rs.getBoolean("compiled"));
            res.setTestsPassed(rs.getInt("tests_passed"));
            res.setTestsTotal(rs.getInt("tests_total"));

            return res;
        }
    }
    
    private String exercise;
    private String sheet;
    private Team team;
    private String assignment;
    private int requestnr;
    private int retries = 0;
    private DateTime timeRequest;
    private DateTime snapshot;
    private DateTime timeStarted;
    private DateTime timeDone;
    private boolean compiled;
    private boolean internalError;
    private int testsPassed;
    private int testsTotal;
    private String result;
    private boolean missingFiles;
    private boolean illegalFiles;
    private int codeCommentsNumber;
    private int linesOfCodeNumber;

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getAssignment() {

        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
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

    public int getRequestnr() {
        return requestnr;
    }

    public void setRequestnr(int requestnr) {
        this.requestnr = requestnr;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public DateTime getTimeRequest() {
        return timeRequest;
    }

    public void setTimeRequest(DateTime timeRequest) {
        this.timeRequest = timeRequest;
    }

    public DateTime getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(DateTime snapshot) {
        this.snapshot = snapshot;
    }

    public DateTime getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(DateTime timeStarted) {
        this.timeStarted = timeStarted;
    }

    public DateTime getTimeDone() {
        return timeDone;
    }

    public void setTimeDone(DateTime timeDone) {
        this.timeDone = timeDone;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    public boolean isInternalError() {
        return internalError;
    }

    public void setInternalError(boolean internalError) {
        this.internalError = internalError;
    }

    public int getTestsPassed() {
        return testsPassed;
    }

    public void setTestsPassed(int testsPassed) {
        this.testsPassed = testsPassed;
    }

    public int getTestsTotal() {
        return testsTotal;
    }

    public void setTestsTotal(int testsTotal) {
        this.testsTotal = testsTotal;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isMissingFiles() {
        return missingFiles;
    }

    public void setMissingFiles(boolean missingFiles) {
        this.missingFiles = missingFiles;
    }

    public boolean isIllegalFiles() {
        return illegalFiles;
    }

    public void setIllegalFiles(boolean illegalFiles) {
        this.illegalFiles = illegalFiles;
    }

    public int getCodeCommentsNumber() {
        return codeCommentsNumber;
    }

    public void setCodeCommentsNumber(int codeCommentsNumber) {
        this.codeCommentsNumber = codeCommentsNumber;
    }

    public int getLinesOfCodeNumber() {
        return linesOfCodeNumber;
    }

    public void setLinesOfCodeNumber(int linesOfCodeNumber) {
        this.linesOfCodeNumber = linesOfCodeNumber;
    }

    /**
     * Returns a link to the test-result
     */
    public String linkTo() {
        return "/exercise/" + exercise
                + "/sheet/" + sheet
                + "/" + assignment
                + "/team/" + team.getGroup() + "/" + team.getTeam()
                + "/test/" + requestnr;
    }
}
