package de.tukl.softech.exclaim.dao;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.data.Deltapoints;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeltapointsDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public DeltapointsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Deltapoints getDeltapointsForStudent(String exercise, String sheet, String studentid) {
        String query = "SELECT * FROM deltapoints\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("studentid", studentid);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new Deltapoints.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<String, Float> getDeltapointsForStudent(String exercise, String studentid) {
        String query = "SELECT sheet, SUM(delta) AS delta FROM deltapoints\n" +
                "WHERE exercise = :exercise AND studentid = :studentid GROUP BY sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentid);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Float> deltapoints = new HashMap<>();
        while(rowSet.next()) {
            deltapoints.put(rowSet.getString("sheet"), rowSet.getFloat("delta"));
        }
        return deltapoints;
    }

    public List<Deltapoints> getAllDeltapoints(String exercise, String sheet) {
        String query = "SELECT * FROM deltapoints\n" +
                "WHERE exercise = :exercise AND sheet = :sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParams, new Deltapoints.RowMapper());
    }

    /*
        Update
     */

    public int createOrUpdateDeltapoints(Deltapoints deltapoints) {
        String query = "MERGE INTO deltapoints\n" +
                "(sheet, exercise, studentid, delta, reason)\n" +
                "KEY (sheet, exercise, studentid)" +
                "VALUES (:sheet, :exercise, :studentid, :delta, :reason)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(deltapoints));
    }

    /*
        Delete
     */

    public int deleteDeltapoints(Deltapoints deltapoints) {
        String query = "DELETE FROM deltapoints\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND studentid = :studentid";
        return this.jdbcTemplate.update(query, new BeanParameterSource(deltapoints));
    }

    public int updateStudentId(String oldId, String newId) {
        String query = "SELECT COUNT(*) FROM deltapoints WHERE studentid = :studentid";
        Integer count = jdbcTemplate.queryForObject(query, ImmutableMap.of("studentid", newId), Integer.class);
        if (count > 0) {
            throw new IllegalArgumentException("new student id " + newId + " already has " + count + " entries in deltapoints");
        }
        String update = "UPDATE deltapoints SET studentid = :newid WHERE studentid = :oldid";
        return jdbcTemplate.update(update, ImmutableMap.of("oldid", oldId, "newid", newId));
    }
}
