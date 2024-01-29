package de.tukl.softech.exclaim.data;

import de.tukl.softech.exclaim.uploads.UploadManager;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.apache.commons.lang3.SystemUtils;

public class Upload implements Cloneable {


    private int id;
    private String exercise;
    private String sheet;
    private Team team;
    private String assignment;
    private String filename;
    private DateTime uploadDate;
    private String uploaderStudentid;
    private DateTime deleteDate;
    private String deleterStudentid;
    private int totalUploadsNo;

    /**
     * @return the totalUploadsNo
     */
    public int getTotalUploadsNo() {
        return totalUploadsNo;
    }

    /**
     * @param totalUploadsNo the totalUploadsNo to set
     */
    public void setTotalUploadsNo(int totalUploadsNo) {
        this.totalUploadsNo = totalUploadsNo;
    }

    public String getAssignment() {

        return assignment;
    }

    public void setAssignment(String assignment) {
        this.assignment = assignment;
    }

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public DateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(DateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploaderStudentid() {
        return uploaderStudentid;
    }

    public void setUploaderStudentid(String uploaderStudentid) {
        this.uploaderStudentid = uploaderStudentid;
    }

    public DateTime getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(DateTime deleteDate) {
        this.deleteDate = deleteDate;
    }

    public String getDeleterStudentid() {
        return deleterStudentid;
    }

    public void setDeleterStudentid(String deleterStudentid) {
        this.deleterStudentid = deleterStudentid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Upload clone() {
        try {
            return (Upload) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * returns a link to this upload
     */
    public String linkToUpload() {
        return "/data/" + exercise
                + "/" + sheet
                + "/" + team.getGroup()
                + "/" + team.getTeam()
                + "/" + assignment + "/"
                + UriUtils.encodePathSegment(UploadManager.internFilename(uploadDate, filename), StandardCharsets.UTF_8.toString());
    }

    public String getInternFilename() {
        return UploadManager.internFilename(uploadDate, filename);
    }

    public String getPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return "data/" + exercise
                    + "/" + sheet
                    + "/" + team.getGroup()
                    + "-" + team.getTeam()
                    + "/" + assignment + "/"
                    + UploadManager.internFilename(uploadDate, filename);
        }
        else {
            return "data/" + exercise
                    + "/" + sheet
                    + "/" + team.getGroup()
                    + "|" + team.getTeam()
                    + "/" + assignment + "/"
                    + UploadManager.internFilename(uploadDate, filename);
        }
    }

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<Upload> {

        @Override
        public Upload mapRow(ResultSet rs, int rowNum) throws SQLException {
            Upload res = new Upload();

            res.setId(rs.getInt("id"));
            res.setExercise(rs.getString("exercise"));
            res.setSheet(rs.getString("sheet"));
            res.setTeam(TeamConverter.convertToTeam(rs.getString("team")));
            res.setAssignment(rs.getString("assignment"));
            res.setFilename(rs.getString("filename"));

            Timestamp upload_date = rs.getTimestamp("upload_date");
            res.setUploadDate(upload_date == null ? null : new DateTime(upload_date.getTime()));

            res.setUploaderStudentid(rs.getString("uploader_studentid"));

            Timestamp delete_date = rs.getTimestamp("delete_date");
            res.setDeleteDate(delete_date == null ? null : new DateTime(delete_date.getTime()));

            res.setDeleterStudentid(rs.getString("deleter_studentid"));
            return res;
        }
    }

    public static class RowMapperTotals implements org.springframework.jdbc.core.RowMapper<Upload> {

        @Override
        public Upload mapRow(ResultSet rs, int rowNum) throws SQLException {
            Upload res = new Upload();

            res.setExercise(rs.getString("exercise"));
            res.setSheet(rs.getString("sheet"));
            res.setTeam(TeamConverter.convertToTeam(rs.getString("team")));
            res.setAssignment(rs.getString("assignment"));
            res.setTotalUploadsNo(rs.getInt("totalUploadsNo"));
            return res;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Upload upload = (Upload) o;
        return Objects.equals(exercise, upload.exercise) &&
                Objects.equals(sheet, upload.sheet) &&
                Objects.equals(team, upload.team) &&
                Objects.equals(assignment, upload.assignment) &&
                Objects.equals(filename, upload.filename) &&
                Objects.equals(uploadDate, upload.uploadDate) &&
                Objects.equals(uploaderStudentid, upload.uploaderStudentid) &&
                Objects.equals(deleteDate, upload.deleteDate) &&
                Objects.equals(deleterStudentid, upload.deleterStudentid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exercise, sheet, team, assignment, filename, uploadDate, uploaderStudentid, deleteDate, deleterStudentid);
    }
}
