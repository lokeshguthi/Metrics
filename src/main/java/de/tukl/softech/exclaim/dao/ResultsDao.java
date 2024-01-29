package de.tukl.softech.exclaim.dao;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.transferdata.APIResult;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class ResultsDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ResultsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Result getResult(String exercise, String sheet, String assignment, Team team) {
        String query = "SELECT * FROM results\n" +
                "WHERE assignment = :assignment AND sheet = :sheet AND exercise = :exercise AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new Result.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<AssignmentAndTeam, Float> getTeamAssignmentResults(String exericse, String sheet, Team team) {
        String query = "SELECT *\n" +
                "FROM assignments a\n" +
                "  LEFT JOIN results r\n" +
                "    ON r.exercise = a.exercise\n" +
                "       AND r.sheet = a.sheet\n" +
                "       AND r.assignment = a.id\n" +
                "WHERE a.exercise= :exercise\n" +
                "      AND a.sheet = :sheet\n" +
                "      AND (:team IS NULL OR r.team = :team)\n" +
                "ORDER BY a.id";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exericse);
        namedParameters.put("sheet", sheet);
        namedParameters.put("team", TeamConverter.convertToString(team));
        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query,
                namedParameters);
        Map<AssignmentAndTeam, Float> res = new HashMap<>();

        while (rowSet.next()) {
            res.put(new AssignmentAndTeam(rowSet.getString("assignment"), team), rowSet.getFloat("points"));
        }

        return res;
    }

    public List<Result> getAllResults(String exercise, String sheet, String assignment) {
        String query = "SELECT * FROM results\n" +
                "WHERE assignment = :assignment AND sheet = :sheet AND exercise = :exercise";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("sheet", sheet);
        namedParameters.put("assignment", assignment);
        return this.jdbcTemplate.query(query, namedParameters, new Result.RowMapper());
    }

    public List<Result> getResultsForSheet(String exericse, String sheet) {
        String query = "SELECT * FROM results\n" +
                "WHERE sheet = :sheet AND exercise = :exercise";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exericse);
        namedParameters.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParameters, new Result.RowMapper());
    }

    public List<Result> getResultsForTeam(String exercise, String sheet, Team team) {
        String query = "SELECT * FROM results\n" +
                "WHERE sheet = :sheet AND exercise = :exercise AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Result.RowMapper());
    }

    public List<Result> getResultsForStudent(String studentid) {
        String query = "SELECT * FROM results, studentres\n" +
                "WHERE studentres.studentid = :studentid AND studentres.sheet = results.sheet AND studentres.exercise = results.exercise AND\n" +
                "    studentres.team = results.team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("studentid", studentid);
        return this.jdbcTemplate.query(query, namedParams, new Result.RowMapper());
    }

    /*
        Returns Map with sheet and students result for this sheet
     */
    public Map<String, Float> getResultsForStudentAndExercise(String studentid, String exercise) {
        String query = "SELECT results.sheet, SUM(points) AS points FROM results INNER JOIN studentres\n" +
                "ON (studentres.sheet = results.sheet AND studentres.exercise = results.exercise AND studentres.team = results.team)\n" +
                "WHERE studentres.studentid = :studentid  AND results.exercise = :exercise GROUP BY results.sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("studentid", studentid);
        namedParams.put("exercise", exercise);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Float> points = new HashMap<>();
        while (rowSet.next()) {
            points.put(rowSet.getString("sheet"), rowSet.getFloat("points"));
        }
        return points;
    }

    public Team getTeamFromResults(String exercise, String sheet, String studentid) {
        String query = "SELECT team FROM studentres\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("studentid", studentid);
        try {
            return TeamConverter.convertToTeam(
                    this.jdbcTemplate.queryForObject(query, namedParams, String.class)
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<String> getMembersOfTeam(String exercise, String sheet, Team team) {
        String query = "SELECT studentid FROM studentres\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, (rs, rowNum) -> rs.getString(1));
    }

    public Map<String, List<APIResult>> getResultsForExercise(String exercise) {
        String query = "SELECT r.sheet as sheet, s.studentid as studentid, SUM(r.points) + COALESCE(d.delta, 0) as points\n" +
                "FROM results r\n" +
                "JOIN studentres s ON s.sheet = r.sheet AND s.exercise = r.exercise AND s.team = r.team\n" +
                "JOIN assignments a ON a.exercise = r.exercise AND a.sheet = r.sheet AND a.id = r.assignment\n" +
                "LEFT JOIN deltapoints d ON d.exercise = r.exercise AND d.sheet = r.sheet AND d.studentid = s.studentid\n" +
                "WHERE r.exercise = :exercise\n" +
                "GROUP BY sheet, studentid\n" +
                "ORDER BY sheet, studentid";
        List<Map<String, Object>> exerciseResults = this.jdbcTemplate.queryForList(query, new MapSqlParameterSource("exercise", exercise));

        Map<String, List<APIResult>> resultsBySheet = new HashMap<>();
        for (Map<String, Object> result : exerciseResults) {
            String sheet = (String) result.get("sheet");
            if (!resultsBySheet.containsKey(sheet))
                resultsBySheet.put(sheet, new LinkedList<>());
            resultsBySheet.get(sheet).add(new APIResult((String) result.get("studentid"), ((BigDecimal) result.get("points")).doubleValue()));
        }

        return resultsBySheet;
    }

    public List<Result> getResultsForListOfTeams(String exercise, String sheet, List<Team> teams) {
        String query = "SELECT exercise, sheet, team, assignment, points   \r\n" +
                "FROM results   \r\n" +
                "WHERE exercise = :ex AND sheet = :sh AND team = ANY (:tms)  \r\n";
        String[] teamsArray = teams.stream()
                .map(team -> TeamConverter.convertToString(team))
                .toArray(String[]::new);
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("ex", exercise);
        namedParams.put("sh", sheet);
        namedParams.put("tms", teamsArray);
        return this.jdbcTemplate.query(query, namedParams, new Result.RowMapper());
    }

    /*
        Update
     */

    public int createOrUpdateResult(Result result) {
        String query = "MERGE INTO results(assignment, sheet, exercise, team, points)\n" +
                "KEY (assignment, sheet, exercise, team)\n" +
                "VALUES (:assignment, :sheet, :exercise, :team, :points)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(result));
    }

    public int createStudentres(Studentres studentres) {
        String query = "INSERT INTO studentres(sheet, exercise, team, studentid)\n" +
                "VALUES (:sheet, :exercise, :team, :studentid)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(studentres));
    }

    /*
        Delete
     */

    public int deleteResult(Result result) {
        String query = "DELETE FROM results\n" +
                "WHERE assignment = :assignment AND sheet = :sheet AND exercise = :exercise AND team = :team";
        return this.jdbcTemplate.update(query, new BeanParameterSource(result));
    }

    public int deleteStudentres(Studentres studentres) {
        String query = "DELETE FROM studentres\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND team = :team AND studentid = :studentid";
        return this.jdbcTemplate.update(query, new BeanParameterSource(studentres));
    }

    public void updateStudentInfos(List<StudentInfo> studentInfos, String exercise, String sheet) {
        for (StudentInfo student : studentInfos) {
            Team teamFromResults = getTeamFromResults(exercise, sheet, student.getId());
            if (teamFromResults != null) {
                student.setTeam(teamFromResults);
            }
        }
    }

    public int updateStudentId(String oldId, String newId) {
        String query = "SELECT COUNT(*) FROM studentres WHERE studentid = :studentid";
        Integer count = jdbcTemplate.queryForObject(query, ImmutableMap.of("studentid", newId), Integer.class);
        if (count > 0) {
            throw new IllegalArgumentException("new student id " + newId + " already has " + count + " entries in studentres");
        }
        String update = "UPDATE studentres SET studentid = :newid WHERE studentid = :oldid";
        return jdbcTemplate.update(update, ImmutableMap.of("oldid", oldId, "newid", newId));
    }
}
