package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Comment;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommentsDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public CommentsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public Comment getComment(String exercise, String sheet, Team team) {
        String query = "SELECT * FROM comments\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new Comment.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Comment> getAllComments(String exercise, String sheet) {
        String query = "SELECT * FROM comments\n" +
                "WHERE exercise = :exercise AND sheet = :sheet ORDER BY team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParams, new Comment.RowMapper());
    }

    public boolean isHiddenComment(String exercise, String sheet, Team team) {
        String query = "SELECT hidden FROM comments\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        if (rowSet.next()) {
            return rowSet.getBoolean("hidden");
        }
        return false;
    }

    /*
        Update
     */

    public int createOrUpdateComment(Comment comment) {
        String query = "MERGE INTO comments(sheet, exercise, team, comment, hidden)\n" +
                "KEY (exercise, sheet, team)\n" +
                "VALUES (:sheet, :exercise, :team, :comment, :hidden)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(comment));
    }

    /*
        Delete
     */

    public int deleteComment(Comment comment) {
        String query = "DELETE FROM comments\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND team = :team";
        return this.jdbcTemplate.update(query, new BeanParameterSource(comment));
    }
}
