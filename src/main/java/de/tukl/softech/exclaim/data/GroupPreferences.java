package de.tukl.softech.exclaim.data;

import model.other.PreferenceStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GroupPreferences {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<GroupPreferences> {

        @Override
        public GroupPreferences mapRow(ResultSet rs, int rowNum) throws SQLException {
            String[] preferred = rs.getString("preferred").split(",");
            String[] possible = rs.getString("possible").split(",");
            String[] dislike = rs.getString("dislike").split(",");
            String[] impossible = rs.getString("impossible").split(",");
            List<String> friends =  Arrays.asList(rs.getString("friends").split(","));

            Map<String, PreferenceStatus> preferences = new HashMap<>();
            Arrays.stream(preferred).filter(s -> !s.isEmpty()).forEach(s -> preferences.put(s, PreferenceStatus.PREFERRED));
            Arrays.stream(possible).filter(s -> !s.isEmpty()).forEach(s -> preferences.put(s, PreferenceStatus.POSSIBLE));
            Arrays.stream(dislike).filter(s -> !s.isEmpty()).forEach(s -> preferences.put(s, PreferenceStatus.DISLIKE));
            Arrays.stream(impossible).filter(s -> !s.isEmpty()).forEach(s -> preferences.put(s, PreferenceStatus.IMPOSSIBLE));

            return new GroupPreferences(rs.getString("exerciseid"), rs.getInt("userid"), preferences, friends);
        }
    }

    public Map<String, String> getParameterSource() {
        Map<String, String> parameterSource = new HashMap<>();
        parameterSource.put("exerciseid", exerciseId);
        parameterSource.put("userid", String.valueOf(userId));
        StringJoiner friendsJoiner = new StringJoiner(",");
        friends.forEach(friendsJoiner::add);
        parameterSource.put("friends", friendsJoiner.toString());

        StringJoiner preferred = new StringJoiner(",");
        StringJoiner possible = new StringJoiner(",");
        StringJoiner dislike = new StringJoiner(",");
        StringJoiner impossible = new StringJoiner(",");
        for (Map.Entry<String, PreferenceStatus> entry : preferences.entrySet()) {
            if (entry.getValue() == PreferenceStatus.PREFERRED) preferred.add(entry.getKey());
            else if (entry.getValue() == PreferenceStatus.POSSIBLE) possible.add(entry.getKey());
            else if (entry.getValue() == PreferenceStatus.DISLIKE) dislike.add(entry.getKey());
            else if (entry.getValue() == PreferenceStatus.IMPOSSIBLE) impossible.add(entry.getKey());
        }
        parameterSource.put("preferred", preferred.toString());
        parameterSource.put("possible", possible.toString());
        parameterSource.put("dislike", dislike.toString());
        parameterSource.put("impossible", impossible.toString());

        return parameterSource;
    }

    private String exerciseId;
    private int userId;

    private Map<String, PreferenceStatus> preferences;
    private List<String> friends;

    public GroupPreferences(String exerciseId, int userId) {
        this.exerciseId = exerciseId;
        this.userId = userId;
        this.preferences = new HashMap<>();
        this.friends = new ArrayList<>();
    }

    public GroupPreferences(String exerciseId, int userId, Map<String, PreferenceStatus> preferences, List<String> friends) {
        this.exerciseId = exerciseId;
        this.userId = userId;
        this.preferences = preferences;
        this.friends = friends;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public int getUserId() {
        return userId;
    }

    public Map<String, PreferenceStatus> getPreferences() {
        return preferences;
    }

    public List<String> getFriends() {
        return friends;
    }


}
