package de.tukl.softech.exclaim.transferdata;

import de.tukl.softech.exclaim.data.Exercise;

import java.util.List;

public class APIExercise {

    private String id;
    private String lecture;
    private String term;
    private List<APISheet> sheets;

    public APIExercise() {
    }

    public APIExercise(Exercise exercise, List<APISheet> sheets) {
        id = exercise.getId();
        lecture = exercise.getLecture();
        term = exercise.getTerm();
        this.sheets = sheets;
    }

    public String getId() {
        return id;
    }

    public String getLecture() {
        return lecture;
    }

    public String getTerm() {
        return term;
    }

    public List<APISheet> getSheets() {
        return sheets;
    }

    public void setSheets(List<APISheet> sheets) {
        this.sheets = sheets;
    }

    public Exercise toExercise() {
        return new Exercise(id, lecture, term);
    }
}
