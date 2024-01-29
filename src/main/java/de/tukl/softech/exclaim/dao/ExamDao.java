package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("examDao")
public class ExamDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public ExamDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public List<Exam> getExamsForExercise(String exerciseId) {
        return this.jdbcTemplate.query("SELECT * FROM exams WHERE exercise = :exercise ORDER BY id",
                new MapSqlParameterSource("exercise", exerciseId), new Exam.RowMapper());
    }

    public Exam getExam(String exercise, String exam) {
        String sql = "SELECT * FROM exams WHERE exercise = :exercise AND id = :exam";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        try {
            return this.jdbcTemplate.queryForObject(sql, namedParams, new Exam.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<ExamTask> getExamTasks(String exercise, String exam) {
        String query = "SELECT * FROM examtasks WHERE exercise = :exercise AND examid = :exam ORDER BY id";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        return this.jdbcTemplate.query(query, namedParams, new ExamTask.RowMapper());
    }

    public List<ExamGrade> getExamGrades(String exercise, String exam) {
        String query = "SELECT * FROM examgrades WHERE exercise = :exercise AND examid = :exam ORDER BY min_points DESC";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        return this.jdbcTemplate.query(query, namedParams, new ExamGrade.RowMapper());
    }

    public List<String> getExamParticipants(String exercise, String exam) {
        String query = "SELECT studentid FROM examparticipants WHERE exercise = :exercise AND examid = :exam";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        return this.jdbcTemplate.query(query, namedParams, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("studentid");
            }
        });
    }

    //return map with form <studentid, <taskid,points>>
    public Map<String, Map<String, Float>> getExamResults(String exercise, String exam) {
        String query = "SELECT * FROM examresults WHERE exercise = :exercise AND examid = :exam";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Map<String, Float>> results = new HashMap<>();
        while(rowSet.next()) {
            String studentId = rowSet.getString("studentid");
            if (results.containsKey(studentId)) {
                results.get(studentId).put(rowSet.getString("taskid"), rowSet.getFloat("points"));
            } else {
                Map<String, Float> studentResults = new HashMap<>();
                studentResults.put(rowSet.getString("taskid"), rowSet.getFloat("points"));
                results.put(studentId, studentResults);
            }
        }
        return results;
    }

    //return map with form <groupid, <taskid, avgpoints>>
    public Map<String, Map<String, Double>> getExamGroupTaskAvgPoints(String exercise, String exam) {
        String query = "SELECT ur.groupid, er.taskid, AVG(er.points) AS avgpoints\n" +
                "FROM examresults AS er\n" +
                "INNER JOIN users AS u ON u.studentid = er.studentid\n" +
                "INNER JOIN user_rights as ur ON ur.exerciseid = er.exercise AND ur.userid = u.userid\n" +
                "WHERE er.exercise = :exercise AND er.examid = :exam\n" +
                "GROUP BY ur.groupid, er.taskid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Map<String, Double>> result = new HashMap<>();
        while (rowSet.next()) {
            String groupId = rowSet.getString("groupid");
            if (groupId == null) groupId = "";
            Map<String, Double> m = result.get(groupId);
            if (m == null) {
                m = new HashMap<>();
                result.put(groupId, m);
            }
            m.put(rowSet.getString("taskid"), rowSet.getDouble("avgpoints"));
        }
        return result;
    }

    //return map with form <taskid, avgpoints>
    public Map<String, Double> getExamTaskAvgPoints(String exercise, String exam) {
        String query = "SELECT taskid, AVG(points) AS avgpoints\n" +
                "FROM examresults\n" +
                "WHERE exercise = :exercise AND examid = :exam\n" +
                "GROUP BY taskid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        HashMap<String, Double> result = new HashMap<>();
        while (rowSet.next()) {
            result.put(rowSet.getString("taskid"), rowSet.getDouble("avgpoints"));
        }
        return result;
    }

    //return map with form <group, #participants>
    public Map<String, Integer> getExamGroupParticipants(String exercise, String exam) {
        String query = "SELECT ur.groupid, COUNT(DISTINCT er.studentid) AS participants\n" +
                "FROM examresults AS er\n" +
                "INNER JOIN users AS u ON u.studentid = er.studentid\n" +
                "INNER JOIN user_rights as ur ON ur.exerciseid = er.exercise AND ur.userid = u.userid\n" +
                "WHERE er.exercise = :exercise AND er.examid = :exam\n" +
                "GROUP BY ur.groupid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Integer> result = new HashMap<>();
        while(rowSet.next()) {
            String groupId = rowSet.getString("groupid");
            if (groupId == null) groupId = "";
            result.put(groupId, rowSet.getInt("participants"));
        }
        return result;
    }

    public ExamGroupResults getExamGroupResults(String exercise, String exam) {
        return new ExamGroupResults(getExamGroupTaskAvgPoints(exercise, exam),
                getExamTaskAvgPoints(exercise, exam),
                getExamGroupParticipants(exercise, exam));
    }

    //return map with form <studentid,points>
    public Map<String, Float> getExamPoints(String exercise, String exam) {
        String query = "SELECT studentid, SUM(points) AS points FROM examresults WHERE exercise = :exercise AND examid = :exam\n" +
                "GROUP BY studentid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Float> results = new HashMap<>();
        while(rowSet.next()) {
            results.put(rowSet.getString("studentid"), rowSet.getFloat("points"));
        }
        return results;
    }

    //return map with form <taskid,points>
    public Map<String, Float> getExamResultsForStudent(String exercise, String exam, String studentId) {
        String query = "SELECT * FROM examresults WHERE exercise = :exercise AND examid = :exam AND studentid = :studentid";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        namedParams.put("studentid", studentId);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, namedParams);
        Map<String, Float> results = new HashMap<>();
        while(rowSet.next()) {
            results.put(rowSet.getString("taskid"), rowSet.getFloat("points"));
        }
        return results;
    }

    public boolean isParticipant(String exercise, String exam, String studentId) {
        String query = "SELECT COUNT(*) FROM examparticipants WHERE exercise = :exercise\n" +
                "AND examid = :exam AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        namedParams.put("studentid", studentId);

        return this.jdbcTemplate.queryForObject(query, namedParams, Integer.class) > 0;
    }

    public List<String> getParticipatingExams(String exercise, String studentId) {
        String query = "SELECT examid FROM examparticipants WHERE exercise = :exercise\n" +
                "AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentId);

        return this.jdbcTemplate.query(query, namedParams, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("examid");
            }
        });
    }

    //Returns true if exam exists and results are shown, otherwise false
    public boolean showResults(String exercise, String exam) {
        String query = "SELECT show_results FROM exams WHERE exercise = :exercise\n" +
                "AND id = :exam";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);

        return this.jdbcTemplate.query(query, namedParams, rs -> {
            try {
                return rs.next() ? rs.getBoolean("show_results") : false;
            } catch (SQLException ignored) {
                return false;
            }
        });
    }

    /*
        Update
     */

    public int createOrUpdateExam(Exam exam) {
        String query = "MERGE INTO exams(id, exercise, label, date, location, registration_open, show_results)\n" +
                "KEY (id, exercise)\n" +
                "VALUES (:id, :exercise, :label, :date, :location, :registrationOpen, :showResults)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(exam));
    }

    public int createOrUpdateExamTask(ExamTask task) {
        String query = "MERGE INTO examtasks(exercise, examid, id, max_points)\n" +
                "KEY (exercise, examid, id)\n" +
                "VALUES (:exercise, :examId, :id, :maxPoints)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(task));
    }

    public int createOrUpdateExamGrade(ExamGrade grade) {
        String query = "MERGE INTO examgrades(exercise, examid, grade, min_points)\n" +
                "KEY (exercise, examid, grade)\n" +
                "VALUES (:exercise, :examId, :grade, :minPoints)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(grade));
    }

    public int[] addExamParticipants(String exercise, String exam, List<String> studentIds) {
        String sql = "MERGE INTO examparticipants(exercise, examid, studentid)\n" +
                "KEY (exercise, examid, studentid)\n" +
                "VALUES (?, ?, ?)";
        return this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String studentId = studentIds.get(i);
                ps.setString(1, exercise);
                ps.setString(2, exam);
                ps.setString(3, studentId);
            }

            @Override
            public int getBatchSize() {
                return studentIds.size();
            }
        });
    }

    public void addExamResults(String exercise, String exam, String studentId, Map<String, Float> results) {
        String sql = "MERGE INTO examresults(exercise, examid, studentid, taskid, points)\n" +
                "KEY (exercise, examid, studentid, taskid)\n" +
                "VALUES (?, ?, ?, ?, ?)";
        List<Map.Entry<String, Float>> resultList =
                results.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .collect(Collectors.toList());
        this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<String, Float> result = resultList.get(i);
                ps.setString(1, exercise);
                ps.setString(2, exam);
                ps.setString(3, studentId);
                ps.setString(4, result.getKey());
                ps.setFloat(5, result.getValue());
            }

            @Override
            public int getBatchSize() {
                return resultList.size();
            }
        });
        List<String> removedTasks = results.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(e -> e.getKey())
                .collect(Collectors.toList());
        if (!removedTasks.isEmpty()) {
            String query = "DELETE FROM examresults WHERE " +
                    "exercise = :exercise " +
                    "AND examid = :exam " +
                    "AND studentid = :student " +
                    "AND taskid IN (:task)";
            Map<String, Object> namedParams = new HashMap<>();
            namedParams.put("exercise", exercise);
            namedParams.put("exam", exam);
            namedParams.put("student", studentId);
            namedParams.put("task", removedTasks);
            this.jdbcTemplate.update(query, namedParams);
        }
    }

    public int updateExamRegistrationStatus(String exercise, String exam, boolean registrationOpen) {
        String query = "UPDATE exams SET registration_open = :registrationOpen\n" +
                "WHERE exercise = :exercise AND id = :exam";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        namedParams.put("registrationOpen", String.valueOf(registrationOpen));
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int updateExamShowResults(String exercise, String exam, boolean showResults) {
        String query = "UPDATE exams SET show_results = :showResults\n" +
                "WHERE exercise = :exercise AND id = :exam";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("exam", exam);
        namedParams.put("showResults", String.valueOf(showResults));
        return this.jdbcTemplate.update(query, namedParams);
    }

    /*
        Delete
     */

    public int deleteExam(String id, String exercise) {
        String query = "DELETE FROM exams\n" +
                "WHERE id = :id AND exercise = :exercise";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("id", id);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int deleteExamTask(String exercise, String examid, String id) {
        String query = "DELETE FROM examtasks\n" +
                "WHERE id = :id AND exercise = :exercise AND examid = :examid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("id", id);
        namedParams.put("examid", examid);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int deleteExamGrade(String exercise, String examid, String grade) {
        String query = "DELETE FROM examgrades\n" +
                "WHERE grade = :grade AND exercise = :exercise AND examid = :examid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("grade", grade);
        namedParams.put("examid", examid);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int removeExamParticipant(String exercise, String examid, String studentId) {
        String query = "DELETE FROM examparticipants\n" +
                "WHERE studentid = :studentid AND exercise = :exercise AND examid = :examid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentId);
        namedParams.put("examid", examid);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int deleteExamResults(String exercise, String examid, String studentId) {
        String query = "DELETE FROM examresults\n" +
                "WHERE studentid = :studentid AND exercise = :exercise AND examid = :examid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("studentid", studentId);
        namedParams.put("examid", examid);
        return this.jdbcTemplate.update(query, namedParams);
    }
}
