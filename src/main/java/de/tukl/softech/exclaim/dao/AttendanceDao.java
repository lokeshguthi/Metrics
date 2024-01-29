package de.tukl.softech.exclaim.dao;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.data.Attendance;
import de.tukl.softech.exclaim.data.AttendanceOverview;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AttendanceDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public AttendanceDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public List<Attendance> getAttendanceForStudent(String exercise, String sheet, String studentid) {
        String query = "SELECT * FROM attendance\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("studentid", studentid);
        return this.jdbcTemplate.query(query, namedParams, new Attendance.RowMapper());
    }

    public List<Attendance> getAttendanceForStudentAndExercise(String exercise, String studentid) {
        String query = "SELECT * FROM attendance\n" +
                "WHERE exercise = :exercise AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentid);
        return this.jdbcTemplate.query(query, namedParams, new Attendance.RowMapper());
    }

    public List<Attendance> getAttendanceForSession(String exercise, String sheet) {
        String query = "SELECT * FROM attendance\n" +
                "WHERE exercise = :exercise AND sheet = :sheet\n" +
                "ORDER BY studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParams, new Attendance.RowMapper());
    }

    public AttendanceOverview getAttendanceOverview(String exercise, String studentid) {
        String query = "SELECT COUNT(NULLIF(attended, TRUE)) AS misses,\n" +
                "       COUNT(attended) AS total\n" +
                "FROM attendance\n" +
                "WHERE exercise = :exercise AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentid);
        return this.jdbcTemplate.queryForObject(query, namedParams, new BeanPropertyRowMapper<>(AttendanceOverview.class));
    }

    public AttendanceOverview getAdminAttendanceOverview(String exercise) {
        String query = "SELECT COUNT(NULLIF(attended, TRUE)) AS misses,\n" +
                "       COUNT(attended) AS total\n" +
                "FROM attendance\n" +
                "WHERE exercise = :exercise\n" +
                "GROUP BY studentid";
        return this.jdbcTemplate.queryForObject(query, new MapSqlParameterSource("exercise", exercise), new BeanPropertyRowMapper<>(AttendanceOverview.class));
    }

    /*
        Update
     */

    public int createOrUpdateAttendance(Attendance attendance) {
        String query = "MERGE INTO attendance(exercise, sheet, studentid, attended)\n" +
                "KEY (exercise, sheet, studentid)\n" +
                "VALUES (:exercise, :sheet, :studentid, :attended)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(attendance));
    }

    public int updateStudentId(String oldId, String newId) {
        String query = "SELECT COUNT(*) FROM attendance WHERE studentid = :studentid";
        Integer count = jdbcTemplate.queryForObject(query, ImmutableMap.of("studentid", newId), Integer.class);
        if (count > 0) {
            throw new IllegalArgumentException("new student id " + newId + " already has " + count + " entries in attendance");
        }
        String update = "UPDATE attendance SET studentid = :newid WHERE studentid = :oldid";
        return jdbcTemplate.update(update, ImmutableMap.of("oldid", oldId, "newid", newId));
    }
}
