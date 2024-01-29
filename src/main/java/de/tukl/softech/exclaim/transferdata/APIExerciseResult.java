package de.tukl.softech.exclaim.transferdata;

import java.util.List;

public class APIExerciseResult {

    public APIExerciseResult(String sheet, double maxPoints, List<APIResult> results) {
        this.sheet = sheet;
        this.maxPoints = maxPoints;
        this.results = results;
    }

    private String sheet;
    private double maxPoints;
    private List<APIResult> results;

    public String getSheet() {
        return sheet;
    }

    public double getMaxPoints() {
        return maxPoints;
    }

    public List<APIResult> getResults() {
        return results;
    }
}
