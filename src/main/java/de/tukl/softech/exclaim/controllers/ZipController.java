package de.tukl.softech.exclaim.controllers;

import de.tukl.softech.exclaim.dao.UploadsDao;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Upload;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.SecurityTools;
import de.tukl.softech.exclaim.uploads.UploadManager;
import org.apache.pdfbox.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class ZipController {
    private static final Logger logger = LoggerFactory.getLogger(ZipController.class);

    private UploadManager uploadManager;
    private UploadsDao uploadsDao;
    private SecurityTools securityTools;
    private MetricsService metrics;


    public ZipController(UploadManager uploadManager, UploadsDao uploadsDao, SecurityTools securityTools,
                         MetricsService metrics) {
        this.uploadManager = uploadManager;
        this.uploadsDao = uploadsDao;
        this.securityTools = securityTools;
        this.metrics = metrics;
    }

    @GetMapping("/zip/{exid}/{sid}/{gid}/{tid}/{aid}")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public void assignmentZip(@PathVariable("exid") String exercise,
                              @PathVariable("sid") String sheet,
                              @PathVariable("gid") String groupid,
                              @PathVariable("tid") String teamid,
                              @PathVariable("aid") String assignment,
                              HttpServletResponse response) throws IOException {
        metrics.registerAccessZip();
        Team team = new Team(groupid, teamid);

        List<Upload> uploads = uploadsDao.getSnapshot(exercise, sheet, assignment, team, DateTime.now());

        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        String zipFilename = exercise + "_" + sheet + "_" + groupid + "_" + teamid + "_" + assignment + ".zip";
        response.addHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            addUploadsToZip(zipOutputStream, "", uploads);
        } catch (Throwable t) {
            logger.error("Error creating zip file for " + exercise + ", " + sheet, t);
            metrics.registerException(t);
        }
    }


    @GetMapping("/zip/{exid}/{sid}")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public void sheetZip(@PathVariable("exid") String exercise,
                         @PathVariable("sid") String sheet,
                         HttpServletResponse response) throws IOException {
        metrics.registerAccessZip();
        List<Team> accessibleTeams = securityTools.getAccessibleTeams(exercise);

        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        String zipFilename = exercise + "_" + sheet + ".zip";
        response.addHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + "\"");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {

            Map<Team, Map<String, List<Upload>>> uploads = uploadsDao.getUploadsForSheet(exercise, sheet).stream()
                    .collect(Collectors.groupingBy(Upload::getTeam,
                            Collectors.groupingBy(Upload::getAssignment)));

            for (Map.Entry<Team, Map<String, List<Upload>>> e1 : uploads.entrySet()) {
                Team team = e1.getKey();
                if (!accessibleTeams.contains(team)) {
                    continue;
                }
                String folder1 = team.getGroup() + "-" + team.getTeam() + "/";
                zipOutputStream.putNextEntry(new ZipEntry(folder1));


                for (Map.Entry<String, List<Upload>> e2 : e1.getValue().entrySet()) {
                    String assignment = e2.getKey();
                    String folder2 = folder1 + assignment + "/";
                    zipOutputStream.putNextEntry(new ZipEntry(folder2));

                    List<Upload> uploadList = e2.getValue();
                    addUploadsToZip(zipOutputStream, folder2, uploadList);
                }
            }
        } catch (Throwable t) {
            logger.error("Error creating zip file for " + exercise + ", " + sheet, t);
            metrics.registerException(t);
        }
    }

    private void addUploadsToZip(ZipOutputStream zipOutputStream, String folder, List<Upload> uploadList) throws IOException {
        Set<String> existingFilenames = new HashSet<>();
        for (Upload upload : uploadList) {
            if (upload.getDeleteDate() != null) {
                continue;
            }
            String filename = upload.getFilename();
            if (!existingFilenames.add(filename)) {
                continue;
            }

            zipOutputStream.putNextEntry(new ZipEntry(folder + filename));

            Path filePath = uploadManager.getUploadPath(upload);

            try (InputStream fileInputStream = Files.newInputStream(filePath)) {
                IOUtils.copy(fileInputStream, zipOutputStream);
            }

            zipOutputStream.closeEntry();
        }
    }

}
