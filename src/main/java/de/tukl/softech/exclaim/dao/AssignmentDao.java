package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Assignment;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AssignmentDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public AssignmentDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Assignment getAssignment(String exercise, String sheet, String id) {
        String query = "SELECT * FROM assignments\n" +
                "WHERE id = :id AND exercise = :exercise AND sheet = :sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("id", id);
        return this.jdbcTemplate.queryForObject(query, namedParams, new Assignment.RowMapper());
    }

    public List<Assignment> getAllAssignments(String exid, String sheet) {
        String query = "SELECT * FROM assignments WHERE exercise= :exercise AND sheet = :sheet ORDER BY id";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exid);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query,
                namedParams, new Assignment.RowMapper());
    }

    /*
     * Returns map with sheetid and maxpoints for this sheet
     */
    public Map<String, Float> getMaxPoints(String exercise) {
        String query = "SELECT sheet, SUM(maxpoints) AS maxpoints FROM assignments WHERE exercise= :exercise GROUP BY sheet;";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Float> maxPoints = new HashMap<>();
        while(rowSet.next()) {
            maxPoints.put(rowSet.getString("sheet"), rowSet.getFloat("maxpoints"));
        }
        return maxPoints;
    }

    /*
        Update
     */

    public int createOrUpdateAssignment(Assignment assignment) {
        String query = "MERGE INTO assignments(id, exercise, sheet, label, maxpoints, showstatistics)\n" +
                "KEY (id, exercise, sheet)\n" +
                "VALUES (:id, :exercise, :sheet, :label, :maxpoints, :statistics)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(assignment));
    }

    /*
        Delete
     */

    public int deleteAssignment(String exercise, String sheet, String id) {
        String query = "DELETE FROM assignments\n" +
                "WHERE id = :id AND exercise = :exercise AND sheet = :sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("id", id);
        return this.jdbcTemplate.update(query, namedParams);
    }
}
