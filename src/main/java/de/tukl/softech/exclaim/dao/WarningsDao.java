package de.tukl.softech.exclaim.dao;

import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.transferdata.FileWarnings;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class WarningsDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public WarningsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public List<FileWarnings.Warning> getWarningsForFile(int fileId) {
        String query = "SELECT * FROM warnings\n" +
                "WHERE fileid = :fileid\n";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("fileid", String.valueOf(fileId));
        return this.jdbcTemplate.query(query, namedParams, new FileWarnings.Warning.RowMapper());
    }

    public Map<Integer, List<FileWarnings.Warning>> getWarningsForSheet(String exercise, String sheet, Team team) {
        String query = "SELECT * FROM warnings w JOIN uploads u\n" +
                "ON w.fileid = u.id\n" +
                "WHERE u.exercise = :exercise\n" +
                "    AND u.sheet = :sheet\n" +
                "    AND u.team = :team";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("exercise", exercise);
        namedParams.put("sheet", sheet);
        namedParams.put("team", TeamConverter.convertToString(team));

        return this.jdbcTemplate.query(query, namedParams, new FileWarnings.FileWarningExtractor());
    }

    /*
        Update
     */

    public void createWarnings(List<FileWarnings> fileWarnings) {
        for (FileWarnings file : fileWarnings) {

            String sql = "MERGE INTO warnings (fileid, line, rule, ruleset, infourl, priority, message)\n" +
                    "KEY (fileid, line, rule)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(@Nonnull PreparedStatement ps, int i) throws SQLException {
                    FileWarnings.Warning warning = file.getWarnings().get(i);
                    ps.setInt(1, file.getFileId());
                    ps.setInt(2, warning.getBegin_line());
                    ps.setString(3, warning.getRule());
                    ps.setString(4, warning.getRule_set());
                    ps.setString(5, warning.getInfo_url());
                    ps.setInt(6, warning.getPriority());
                    ps.setString(7, warning.getMessage());
                }

                @Override
                public int getBatchSize() {
                    return file.getWarnings().size();
                }
            });
        }
    }

    /*
        Delete
     */

    public void deleteWarnings(Set<Integer> fileIds) {
        String sql = "DELETE FROM warnings WHERE fileid IN (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", fileIds);

        this.jdbcTemplate.update(sql, parameters);
    }
}
