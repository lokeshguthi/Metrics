package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.AssignmentAndTeam;
import de.tukl.softech.exclaim.data.Result;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Testresult;
import de.tukl.softech.exclaim.transferdata.TestStatistics;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TestresultDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public TestresultDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Map<AssignmentAndTeam, Testresult> getLatestTestresults(String exercise, String sheet, Team team) {
        String query = "SELECT *\n" +
                "FROM assignments a\n" +
                "  LEFT JOIN testresult tr\n" +
                "    ON tr.exercise = a.exercise\n" +
                "       AND tr.sheet = a.sheet\n" +
                "       AND tr.assignment = a.id\n" +
                "       AND tr.requestnr =\n" +
                "           (SELECT MAX(requestnr) FROM testresult x\n" +
                "           WHERE x.exercise = a.exercise\n" +
                "                 AND x.sheet = a.sheet\n" +
                "                 AND x.assignment = a.id\n" +
                "                 AND x.team = tr.team)\n" +
                "WHERE a.exercise= :exercise\n" +
                "      AND a.sheet = :sheet\n" +
                "      AND (:team IS NULL OR tr.team = :team)\n" +
                "ORDER BY a.id";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("sheet", sheet);
        namedParameters.put("team", TeamConverter.convertToString(team));
        List<Testresult> testresults = this.jdbcTemplate.query(query, namedParameters, new Testresult.RowMapper());

        Map<AssignmentAndTeam, Testresult> res = new HashMap<>();

        for (Testresult testresult : testresults) {
            res.put(new AssignmentAndTeam(testresult.getAssignment(), testresult.getTeam()), testresult);
        }

        return res;
    }

    public List<Testresult> getLatestTestresultsForTeam(String exercise, String sheet, Team team) {
        String query = "SELECT *\n" +
                "FROM testresult tr\n" +
                "WHERE tr.exercise= :exercise\n" +
                "      AND tr.sheet = :sheet \n" +
                "      AND tr.team = :team   \n" +
                "      AND tr.requestnr =    \n" +
                "           (SELECT MAX(requestnr) FROM testresult x\n" +
                "           WHERE x.exercise = tr.exercise\n" +
                "                 AND x.sheet = tr.sheet\n" +
                "                 AND x.assignment = tr.assignment\n" +
                "                 AND x.team = tr.team)\n";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("sheet", sheet);
        namedParameters.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParameters, new Testresult.RowMapper());
    }

    public Testresult getLatestTestresult(String exercise, String sheet, String assignment, Team team) {
        String query = "SELECT *\n" +
                "FROM assignments a\n" +
                "  LEFT JOIN testresult tr\n" +
                "    ON tr.exercise = a.exercise\n" +
                "       AND tr.sheet = a.sheet\n" +
                "       AND tr.assignment = a.id\n" +
                "       AND tr.requestnr =\n" +
                "           (SELECT MAX(requestnr) FROM testresult x\n" +
                "           WHERE x.exercise = a.exercise\n" +
                "                 AND x.sheet = a.sheet\n" +
                "                 AND x.assignment = a.id\n" +
                "                 AND x.team = tr.team)\n" +
                "WHERE a.exercise= :exercise\n" +
                "      AND a.sheet = :sheet\n" +
                "      AND (:team IS NULL OR tr.team = :team)\n" +
                "      AND a.id = :assignment\n";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("sheet", sheet);
        namedParameters.put("team", TeamConverter.convertToString(team));
        namedParameters.put("assignment", assignment);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParameters, new Testresult.RowMapper());
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public int getNextRequestnr(String exercise, String sheet, String assignment, Team team) {
        String query = "SELECT 1+COALESCE(MAX(requestnr),0) as requestnr FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.queryForObject(query, namedParams, Integer.class);
    }

    public List<Testresult> getTestResults(String exercise, String sheet, Team team) {
        String query = "SELECT exercise, sheet, team, assignment, requestnr, time_request, time_started, time_done, compiled, tests_passed, tests_total, snapshot\n" +
                "FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Testresult.RowMapper());
    }


    public Testresult getTestResultDetails(String exercise, String sheet, String assignment, Team team, int requestnr) {
        String query = "SELECT *\n" +
                "FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND requestnr = :requestnr";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        namedParams.put("requestnr", requestnr);
        return this.jdbcTemplate.queryForObject(query, namedParams, new Testresult.RowMapper());
    }

    public Testresult getTestResultDetails(String exercise, String sheet, String assignment, Team team, DateTime snapshot) {
        String query = "SELECT *\n" +
                "FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND snapshot = :snapshot";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        namedParams.put("snapshot", new Timestamp(snapshot.getMillis()));
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new Testresult.RowMapper());
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public List<Testresult> getAllTestResultDetails(String exercise, String sheet, String assignment, Team team) {
        String query = "SELECT *\n" +
                "FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "ORDER BY requestnr";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Testresult.RowMapper());
    }

    public Testresult getTestResultDetails(Testresult testresult) {
        String query = "SELECT *\n" +
                "FROM testresult\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND requestnr = :requestnr";
        return this.jdbcTemplate.queryForObject(query, new BeanParameterSource(testresult), new Testresult.RowMapper());
    }

    public List<Testresult> getUnfinishedTestRequests() {
        String query = "SELECT *\n" +
                "FROM testresult\n" +
                "WHERE result IS NULL";
        return this.jdbcTemplate.query(query, new Testresult.RowMapper());
    }

    public List<Testresult> getTestresultsForListOfTeams(String exercise, String sheet, List<Team> teams) {
        String query = "SELECT tr.exercise, tr.sheet, tr.team, tr.assignment, tr.tests_passed, tr.tests_total, tr.compiled   \r\n" + 
                "FROM testresult tr   \r\n" + 
                "     INNER JOIN   \r\n" + 
                "        (SELECT exercise, sheet, team, assignment, Max(requestnr) AS maxrn   \r\n" + 
                "         FROM testresult   \r\n" + 
                "         WHERE exercise = :ex AND sheet = :sh AND team = ANY(:tms)   \r\n" + 
                "         GROUP BY exercise, sheet, team, assignment) AS t   \r\n" + 
                "     ON (t.exercise = tr.exercise AND t.sheet = tr.sheet AND t.team = tr.team   \r\n" + 
                "            AND t.assignment = tr.assignment AND t.maxrn = tr.requestnr)";
        String[] teamsArray = teams.stream().map(team -> TeamConverter.convertToString(team)).toArray(String[]::new);
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("ex", exercise);
        namedParams.put("sh", sheet);
        namedParams.put("tms", teamsArray);
        return this.jdbcTemplate.query(query, namedParams, new Testresult.RowMapperShort());
    }

    public List<TestStatistics.TestPassed> getTestsPassedByTeam(String exercise, String sheet, String assignment) {
        String query = "SELECT a.team, a.tests_passed, MIN(a.snapshot) AS snapshot\n" +
                "FROM testresult AS a\n" +
                "NATURAL JOIN (\n" +
                "    SELECT exercise, sheet, assignment, team, MAX(tests_passed) AS tests_passed\n" +
                "    FROM testresult\n" +
                "    GROUP BY exercise, sheet, assignment, team\n" +
                ")\n" +
                "WHERE a.exercise = :exercise AND a.sheet = :sheet AND a.assignment = :assignment\n" +
                "GROUP BY a.team";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        return this.jdbcTemplate.query(query, namedParams, new TestStatistics.TestPassed.RowMapper());
    }

    /*
        Update
     */

    public int createTestresult(Testresult testresult) {
        String query = "INSERT INTO testresult\n" +
                "(exercise, sheet, team, assignment, requestnr, time_request, snapshot)\n" +
                "VALUES (:exercise, :sheet, :team, :assignment, :requestnr, :timeRequest, :snapshot)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(testresult));
    }

    public int storeTestResult(Testresult testresult) {
        String query = "UPDATE testresult\n" +
                "SET time_started = :timeStarted,\n" +
                "    time_done = :timeDone,\n" +
                "    compiled = :compiled,\n" +
                "    internal_error = :internalError,\n" +
                "    tests_passed = :testsPassed,\n" +
                "    tests_total = :testsTotal,\n" +
                "    result = :result,\n" +
                "    missing_files = :missingFiles,\n" +
                "    illegal_files = :illegalFiles,\n" +
                /* Saving and updating the Loc and comments data in our datastorage */
                "    comments_number = :codeCommentsNumber,\n" +
                "    loc_number = :linesOfCodeNumber\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND requestnr = :requestnr";
        return this.jdbcTemplate.update(query, new BeanParameterSource(testresult));
    }

    public int updateTestresultTries(Testresult testresult) {
        String query = "UPDATE testresult\n" +
                "SET retries = retries + 1\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND requestnr = :requestnr";
        return this.jdbcTemplate.update(query, new BeanParameterSource(testresult));
    }

}
