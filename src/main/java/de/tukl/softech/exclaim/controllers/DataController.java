package de.tukl.softech.exclaim.controllers;

import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.transferdata.PreviewFileType;
import de.tukl.softech.exclaim.uploads.UploadManager;
import de.tukl.softech.exclaim.utils.Constants;
import de.tukl.softech.exclaim.utils.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.tukl.softech.exclaim.utils.Constants.FEEDBACK_SUB;

@Controller
public class DataController {
    private UploadManager uploadManager;
    private MetricsService metrics;


    public DataController(UploadManager uploadManager, MetricsService metrics) {
        this.uploadManager = uploadManager;
        this.metrics = metrics;
    }

    @GetMapping(value = "/data/{exid}/{sid}/{gid}/{tid}/{aid}/{filename:.*}")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public void getFile(@PathVariable("exid") String exercise,
                            @PathVariable("sid") String sheet,
                            @PathVariable("gid") String groupid,
                            @PathVariable("tid") String teamid,
                            @PathVariable("aid") String assignment,
                            @PathVariable("filename") String filename,
                            HttpServletResponse response) throws IOException {
        metrics.registerAccessFile();
        Team team = new Team(groupid, teamid);
        Path filePath = uploadManager.getUploadPath(exercise, sheet, assignment, team, filename);

        String headerFileName = filename;
        if (!assignment.equals(FEEDBACK_SUB)) {
            headerFileName = UploadManager.internalFilenameToFilename(headerFileName);
        }
        headerFileName = headerFileName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        String where;
        String extension = FileUtils.getExtension(filename);
        PreviewFileType fileType = Constants.previewFileTypeByExtension(extension);
        if (fileType != PreviewFileType.NoPreview) {
            where = "inline";
            if (fileType == PreviewFileType.Text) {
                response.setContentType("text/plain; charset=utf-8");
            } else if (fileType == PreviewFileType.PDF) {
                response.setContentType(MediaType.APPLICATION_PDF.toString());
            }
        } else {
            where = "attachment";
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM.toString());
        }

        response.setHeader("Content-Disposition", where + "; filename=\"" + headerFileName + "\"");
        Files.copy(filePath, response.getOutputStream());
    }

}
