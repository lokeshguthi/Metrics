package de.tukl.softech.exclaim.dao;


import de.tukl.softech.exclaim.controllers.UserController;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.User;
import de.tukl.softech.exclaim.transferdata.ExerciseRights;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import de.tukl.softech.exclaim.utils.BeanParameterSource;
import org.springframework.dao.DuplicateKeyException;
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

@Component
public class UserDao {
    private NamedParameterJdbcTemplate jdbcTemplate;

    public UserDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
        Query
     */

    public List<User> getAllUsers() {
        String query = "SELECT * FROM users;";
        return this.jdbcTemplate.query(query,  new User.RowMapper());
    }

    public User getUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username  = :username AND password = :password;";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("username", username);
        namedParams.put("password", password);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new User.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getUser(String username) {
        String query = "SELECT * FROM users WHERE username  = :username;";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("username", username);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new User.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<String, Integer> getUserIds(List<String> usernames) {
        String query = "SELECT username, userid FROM users WHERE username  IN (:usernames);";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("usernames", usernames);

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, parameters);
        Map<String, Integer> result = new HashMap<>();
        while (rowSet.next()) {
            result.put(rowSet.getString("username"), rowSet.getInt("userid"));
        }
        return result;
    }

    public User getUserByStudentId(String studentid) {
        String query = "SELECT * FROM users WHERE studentid  = :studentid;";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("studentid", studentid);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new User.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User getUserByMail(String email) {
        String query = "SELECT * FROM users WHERE email  = :email;";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("email", email);
        return this.jdbcTemplate.query(query, namedParams, new User.RowMapper()).stream().findFirst().orElse(null);
    }

    public User getUserById(int userid) {
        String query = "SELECT * FROM users WHERE userid  = :userid;";
        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        try {
            return this.jdbcTemplate.queryForObject(query, namedParams, new User.RowMapper());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<ExerciseRights> getUserRights(int userid) {
        String query = "SELECT * FROM user_rights WHERE userid = :userid";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("userid", String.valueOf(userid));
        return this.jdbcTemplate.query(query, namedParameters, new ExerciseRights.RowMapper());
    }

    public List<ExerciseRights> getUserRightsForExercise(int userid, String exercise) {
        String query = "SELECT * FROM user_rights WHERE userid = :userid AND exerciseid = :exerciseid";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("userid", String.valueOf(userid));
        namedParameters.put("exerciseid", exercise);
            return this.jdbcTemplate.query(query, namedParameters, new ExerciseRights.RowMapper());
    }

    public List<StudentInfo> getStudentsInExercise(String exercise) { //TODO check
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid WHERE exerciseid = :exercise AND role = 'student'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        return this.jdbcTemplate.query(query, namedParameters, new StudentInfo.RowMapper());

    }

    public List<User> getStudentsUserInExercise(String exercise) {
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid WHERE exerciseid = :exercise AND role = 'student'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        return this.jdbcTemplate.query(query, namedParameters, new User.RowMapper());
    }

    public List<StudentInfo> getStudentsInTeam(String exercise, Team team) { //TODO check
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid " +
                "WHERE exerciseid = :exercise AND role = 'student' AND groupid = :groupid AND teamid = :teamid";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("groupid", team.getGroup());
        namedParameters.put("teamid", team.getTeam());
        return this.jdbcTemplate.query(query, namedParameters, new StudentInfo.RowMapper());
    }

    public StudentInfo getStudentInfo(String exercise, String studentId) { //TODO check
        String query = "SELECT * FROM user_rights r " +
                "INNER JOIN users u ON r.userid = u.userid " +
                "WHERE studentid = :studentid " +
                "AND exerciseid = :exercise " +
                "AND role = 'student'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("studentid", studentId);
        return this.jdbcTemplate.queryForObject(query, namedParameters, new StudentInfo.RowMapper());
    }

    public List<StudentInfo> getStudentsInGroup(String exercise, String group) { //TODO check
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid " +
                "WHERE exerciseid = :exercise AND role = 'student' AND ((:groupid IS NULL AND groupid IS NULL) OR groupid = :groupid)";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", exercise);
        namedParameters.put("groupid", group);
        return this.jdbcTemplate.query(query, namedParameters, new StudentInfo.RowMapper());
    }

    public List<StudentInfo> getStudentInfos(String exercise, List<String> studentids) { //TODO check
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid WHERE u.studentid IN (:studentid) AND r.exerciseid = :exercise";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("studentid", studentids);
        namedParameters.put("exercise", exercise);
        return this.jdbcTemplate.query(query, namedParameters, new StudentInfo.RowMapper());

    }

    public List<Team> getExerciseTeams(String exercise) { //TODO check
        String query = "SELECT DISTINCT groupid, teamid FROM user_rights WHERE exerciseid = :exerciseid AND role = 'student'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exerciseid", exercise);
        return this.jdbcTemplate.query(query, namedParameters, new Team.RowMapper());
    }

    public List<Team> getExerciseTeams(String exercise, List<String> groups) { //TODO check
        String query = "SELECT DISTINCT groupid, teamid FROM user_rights WHERE exerciseid = :exerciseid AND groupid IN (:groups) AND role = 'student'";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("exerciseid", exercise);
        namedParameters.put("groups", groups);
        return this.jdbcTemplate.query(query, namedParameters, new Team.RowMapper());
    }

    //returns map with exercise id and number of students
    public Map<String, Integer> getStudentCountForExercise() {
        String query = "SELECT exerciseid, COUNT(*) AS count FROM user_rights\n" +
                "WHERE role = 'student' GROUP BY exerciseid";

        SqlRowSet rowSet = this.jdbcTemplate.queryForRowSet(query, new HashMap<>());
        Map<String, Integer> studentCount = new HashMap<>();
        while (rowSet.next()) {
            studentCount.put(rowSet.getString("exerciseid"), rowSet.getInt("count"));
        }
        return studentCount;
    }

    public List<User> getExerciseAssistants(String exercise) {
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid WHERE r.exerciseid = :exercise\n" +
                "AND r.role = 'assistant'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", String.valueOf(exercise));
        return this.jdbcTemplate.query(query, namedParameters, new User.RowMapper());
    }

    public List<UserController.Tutor> getExerciseTutors(String exercise) {
        String query = "SELECT * FROM user_rights r INNER JOIN users u ON r.userid = u.userid WHERE r.exerciseid = :exercise\n" +
                "AND r.role = 'tutor'";
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("exercise", String.valueOf(exercise));
        return this.jdbcTemplate.query(query, namedParameters, (rs, rowNum) -> {
            UserController.Tutor tutor = new UserController.Tutor();
            tutor.user = new User.RowMapper().mapRow(rs, rowNum);
            tutor.group = rs.getString("groupid");
            return tutor;
        });
    }

    /*
        Update
     */

    public int createUser(User user) throws DuplicateKeyException {
        String query = "INSERT INTO users(firstname, lastname, username, studentid, email, password, verified, admin, code)\n" +
                    "VALUES (:firstname, :lastname, :username, :studentid, :email, :password, :verified, :admin, :code)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(user));
    }

    //used for converter
    public int mergeUser(User user) throws DuplicateKeyException {
        String query = "MERGE INTO users(firstname, lastname, username, studentid, email, password, verified, admin)\n" +
                "KEY (username)\n" +
                "VALUES (:firstname, :lastname, :username, :studentid, :email, :password, :verified, :admin)";
        return this.jdbcTemplate.update(query, new BeanParameterSource(user));
    }

    public int addUserRights(int userid, ExerciseRights rights) throws DuplicateKeyException {
        String query = "INSERT INTO user_rights(userid, exerciseid, role, groupid, teamid)\n" +
                "VALUES (:userid, :exerciseid, :role, :groupid, :teamid)";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("userid", String.valueOf(userid));
        namedParams.put("exerciseid", rights.getExerciseId());
        namedParams.put("role", rights.getRole().name());
        namedParams.put("groupid", rights.getGroupId());
        namedParams.put("teamid", rights.getTeamId());
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int updateUserRights(int userid, ExerciseRights rights) throws DuplicateKeyException {
        String query = "UPDATE user_rights SET role = :role, groupid = :groupid, teamid = :teamid\n" +
                "WHERE userid = :userid AND exerciseid = :exerciseid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("userid", String.valueOf(userid));
        namedParams.put("exerciseid", rights.getExerciseId());
        namedParams.put("role", rights.getRole().name());
        namedParams.put("groupid", rights.getGroupId());
        namedParams.put("teamid", rights.getTeamId());
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int[] mergeUserRights(List<ExerciseRights> exerciseRights) {
        String sql = "MERGE INTO user_rights(userid, exerciseid, role, groupid, teamid)\n" +
                "KEY (userid, exerciseid)\n" +
                "VALUES (?, ?, ?, ?, ?)";
        return this.jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ExerciseRights rights = exerciseRights.get(i);
                ps.setInt(1, rights.getUserId());
                ps.setString(2, rights.getExerciseId());
                ps.setString(3, rights.getRole().name());
                ps.setString(4, rights.getGroupId());
                ps.setString(5, rights.getTeamId());
            }

            @Override
            public int getBatchSize() {
                return exerciseRights.size();
            }
        });
    }

