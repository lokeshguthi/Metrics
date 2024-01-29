package de.tukl.softech.exclaim.data;

import javax.annotation.Nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Annotation {
    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Annotation> {

        @Override
        public Annotation mapRow(@Nonnull ResultSet rs, int rowNum) throws SQLException {
            return new Annotation(rs.getInt("fileid"), rs.getInt("line"),
                    rs.getString("annotationObj"));
        }
    }

    private int fileId;
    private int line;
    private String annotationObj;

    public Annotation(int fileId, int line, String annotationObj) {
        this.fileId = fileId;
        this.line = line;
        this.annotationObj = annotationObj;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getAnnotationObj() {
        return annotationObj;
    }

    public void setAnnotationObj(String annotationObj) {
        this.annotationObj = annotationObj;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }
}
