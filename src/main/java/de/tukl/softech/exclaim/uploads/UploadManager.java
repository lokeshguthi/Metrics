package de.tukl.softech.exclaim.uploads;

import de.tukl.softech.exclaim.dao.UploadsDao;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Upload;
import de.tukl.softech.exclaim.utils.Constants;
import de.tukl.softech.exclaim.utils.TeamConverter;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UploadManager {
    private TransactionTemplate transactionTemplate;
    private UploadsDao uploadsDao;

    public UploadManager(TransactionTemplate transactionTemplate, UploadsDao uploadsDao) {
        this.transactionTemplate = transactionTemplate;
        this.uploadsDao = uploadsDao;
    }

    public static String internalFilenameToFilename(String internalFilename) {
        return internalFilename.substring(15);
    }

    public static DateTime internalFilenameToDate(String internalFilename) {
        return Constants.DATE_FORMATER.parseDateTime(internalFilename.substring(0, 14));
    }

    private static Path internFilePath(Upload upload) {
        return Paths.get(".",
                Constants.DATA_PATH,
                upload.getExercise(),
                upload.getSheet(),
                TeamConverter.convertToString(upload.getTeam()),
                upload.getAssignment(),
                internFilename(upload.getUploadDate(), upload.getFilename()));
    }

    public static String internFilename(DateTime uploadDate, String filename) {
        return Constants.DATE_FORMATER.print(uploadDate) + "-" + filename;
    }

    private static Path teamUploadFolder(String exercise, String sheet, String assignment, Team team) {
        return Paths.get(Constants.DATA_PATH, exercise, sheet, TeamConverter.convertToString(team), assignment);
    }

    public File getUploadFile(String exercise, String sheet, String assignment, Team team, String filename) {
        return getUploadPath(exercise, sheet, assignment, team, filename).toFile();
    }

    public Path getUploadPath(String exercise, String sheet, String assignment, Team team, String filename) {
        Path uploadPath = teamUploadFolder(exercise, sheet, assignment, team);
        return uploadPath.resolve(filename);
    }

    public Path getUploadPath(Upload upload) {
        return getUploadPath(upload.getExercise(), upload.getSheet(), upload.getAssignment(), upload.getTeam(), internFilename(upload.getUploadDate(), upload.getFilename()));
    }

    public File getUploadFile(Upload upload) {
        return getUploadPath(upload).toFile();
    }


    public void putUpload(String exercise, String sheet, String assignment, Team team, MultipartFile file, String uploaderStudentid) {
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }
        DateTime uploadDate = DateTime.now().withMillisOfSecond(0);

        Upload data = new Upload();
        data.setExercise(exercise);
        data.setSheet(sheet);
        data.setTeam(team);
        data.setAssignment(assignment);
        data.setUploadDate(uploadDate);
        data.setFilename(file.getOriginalFilename());
        data.setUploaderStudentid(uploaderStudentid);

        Path destPath = internFilePath(data);
        File uploadDir = destPath.getParent().toFile();
        uploadDir.mkdirs();
        if (!uploadDir.exists()) {
            throw new UploadException("Could not create upload folder " + uploadDir);
        }
        try {
            file.transferTo(destPath.toFile().getAbsoluteFile());
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Upload upload = data.clone();
                    upload.setDeleteDate(uploadDate);
                    upload.setDeleterStudentid(uploaderStudentid);
                    uploadsDao.markOldFilesDeleted(upload);
                    uploadsDao.insertUpload(data);
                }
            });
        } catch (IOException e) {
            throw new UploadException("Error moving upload", e);
        }
    }

    public void putFeedbackUpload(String exercise, String sheet, Team team, MultipartFile file) {
        Path filePath = teamUploadFolder(exercise, sheet, Constants.FEEDBACK_SUB, team).resolve(file.getOriginalFilename());
        File destFile = filePath.toFile();
        File uploadDir = filePath.getParent().toFile();
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new UploadException("Could not create upload folder " + uploadDir);
        }
        try {
            file.transferTo(destFile.getAbsoluteFile());
        } catch (IOException e) {
            throw new UploadException("Error moving feedback upload", e);
        }
    }

    public List<String> getFeedbackUploads(String exercise, String sheet, Team team) {
        try {
            Path feedbackUploadPath = teamUploadFolder(exercise, sheet, Constants.FEEDBACK_SUB, team);
            if (Files.isDirectory(feedbackUploadPath)) {
                return Files.walk(feedbackUploadPath, 1)
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new UploadException("Error listing feedback uploads", e);
        }
    }

    public Path getFeedbackFilePath(String exercise, String sheet, Team team, String filename) {
        Path filePath = teamUploadFolder(exercise, sheet, Constants.FEEDBACK_SUB, team).resolve(filename);
        if (Files.isRegularFile(filePath)) {
            return filePath;
        } else {
            throw new UploadException("Feedback file not found");
        }
    }
}
