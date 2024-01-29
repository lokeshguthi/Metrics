package de.tukl.softech.exclaim.utils;

import de.tukl.softech.exclaim.transferdata.StudentInfo;
import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.function.Function;

public class StreamUtils {
    public static final Comparator<StudentInfo> FIRST_LAST_IGNORE_CASE = Comparator.comparing(StudentInfo::getLastname, String::compareToIgnoreCase)
            .thenComparing(StudentInfo::getFirstname, String::compareToIgnoreCase);
}
