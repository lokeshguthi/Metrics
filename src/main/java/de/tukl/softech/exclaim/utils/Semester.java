package de.tukl.softech.exclaim.utils;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Semester implements Comparable<Semester> {
    private static Comparator<Semester> comparator = Comparator.comparing(Semester::getYear).thenComparing(Semester::getSw);
    private int year;
    private SW sw;

    public Semester(int year, SW sw) {
        this.year = year;
        this.sw = sw;
    }

    public static Semester fromString(String s) {
        int year;
        SW sw;
        Pattern p = Pattern.compile(".*([0-9]+)/([0-9]+)");
        Matcher m = p.matcher(s);
        if (m.matches()) {
            sw = SW.WINTER;
            year = Integer.parseInt(m.group(1));
            if (year < 100) {
                year += 2000;
            }
        } else {
            p = Pattern.compile(".*([0-9]+)");
            m = p.matcher(s);
            if (m.matches()) {
                sw = SW.SUMMER;
                year = Integer.parseInt(m.group(1));
                if (year < 100) {
                    year += 2000;
                }
            } else {
                return new Semester(0, SW.SUMMER);
            }
        }
        return new Semester(year, sw);
    }

    @Override
    public int compareTo(Semester other) {
        return comparator.compare(this, other);
    }

    public int getYear() {
        return year;
    }

    public SW getSw() {
        return sw;
    }

    enum SW implements Comparable<SW> {
        SUMMER, WINTER
    }
}
