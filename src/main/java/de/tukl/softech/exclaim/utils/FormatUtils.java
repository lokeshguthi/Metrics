package de.tukl.softech.exclaim.utils;

import de.tukl.softech.exclaim.controllers.ExerciseController;
import de.tukl.softech.exclaim.controllers.ExerciseController.OverviewStudentData;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Component("format")
public class FormatUtils {
    public String hello(String name) {
        return "Hello, " + name;
    }

    public String date(DateTime t) {
        return DateTimeFormat
                .forPattern("EEEEEE, dd.MM.YYY, HH:mm")
                .withLocale(Locale.GERMANY)
                .print(t);
    }

    public String day(DateTime t) {
        return DateTimeFormat
                .forPattern("dd.MM.YYY")
                .withLocale(Locale.GERMANY)
                .print(t);
    }

    public String points(float points) {
        NumberFormat nf = new DecimalFormat("##.#");
        return nf.format(points);
    }

    public String pointsOpt(Double points) {
        if (points == null) {
            return "";
        }
        NumberFormat nf = new DecimalFormat("##.#");
        return nf.format(points);
    }

    public String pointsDelta(float points) {
        NumberFormat nf = new DecimalFormat("+##.#;-##.#");
        return nf.format(points);
    }

    public String addNewline(String s) {
        return "\n" + s + "\n";
    }

    //replace non-printable characters
    public String characters(String s) {
        if (s != null) {
            return s.replace('\u0000', '\u2400'); //replace null character with null unicode
        } else {
            return null;
        }
    }

    public String annotationAndWarningCount(int annotationCount, int warningCount, int unread) {
        StringJoiner sj = new StringJoiner(", ");
        if (annotationCount == 1) {
            sj.add("1 Kommentar");
        } else if (annotationCount > 1) {
            sj.add(annotationCount + " Kommentare");
        }if (unread == 1) {
            sj.add("<strong>ungelesen</strong>");
        } else if (unread > 1) {
            sj.add("<strong>" + unread + " ungelesen</strong>");
        }
        if (warningCount == 1) {
            sj.add("1 Warnung");
        } else if (warningCount > 1) {
            sj.add(warningCount + " Warnungen");
        }
        return sj.toString();
    }

    public String getCommaDelimetedOfObjects(List<OverviewStudentData> list) {
        return list
                .stream()
                .map(studentInfo -> studentInfo.student.getFirstname() + ' ' + studentInfo.student.getLastname())
                .collect(Collectors.joining(", "));
    }


    public String internalDateTime(DateTime dateTime) {
        return Constants.DATE_FORMATER.print(dateTime);
    }

    public String overviewAnnotationAndWarning(ExerciseController.OverviewAssignmentData data, boolean deleted) {
        int annotationCount, warningCount;
        if (!deleted) {
            annotationCount = data.getAnnotationCount();
            warningCount = data.getWarningsCount();
            return annotationAndWarningCount(annotationCount, warningCount, data.unreadCount());
        } else {
            annotationCount = data.deletedAnnotationCount();
            warningCount = data.deletedWarningsCount();
            return annotationAndWarningCount(annotationCount, warningCount, data.deletedUnreadCount());
        }
    }

    public String booleanSymbol(boolean b) {
        if (b) {
            return "<span class=\"glyphicon glyphicon-ok\" aria-label=\"Ja\" aria-hidden=\"true\"></span>";
        } else {
            return "<span class=\"glyphicon glyphicon-remove\" aria-label=\"Nein\" aria-hidden=\"true\"></span>";
        }
    }

    public static String formatEmail(String to, List<StudentInfo> bcc) {
        String prefix = "mailto:" + to + "?bcc=";
        StringJoiner sj = new StringJoiner(", ", prefix, "");
        bcc.forEach(studentInfo -> sj.add(studentInfo.getEmail()));
        return sj.toString();
    }

    public String formatEmail(String to) {
        return "mailto:" + to;
    }

    public static String empty(String s) {
        if (s == null || s.isEmpty()) {
            return "-";
        } else {
            return s;
        }
    }

    public static String joinStrings(List<String> strings) {
        StringJoiner sj = new StringJoiner(", ");
        strings.forEach(sj::add);
        return sj.toString();
    }

    public static String weekDay(String day) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE").withLocale(Locale.ENGLISH);
        TemporalAccessor accessor = formatter.parse(day);
        return DayOfWeek.from(accessor).getDisplayName(TextStyle.FULL, Locale.GERMAN);
    }

    public String teamJson(Team team) {
        if (team == null) {
            return "null";
        }
        String g = team.getGroup();

        String t = team.getTeam();
        if (g == null)
            g = "null";
        else if (!g.matches("[0-9]+"))
            g = '"' + g + '"';

        if (t == null)
            t = "null";
        else if (!t.matches("[0-9]+"))
            t = '"' + t + '"';

        return "{group: " + g + ", team: " +t+" }";
    }
}
