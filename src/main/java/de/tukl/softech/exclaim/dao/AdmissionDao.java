package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Admission;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdmissionDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public AdmissionDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public String getAdmissionMessage(String exercise, String studentid) {
        String query = "SELECT message FROM admissions\n" +
                "WHERE exercise = :exercise AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentid);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

    }

    public List<Admission> getAdmissions(String exercise) {
        String query = "SELECT * FROM admissions\n" +
                "WHERE exercise = :exercise";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        return this.jdbcTemplate.query(query, namedParams, new Admission.RowMapper());
    }



    /*
        Update
     */
    public int[] createOrUpdateAdmissions(List<Admission> admissions) {
        String sql = "MERGE INTO admissions(exercise, studentid, message)\n" +
                "KEY (exercise, studentid)\n" +
                "VALUES (?, ?, ?)";
        return this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Admission admission = admissions.get(i);
                ps.setString(1, admission.getExercise());
                ps.setString(2, admission.getStudentId());
                ps.setString(3, admission.getMessage());
            }

            @Override
            public int getBatchSize() {
                return admissions.size();
            }
        });
    }


    /*
        Delete
     */
    public int deleteAdmission(Admission admission) {
        String query = "DELETE FROM admissions\n" +
                "WHERE   exercise = :exercise\n" +
                "    AND studentid = :studentId";
        return this.jdbcTemplate.update(query, new BeanParameterSource(admission));
    }

}
