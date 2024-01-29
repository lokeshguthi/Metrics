package de.tukl.softech.exclaim.data;

public class AssignmentAndTeam {
    private String assignment;
    private Team team;

    public AssignmentAndTeam(String assignment, Team team) {
        this.assignment = assignment;
        this.team = team;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssignmentAndTeam that = (AssignmentAndTeam) o;

        if (assignment != null ? !assignment.equals(that.assignment) : that.assignment != null) return false;
        return team != null ? team.equals(that.team) : that.team == null;
    }

    @Override
    public int hashCode() {
        int result = assignment != null ? assignment.hashCode() : 0;
        result = 31 * result + (team != null ? team.hashCode() : 0);
        return result;
    }

    public String getAssignment() {

        return assignment;
    }

    public Team getTeam() {
        return team;
    }

    @Override
    public String toString() {
        return "Aufgabe " + assignment + ", Team " + team;
    }
}