    public int setPassword(int userid, String password) {
        String query = "UPDATE users SET password = :password\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("password", password);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int changeName(int userid, String firstname, String lastname) {
        String query = "UPDATE users SET firstname = :firstname, lastname = :lastname\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("firstname", firstname);
        namedParams.put("lastname", lastname);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int changeUsername(int userid, String username) {
        String query = "UPDATE users SET username = :username\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("username", username);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int setCode(int userid, String code) {
        String query = "UPDATE users SET code = :code\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("code", code);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int setEmail(int userid, String email) {
        String query = "UPDATE users SET email = :email\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("email", email);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int setAdmin(int userid, boolean admin) {
        String query = "UPDATE users SET admin = :admin\n" +
                "WHERE userid = :userid";

        Map<String, Object> namedParams = new HashMap<>();
        namedParams.put("userid", userid);
        namedParams.put("admin", String.valueOf(admin));
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int activate(String username) {
        String query = "UPDATE users SET verified = TRUE, code = NULL\n" +
                "WHERE username = :username";

        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("username", username);
        return this.jdbcTemplate.update(query, namedParams);
    }


    /*
        Delete
     */

    public int deleteUserRights(int userid, String exercise) {
        String query = "DELETE FROM user_rights\n" +
                "WHERE userid = :userid AND exerciseid = :exerciseid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("userid", String.valueOf(userid));
        namedParams.put("exerciseid", exercise);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int deleteUserRights(int userid, String exercise, String group) {
        String query = "DELETE FROM user_rights\n" +
                "WHERE userid = :userid AND exerciseid = :exerciseid AND groupid = :groupid ";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("userid", String.valueOf(userid));
        namedParams.put("exerciseid", exercise);
        namedParams.put("groupid", group);
        return this.jdbcTemplate.update(query, namedParams);
    }

    public int deleteUser(int userid) {
        String query = "DELETE FROM users\n" +
                "WHERE userid = :userid";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("userid", String.valueOf(userid));
        return this.jdbcTemplate.update(query, namedParams);
    }


    public int updateStudentId(String oldId, String newId) {
        String query = "UPDATE users\n" +
                "SET studentid = :newId\n" +
                "WHERE studentid = :oldId";
        Map<String, String> namedParams = new HashMap<>();
        namedParams.put("oldId", oldId);
        namedParams.put("newId", newId);
        return this.jdbcTemplate.update(query, namedParams);
    }
}
