package de.tukl.softech.exclaim.uploads;

import org.joda.time.DateTime;

import java.nio.file.Path;

public class LegacyUpload {
    private String name;
    private String internFilename;
    private Path path;
    private DateTime date;
    private boolean deleted;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInternFilename() {
        return internFilename;
    }

    public void setInternFilename(String internFilename) {
        this.internFilename = internFilename;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
