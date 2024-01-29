package de.tukl.softech.exclaim.data;

public class AttendanceOverview {
    private int misses;
    private int total;

    public AttendanceOverview(int misses, int total) {
        this.misses = misses;
        this.total = total;
    }

    public AttendanceOverview() {
    }

    public int getMisses() {
        return misses;
    }

    public int getTotal() {
        return total;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
