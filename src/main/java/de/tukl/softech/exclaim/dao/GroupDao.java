package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroupDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public GroupDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public List<Group> getGroupsForExercise(String exerciseId) {
        return this.jdbcTemplate.query("SELECT * FROM groups WHERE exerciseid = :exerciseid ORDER BY groupid",
                new MapSqlParameterSource("exerciseid", exerciseId), new Group.RowMapper());
    }

    //Map with key groupid and the number of students in group as value
    public Map<String, Integer> getGroupSizes(String exerciseId) {
        String query = "SELECT groupid, COUNT(*) AS count FROM user_rights\n" +
                "WHERE exerciseid = :exerciseid AND role = 'student' GROUP BY groupid";

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, new MapSqlParameterSource("exerciseid", exerciseId));
        Map<String, Integer> groupSize = new HashMap<>();
        while (rowSet.next()) {
            groupSize.put(rowSet.getString("groupid"), rowSet.getInt("count"));
        }
        return groupSize;
    }

    public Integer getGroupSize(String exerciseId, String groupId) {
        String query = "SELECT COUNT(*) AS count FROM user_rights\n" +
                "WHERE exerciseid = :exerciseid AND groupid = :groupid AND role = 'student'";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exerciseid", exerciseId);
        namedParams.put("groupid", groupId);

        return this.jdbcTemplate.queryForObject(query, namedParams, Integer.class);
    }

    //Map with key groupid and tutors for each group
    public Map<String, List<User>> getGroupTutors(String exerciseId) {
        String query = "SELECT *  FROM groups g INNER JOIN user_rights r ON g.exerciseid = r.exerciseid AND  g.groupid = r.groupid\n" +
                "INNER JOIN users u ON r.userid = u.userid WHERE g.exerciseid = :exerciseid AND role = 'tutor'";

        return this.jdbcTemplate.query(query, new MapSqlParameterSource("exerciseid", exerciseId), rs -> {
            Map<String, List<User>> tutors = new HashMap<>();
            while (rs.next()) {
                String groupid = rs.getString("groupid");
                User tutor = new User.RowMapper().mapRow(rs, rs.getRow());
                if (tutors.containsKey(groupid)) {
                    tutors.get(groupid).add(tutor);
                } else {
                    List<User> groupTutors = new ArrayList<>();
                    groupTutors.add(tutor);
                    tutors.put(groupid, groupTutors);
                }
            }
            return tutors;
        });
    }

    public Group getGroup(String exerciseId, String groupId) {
        String sql = "SELECT * FROM groups WHERE exerciseid = :exerciseid AND groupid = :groupid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exerciseid", exerciseId);
        namedParams.put("groupid", groupId);
        try {
            return this.jdbcTemplate.queryForObject(sql, namedParams, new Group.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public GroupPreferences getGroupPreferences(String exerciseId, int userId) {
        String sql = "SELECT * FROM preferences WHERE exerciseid = :exerciseid AND userid = :userid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exerciseid", exerciseId);
        namedParams.put("userid", String.valueOf(userId));
        try {
            return this.jdbcTemplate.queryForObject(sql, namedParams, new GroupPreferences.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<GroupPreferences> getGroupPreferencesForExercise(String exerciseId) {
        String sql = "SELECT * FROM preferences WHERE exerciseid = :exerciseid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exerciseid", exerciseId);
        return this.jdbcTemplate.query(sql, namedParams, new GroupPreferences.RowMapper());
    }

    /*
        Update
     */

    public int createOrUpdateGroup(Group group) {
        String query = "MERGE INTO groups(exerciseid, groupid, day, time, location, max_size)\n" +
                "KEY (exerciseId, groupId)\n" +
                "VALUES (:exerciseId, :groupId, :day, :time, :location, :maxSize)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(group));
    }

    public int mergeGroupPreferences(GroupPreferences preferences) {
        String query = "MERGE INTO preferences(exerciseid, userid, preferred, possible, dislike, impossible, friends)\n" +
                "KEY (exerciseid, userid)\n" +
                "VALUES (:exerciseid, :userid, :preferred, :possible, :dislike, :impossible, :friends)";
        return this.jdbcTemplate.update(query, preferences.getParameterSource());
    }



    /*
        Delete
     */

    public int deleteGroup(String exercise, String group) {
        String query = "DELETE FROM groups\n" +
                "WHERE exerciseid = :exerciseid AND groupid = :groupid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exerciseid", exercise);
        namedParams.put("groupid", group);
        return this.jdbcTemplate.update(query, namedParams);
    }


}
