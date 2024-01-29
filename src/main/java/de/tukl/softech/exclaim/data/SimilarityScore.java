package de.tukl.softech.exclaim.data;

public class SimilarityScore {


    private double score;
    private String team1;
    private String team2;
    private String group1;
    private String group2;
    private String filename1;
    private String filename2;
    private String sheet;
    private String assignment;

    public SimilarityScore(
            double score, String team1, String team2, String group1, String group2,
            String filename1, String filename2, String sheet, String assignment) {
        this.score = score;
        this.team1 = team1;
        this.team2 = team2;
        this.group1 = group1;
        this.group2 = group2;
        this.filename1 = filename1;
        this.filename2 = filename2;
        this.sheet = sheet;
        this.assignment = assignment;
    }


    public double getScore() {
        return score;
    }

    public String getTeam1() {
        return team1;
    }

    public String getTeam2() {
        return team2;
    }

    public String getGroup1() {
        return group1;
    }

    public String getGroup2() {
        return group2;
    }

    public String getFilename1() {
        return filename1;
    }

    public String getFilename2() {
        return filename2;
    }

    public String getSheet() {
        return sheet;
    }

    public String getAssignment() {
        return assignment;
    }


}
