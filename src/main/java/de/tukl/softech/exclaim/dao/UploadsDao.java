package de.tukl.softech.exclaim.dao;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.data.Result;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Upload;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class UploadsDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public UploadsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static List<DateTime> snapshotFromUploads(List<Upload> uploads) {
        return uploads.stream()
                .flatMap(u -> Stream.of(u.getUploadDate(), u.getDeleteDate()))
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public static Optional<DateTime> latestSnapshotFromUploads(List<Upload> uploads) {
        return uploads.stream()
                .flatMap(u -> Stream.of(u.getUploadDate(), u.getDeleteDate()))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
    }

    public List<Upload> getUploadsForSheet(String exercise, String sheet) {
        String query = "SELECT * FROM uploads\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        return this.jdbcTemplate.query(query, namedParams, new Upload.RowMapper());
    }

    public List<Upload> getUploadsForSheetAndTeam(String exercise, String sheet, Team team) {
        String query = "SELECT * FROM uploads\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Upload.RowMapper());
    }

    public List<Upload> getUploadsForAssignmentAndTeam(String exercise, String sheet, String assignment, Team team) {
        String query = "SELECT * FROM uploads\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND assignment = :assignment\n" +
                "AND team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        return this.jdbcTemplate.query(query, namedParams, new Upload.RowMapper());
    }

    /**
     * Returns uploads for a specific snapshot time.
     */
    public List<Upload> getSnapshot(String exercise, String sheet, String assignment, Team team, DateTime snapshot) {
        String query = "SELECT * FROM uploads\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND assignment = :assignment\n" +
                "AND team = :team\n" +
                "AND upload_date <= :snapshot\n" +
                "AND (delete_date IS NULL OR delete_date > :snapshot)";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        namedParams.put("snapshot", new Timestamp(snapshot.getMillis()));
        return this.jdbcTemplate.query(query, namedParams, new Upload.RowMapper());
    }

    /**
     * Get all snapshot times
     */
    public List<DateTime> getSnapshots(String exercise, String sheet, String assignment, Team team) {
        List<Upload> uploads = getUploadsForAssignmentAndTeam(exercise, sheet, assignment, team);
        return snapshotFromUploads(uploads);
    }

    public List<Upload> getTotalUploadsForListOfTeams(String exercise, String sheet, List<Team> teams) {
        String query = "SELECT u.exercise, u.sheet, u.team, u.assignment, Count(*) AS totalUploadsNo   \r\n" + 
                "FROM uploads u   \r\n" + 
                "WHERE u.exercise = :ex AND u.sheet = :sh AND u.team = ANY(:tms) AND u.delete_date IS NULL   \r\n" + 
                "GROUP BY (u.exercise, u.sheet, u.team, u.assignment)   \r\n";
        String[] teamsArray = teams.stream().map(team -> TeamConverter.convertToString(team)).toArray(String[]::new);
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("ex", exercise);
        namedParams.put("sh", sheet);
        namedParams.put("tms", teamsArray);
        return this.jdbcTemplate.query(query, namedParams, new Upload.RowMapperTotals());
    }

    public int getUploadId(String exercise, String sheet, String assignment, Team team, String filename, DateTime uploadDate) {
        String query = "SELECT MAX(id) FROM uploads\n" +
                "WHERE exercise = :exercise AND sheet = :sheet AND assignment = :assignment\n" +
                "AND team = :team AND filename = :filename AND upload_date = :upload_date";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("assignment", assignment);
        namedParams.put("team", TeamConverter.convertToString(team));
        namedParams.put("filename", filename);
        namedParams.put("upload_date", uploadDate.toString());

        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    /*
        Update
     */

    public int insertUpload(Upload upload) {
        String query = "INSERT INTO uploads(exercise, sheet, team, assignment, upload_date, filename, uploader_studentid)\n" +
                "VALUES (:exercise, :sheet, :team, :assignment, :uploadDate, :filename, :uploaderStudentid)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(upload));
    }

    public int markDeleted(Upload upload) {
        String query = "UPDATE uploads\n" +
                "SET delete_date = :deleteDate,\n" +
                "    deleter_studentid = :deleterStudentid\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND filename = :filename\n" +
                "AND upload_date = :uploadDate";
        return this.jdbcTemplate.update(query, new BeanParameterSource(upload));
    }

    public int markOldFilesDeleted(Upload upload) {
        String query = "UPDATE uploads\n" +
                "SET delete_date = :deleteDate,\n" +
                "    deleter_studentid = :deleterStudentid\n" +
                "WHERE exercise = :exercise\n" +
                "AND sheet = :sheet\n" +
                "AND team = :team\n" +
                "AND assignment = :assignment\n" +
                "AND filename = :filename\n" +
                "AND delete_date IS NULL" ;
        return this.jdbcTemplate.update(query, new BeanParameterSource(upload));
    }

    public int updateStudentId(String oldId, String newId) {
        String query = "SELECT COUNT(*) FROM uploads WHERE uploader_studentid = :studentid OR deleter_studentid = :studentid";
        Integer count = jdbcTemplate.queryForObject(query, ImmutableMap.of("studentid", newId), Integer.class);
        if (count > 0) {
            throw new IllegalArgumentException("new student id " + newId + " already has " + count + " entries in uploads");
        }
        String update = "UPDATE uploads SET uploader_studentid = :newid WHERE uploader_studentid = :oldid";
        int res1 = jdbcTemplate.update(update, ImmutableMap.of("oldid", oldId, "newid", newId));
        update = "UPDATE uploads SET deleter_studentid = :newid WHERE deleter_studentid = :oldid";
        return res1 + jdbcTemplate.update(update, ImmutableMap.of("oldid", oldId, "newid", newId));
    }

}
