package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Exercise;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExerciseDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ExerciseDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */
    public Exercise getExercise(String id) {
        try {
            return this.jdbcTemplate.queryForObject("SELECT * FROM exercises\n" +
                    "WHERE id = :id", new MapSqlParameterSource("id", id), new Exercise.RowMapper());
        } catch(IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public List<Exercise> getAllExercises() {
        return this.jdbcTemplate.query("SELECT * FROM exercises ORDER BY id", new Exercise.RowMapper());
    }

    public List<Exercise> getJoinableExercises() {
        return this.jdbcTemplate.query("SELECT * FROM exercises WHERE registration_open = TRUE ORDER BY id", new Exercise.RowMapper());
    }

    /*
        Update
     */

    public int createOrUpdateExercise(Exercise exercise) {
        String query = "MERGE INTO exercises(id, lecture, term)\n" +
                "KEY (id)\n" +
                "VALUES (:id, :lecture, :term)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(exercise));
    }

    public int updateRegistrationStatus(String exercise, boolean registrationOpen) {
        String query = "UPDATE exercises SET registration_open = :registrationOpen\n" +
                "WHERE id = :exercise";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("registrationOpen", String.valueOf(registrationOpen));
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int updateGroupJoin(String exercise, Exercise.GroupJoin groupJoin) {
        String query = "UPDATE exercises SET group_join = :group_join\n" +
                "WHERE id = :exercise";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("group_join", groupJoin.name());
        return this.jdbcTemplate.update(query, namedParams);
    }

    /*
        Delete
     */

    public int deleteExercise(String id) {
        String query = "DELETE FROM exercises\n" +
                "WHERE id = :id";
        return this.jdbcTemplate.update(query, new MapSqlParameterSource("id", id));
    }
}
