package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Annotation;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnnotationDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public AnnotationDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class SheetMapper implements org.springframework.jdbc.core.RowMapper<String> {

        @Override
        public String mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
            return rs.getString("sheet");
        }
    }

    /*
        Query
     */

    public List<Annotation> getAnnotationsForFile(int fileId) {
        String query = "SELECT * FROM annotations\n" +
                "WHERE fileid = :fileid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("fileid", String.valueOf(fileId));
        return this.jdbcTemplate.query(query, namedParams, new Annotation.RowMapper());
    }

    public List<Annotation> getAnnotationsForSheet(String exercise, String sheet, Team team) {
        String query = "SELECT * FROM annotations a JOIN uploads u\n" +
                "ON u.id = a.fileid\n" +
                "WHERE u.exercise = :exercise\n" +
                "    AND u.sheet = :sheet\n" +
                "    AND u.team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Annotation.RowMapper());
    }

    public List<String> getUnreadSheets(String studentId, String exercise) {
        String query = "SELECT DISTINCT sheet FROM uploads u JOIN unread r ON u.id = r.fileid\n" +
                "WHERE studentid = :studentid AND exercise = :exercise";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("studentid", studentId);
        namedParams.put("exercise", exercise);
        return this.jdbcTemplate.query(query, namedParams, new SheetMapper());
    }

    public List<Integer> getUnreadUploads(String studentId, String exercise, String sheet) {
        String query = "SELECT u.id AS id FROM uploads u JOIN unread r ON u.id = r.fileid\n" +
                "WHERE studentid = :studentid AND exercise = :exercise AND sheet = :sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("studentid", studentId);
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParams, new RowMapper<Integer>() {
            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("id");
            }
        });
    }

    /*
        Update
     */

    public int createOrUpdateAnnotation(Annotation annotation) {
        String query = "MERGE INTO annotations (fileid, line, annotationObj)\n" +
                "KEY (fileid, line)\n" +
                "VALUES (:fileId, :line, :annotationObj)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(annotation));
    }

    public int[] markUnread(List<String> studentIds, int fileId) {
        String sql = "MERGE INTO unread(fileid, studentid)\n" +
                "KEY (fileid, studentid)\n" +
                "VALUES (?, ?)";
        return this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, fileId);
                ps.setString(2, studentIds.get(i));
            }

            @Override
            public int getBatchSize() {
                return studentIds.size();
            }
        });
    }

    /*
        Delete
     */

    public int deleteAnnotation(Annotation annotation) {
        String query = "DELETE FROM annotations\n" +
                "WHERE   fileid = :fileId\n" +
                "    AND line = :line";
        return this.jdbcTemplate.update(query, new BeanParameterSource(annotation));
    }

    public int markRead(String studentId, int fileId) {
        String query = "DELETE FROM unread\n" +
                "WHERE fileid = :fileid AND studentid = :studentid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("fileid", String.valueOf(fileId));
        namedParams.put("studentid", studentId);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int markRead(int fileId) {
        String query = "DELETE FROM unread\n" +
                "WHERE fileid = :fileid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("fileid", String.valueOf(fileId));
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int[] markRead(String studentId, List<Integer> fileIds) {
        String sql = "DELETE FROM unread\n" +
                "WHERE  fileid = ? AND studentid = ?";
        return this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, fileIds.get(i));
                ps.setString(2, studentId);
            }

            @Override
            public int getBatchSize() {
                return fileIds.size();
            }
        });
    }

}
