package de.tukl.softech.exclaim.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExamGroupResults {
    private Map<String, Map<String, Double>> groupTaskAvgPoints;
    private Map<String, Double> taskAvgPoints;
    private Map<String, Integer> groupParticipants;

    public ExamGroupResults(Map<String, Map<String, Double>> groupTaskAvgPoints, Map<String, Double> taskAvgPoints, Map<String, Integer> groupParticipants) {
        this.groupTaskAvgPoints = groupTaskAvgPoints;
        this.taskAvgPoints = taskAvgPoints;
        this.groupParticipants = groupParticipants;
    }

    public List<String> getGroups() {
        List<String> result = new ArrayList<>(this.groupParticipants.keySet());
        Collections.sort(result);
        return result;
    }

    public List<String> getTasks() {
        List<String> result = new ArrayList<>(this.taskAvgPoints.keySet());
        Collections.sort(result);
        return result;
    }

    public double getTaskAvgPoints(String task) {
        Double result = this.taskAvgPoints.get(task);
        return result == null ? 0 : result;
    }

    public double getGroupTaskAvgPoints(String group, String task) {
        Map<String, Double> results = this.groupTaskAvgPoints.get(group);
        if (results == null) return 0;
        Double result = results.get(task);
        return result == null ? 0 : result;
    }

    public double getGroupAvgPoints(String group) {
        Map<String, Double> results = this.groupTaskAvgPoints.get(group);
        return results == null ? 0 : results.values().stream().reduce(0.0, Double::sum);
    }

    public int getGroupParticipants(String group) {
        Integer result = this.groupParticipants.get(group);
        return result == null ? 0 : result;
    }

    public double getAvgPoints() {
        return this.taskAvgPoints.values().stream().reduce(0.0, Double::sum);
    }

    public int getParticipants() {
        return this.groupParticipants.values().stream().reduce(0, Integer::sum);
    }
}
