package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Sheet;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SheetDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public SheetDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Sheet getSheet(String exercise, String id) {
        String query = "SELECT * FROM sheets\n" +
                "WHERE id = :id AND exercise = :exercise";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("id", id);
        return this.jdbcTemplate.queryForObject(query,namedParams, new Sheet.RowMapper());
    }

    public List<Sheet> getSheetsForExercise(String exerciseId) {
        return this.jdbcTemplate.query("SELECT * FROM sheets WHERE exercise = :exercise ORDER BY id",
                new MapSqlParameterSource("exercise", exerciseId), new Sheet.RowMapper());
    }

    public Map<String, Double> getMaxPointsForExercise(String exercise) {
        String query = "SELECT sheet, SUM(maxpoints) as maxpoints\n" +
                "FROM assignments\n" +
                "WHERE exercise = :exercise\n" +
                "GROUP BY sheet\n" +
                "ORDER BY sheet";
        List<Map<String, Object>> sheetMaxpoints = this.jdbcTemplate.queryForList(query, new MapSqlParameterSource("exercise", exercise));
        return sheetMaxpoints.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("sheet"),
                        m -> ((BigDecimal) m.get("maxpoints")).doubleValue()
                ));
    }

    /*
        Update
     */

    public int createOrUpdateSheet(Sheet sheet) {
        String query = "MERGE INTO sheets(id, exercise, label)\n" +
                "KEY (id, exercise)\n" +
                "VALUES (:id, :exercise, :label)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(sheet));
    }

    /*
        Delete
     */

    public int deleteSheet(String exercise, String id) {
        String query = "DELETE FROM sheets\n" +
                "WHERE id = :id AND exercise = :exercise";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("id", id);
        namedParams.put("exercise", exercise);
        return this.jdbcTemplate.update(query, namedParams);
    }
}
