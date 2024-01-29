package de.tukl.softech.exclaim.data;

public class Studentres {
    private String sheet;
    private String exercise;
    private String studentid;
    private Team team;

    public Studentres(String sheet, String exercise, String studentid, Team team) {
        this.sheet = sheet;
        this.exercise = exercise;
        this.studentid = studentid;
        this.team = team;
    }

    public String getSheet() {

        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getStudentid() {
        return studentid;
    }

    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
