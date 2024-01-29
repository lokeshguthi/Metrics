package de.tukl.softech.exclaim.transferdata;

public class APIResult {
    private String studentid;
    private double points;

    public APIResult(String studentid, double points) {
        this.studentid = studentid;
        this.points = points;
    }

    public String getStudentid() {
        return studentid;
    }

    public double getPoints() {
        return points;
    }
}
