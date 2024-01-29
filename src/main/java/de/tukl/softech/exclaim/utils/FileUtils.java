package de.tukl.softech.exclaim.utils;

public class FileUtils {
    public static String getExtension(String rawFilename) {
        int index = rawFilename.lastIndexOf(".");
        if (index < 0) {
            return "";
        }
        return rawFilename.substring(index + 1);
    }
}
