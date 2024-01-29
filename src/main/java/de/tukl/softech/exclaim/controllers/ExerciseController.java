package de.tukl.softech.exclaim.controllers;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.opencsv.bean.CsvToBeanBuilder;
import de.tukl.softech.exclaim.dao.*;
import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.AccessChecker;
import de.tukl.softech.exclaim.security.SecurityTools;
import de.tukl.softech.exclaim.transferdata.*;
import de.tukl.softech.exclaim.uploads.UploadException;
import de.tukl.softech.exclaim.uploads.UploadManager;
import de.tukl.softech.exclaim.utils.*;
import de.tukl.softech.exclaim.utils.RteServices.TestName;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tukl.softech.exclaim.utils.StreamUtils.FIRST_LAST_IGNORE_CASE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Controller
public class ExerciseController {
    private static final Logger logger = LoggerFactory.getLogger(ExerciseController.class);

    private SheetDao sheetDao;
    private AttendanceDao attendanceDao;
    private AssignmentDao assignmentDao;
    private ResultsDao resultsDao;
    private CommentsDao commentsDao;
    private DeltapointsDao deltapointsDao;
    private AnnotationDao annotationDao;
    private UploadsDao uploadsDao;
    private AccessChecker accessChecker;
    private SecurityTools securityTools;
    private UploadManager uploadManager;
    private TestresultDao testresultDao;
    private RteServices rteServices;
    private MetricsService metrics;
    private AdmissionDao admissionDao;
    private WarningsDao warningsDao;
    private ExamDao examDao;
    private UserDao userDao;
    private ExerciseDao exerciseDao;
    private GroupDao groupDao;
    private SimilarityCheckerDao similarityCheckerDao;

    @Value("${exclaim.csv.separator:;}")
    private char csvSeparator;

    public ExerciseController(SheetDao sheetDao, AttendanceDao attendanceDao, AssignmentDao assignmentDao,
                              ResultsDao resultsDao, CommentsDao commentsDao, DeltapointsDao deltapointsDao,
                              AnnotationDao annotationDao, UploadsDao uploadsDao,
                              AccessChecker accessChecker, SecurityTools securityTools,
                              UploadManager uploadManager, TestresultDao testresultDao, RteServices rteServices,
                              MetricsService metrics, AdmissionDao admissionDao, WarningsDao warningsDao,
                              ExamDao examDao, UserDao userDao, ExerciseDao exerciseDao, GroupDao groupDao, SimilarityCheckerDao similarityCheckerDao) {
        this.sheetDao = sheetDao;
        this.attendanceDao = attendanceDao;
        this.assignmentDao = assignmentDao;
        this.resultsDao = resultsDao;
        this.commentsDao = commentsDao;
        this.deltapointsDao = deltapointsDao;
        this.annotationDao = annotationDao;
        this.uploadsDao = uploadsDao;
        this.accessChecker = accessChecker;
        this.securityTools = securityTools;
        this.uploadManager = uploadManager;
        this.testresultDao = testresultDao;
        this.rteServices = rteServices;
        this.metrics = metrics;
        this.admissionDao = admissionDao;
        this.warningsDao = warningsDao;
        this.examDao = examDao;
        this.userDao = userDao;
        this.exerciseDao = exerciseDao;
        this.groupDao = groupDao;
        this.similarityCheckerDao = similarityCheckerDao;
    }

    @GetMapping("/exercise/{exid}")
    @PreAuthorize("@accessChecker.canAccess(#exercise)")
    public String getExercise(@PathVariable("exid") String exercise, Model model) {
        metrics.registerAccess("exercise");
        List<Sheet> rawSheets = sheetDao.getSheetsForExercise(exercise);
        boolean isOnlyStudent  = accessChecker.isOnlyStudent(exercise);
        List<SheetResult> sheets = rawSheets.stream().map(SheetResult::new).collect(Collectors.toList());
        String admissionMessage = null;
        Team team = null;
        List<StudentInfo> teamMembers = null;
        List<ExamOverview> exams = examDao.getExamsForExercise(exercise).stream().map(ExamOverview::new).collect(Collectors.toList());

        if (isOnlyStudent && accessChecker.getAuthentication() != null) {
            String studentId = accessChecker.getAuthentication().getStudentid();
            if (studentId != null) {
                Map<String, Attendance> attendances = attendanceDao.getAttendanceForStudentAndExercise(exercise, studentId)
                        .stream().collect(Collectors.toMap(Attendance::getSheet, Function.identity()));
                Map<String, Float> points = resultsDao.getResultsForStudentAndExercise(studentId, exercise);
                Map<String, Float> deltaPoints = deltapointsDao.getDeltapointsForStudent(exercise, studentId);
                Map<String, Float> maxPoints = assignmentDao.getMaxPoints(exercise);

                 admissionMessage = admissionDao.getAdmissionMessage(exercise, studentId);

                 List<String> sheetsWithUnreadAnnotations = annotationDao.getUnreadSheets(studentId, exercise);

                for (SheetResult sheet : sheets) {
                    float delta = deltaPoints.getOrDefault(sheet.sheet.getId(), 0f);
                    sheet.maxPoints = maxPoints.getOrDefault(sheet.sheet.getId(), 0f);
                    if (points.containsKey(sheet.sheet.getId()) || delta != 0) {
                        sheet.points = points.getOrDefault(sheet.sheet.getId(), 0f) + delta;
                    }
                    if (attendances.containsKey(sheet.sheet.getId())) {
                        sheet.attended = attendances.get(sheet.sheet.getId()).isAttended();
                    }
                    if (sheetsWithUnreadAnnotations.contains(sheet.sheet.getId())) {
                        sheet.unreadAnnotations = true;
                    }
                }

                StudentInfo studentInfo = userDao.getStudentInfo(exercise, studentId);
                if (studentInfo != null) {
                    team = studentInfo.getTeam();
                    teamMembers = userDao.getStudentsInTeam(exercise, team);
                }
                List<String> participatingExams = examDao.getParticipatingExams(exercise, studentId);
                for (ExamOverview exam : exams) {
                    if (participatingExams.contains(exam.exam.getId())) {
                        exam.isRegistered = true;
                    }
                }
            }
        }

        double totalPoints = sheets.stream()
                .filter(s -> s.points != null)
                .mapToDouble(s -> s.points)
                .sum();
        double totalMaxPointsGraded = sheets.stream()
                .filter(s -> s.points != null)
                .mapToDouble(s -> s.maxPoints)
                .sum();
        double totalMaxPoints = sheets.stream()
                .mapToDouble(s -> s.maxPoints)
                .sum();
        long totalAbsent = sheets.stream()
                .filter(s -> s.attended != null && !s.attended)
                .count();

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheets", sheets);
        model.addAttribute("isOnlyStudent", isOnlyStudent);
        model.addAttribute("capAssess", accessChecker.hasAssessRight(exercise));
        model.addAttribute("capUpload", accessChecker.hasUploadRight(exercise));
        model.addAttribute("capAdmin", accessChecker.hasAdminRight(exercise));
        model.addAttribute("admissionMessage", admissionMessage);
        model.addAttribute("team", team);
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("exams", exams);
        model.addAttribute("totalPoints", totalPoints);
        model.addAttribute("totalMaxPointsGraded", totalMaxPointsGraded);
        model.addAttribute("totalMaxPoints", totalMaxPoints);
        model.addAttribute("totalUngraded", totalMaxPoints - totalMaxPointsGraded);
        model.addAttribute("totalAbsent", totalAbsent);
        return "exercise";
    }

    @GetMapping("/exercise/{exid}/results")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getResults(@PathVariable("exid") String exercise, Model model) {
        metrics.registerAccessExercise();
        List<Team> teams = securityTools.getAccessibleTeams(exercise);
        List<String> groups = teams.stream()
                .map(Team::getGroup)
                .distinct()
                .sorted(Comparator.nullsFirst(Comparator.naturalOrder()))
                .collect(Collectors.toList());
        List<StudentInfo> students = new ArrayList<>();

        // get a mailto link for groups and teams
        Map<String, String> groupEmail = new HashMap<>();
        Map<Team, String> teamEmail = new HashMap<>();

        for (String group : groups) {
            List<StudentInfo> studentsInGroup =  userDao.getStudentsInGroup(exercise, group);

            logger.debug("Got {} students for group {}", studentsInGroup.size(), group);
            studentsInGroup.sort(FIRST_LAST_IGNORE_CASE);
            students.addAll(studentsInGroup);

            groupEmail.put(group, FormatUtils.formatEmail(accessChecker.getEmail(), studentsInGroup));
            studentsInGroup.stream().collect(Collectors.groupingBy(StudentInfo::getTeam))
                    .forEach((team, studentInfos) -> teamEmail.put(team, FormatUtils.formatEmail(accessChecker.getEmail(), studentInfos)));

        }

        //key: studentid, value: totalAttendance
        Map<String, Long> totalAttendance = new HashMap<>();
        //key: studentid, value: (key: sheetid, value: attended)
        Map<String, Map<String, Boolean>> studentAttendance = new HashMap<>();
        for (StudentInfo student : students) {
            Map<String, Boolean> attendance = attendanceDao.getAttendanceForStudentAndExercise(exercise, student.getId())
                    .stream().collect(Collectors.toMap(Attendance::getSheet, Attendance::isAttended));
            studentAttendance.put(student.getId(), attendance);
            totalAttendance.put(student.getId(), attendance.values().stream().filter(x -> x).count());
        }

        Map<String, List<APIResult>> resultsBySheet = resultsDao.getResultsForExercise(exercise);
        Map<String, Double> maxpointsBySheet = sheetDao.getMaxPointsForExercise(exercise);

        List<APIExerciseResult> res = resultsBySheet.entrySet().stream()
                .map(rbs -> new APIExerciseResult(rbs.getKey(), maxpointsBySheet.get(rbs.getKey()), rbs.getValue()))
                .collect(Collectors.toList());

        List<Sheet> sheets = sheetDao.getSheetsForExercise(exercise);
        Map<String, List<APIResult>> results = resultsDao.getResultsForExercise(exercise);
        //Map with key:sheetid, value: max points for sheet
        Map<String, Float> pointsForSheet = assignmentDao.getMaxPoints(exercise);

        // studentsId -> sheetId -> points
        Map<String, Map<String, Double>> pointsForStudentAndSheet = new TreeMap<>();
        for (StudentInfo s : students) {
            pointsForStudentAndSheet.put(s.getId(), new TreeMap<>());
        }
        for (Map.Entry<String, List<APIResult>> e1 : resultsBySheet.entrySet()) {
            String sheet = e1.getKey();
            for (APIResult result : e1.getValue()) {
                Map<String, Double> r = pointsForStudentAndSheet.computeIfAbsent(result.getStudentid(), k -> new TreeMap<>());
                r.put(sheet, result.getPoints());
            }
        }
        // studentid -> overall points
        Map<String, Double> pointsForStudent =
                pointsForStudentAndSheet.keySet().stream()
                        .collect(Collectors.toMap(s -> s, s ->
                                pointsForStudentAndSheet.get(s).values().stream()
                                        .mapToDouble(points -> points)
                                        .sum()
                        ));


        students.sort(Comparator.comparing(StudentInfo::getTeam));
        double totalPoints = maxpointsBySheet.values().stream().mapToDouble(f -> f).sum();

        List<String> exerciseGroups = groupDao.getGroupsForExercise(exercise).stream().map(Group::getGroupId).collect(Collectors.toList());
        exerciseGroups.add("-");

        model.addAttribute("sheets", sheets);
        model.addAttribute("pointsForStudentAndSheet", pointsForStudentAndSheet);
        model.addAttribute("pointsForStudent", pointsForStudent);
        model.addAttribute("pointsForSheet", pointsForSheet);
        model.addAttribute("totalPoints", totalPoints);
        model.addAttribute("exercise", exercise);
        model.addAttribute("students", students);
        model.addAttribute("totalAttendance", totalAttendance);
        model.addAttribute("studentAttendance", studentAttendance);
        model.addAttribute("groupEmail", groupEmail);
        model.addAttribute("teamEmail", teamEmail);
        model.addAttribute("exerciseGroups", exerciseGroups);
        model.addAttribute("isAssistant", accessChecker.hasAdminRight(exercise));
        return "results-overview";
    }

    @GetMapping("/exercise/{exid}/change-teams")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String changeTeamsPage(@PathVariable("exid") String exercise, Model model) {
        List<Team> teams = securityTools.getAccessibleTeams(exercise);
        List<String> groups = teams.stream()
                .map(Team::getGroup)
                .distinct()
                .collect(Collectors.toList());
        List<StudentInfo> students = new ArrayList<>();

        for (String group : groups) {
            List<StudentInfo> studentsInGroup =  userDao.getStudentsInGroup(exercise, group);
            studentsInGroup.sort(FIRST_LAST_IGNORE_CASE);
            students.addAll(studentsInGroup);
        }

        students.sort(
                Comparator.comparing(StudentInfo::getTeam)
                        .thenComparing(StudentInfo::getLastname)
                        .thenComparing(StudentInfo::getFirstname)
        );

        model.addAttribute("exercise", exercise);
        model.addAttribute("students", students);
        return "change-teams";
    }

    @PostMapping("/exercise/{exid}/change-teams/submit")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String postChangeTeams(@PathVariable("exid") String exercise, RedirectAttributes redirectAttributes, @RequestParam List<String> team, @RequestParam List<String> studentid) {


        Preconditions.checkArgument(team.size() == studentid.size());
        for (int i = 0; i < studentid.size(); i++) {
            String s = studentid.get(i);
            String t = team.get(i);
            changeStudentGroupAndTeam(exercise, Optional.empty(), t, s, redirectAttributes);
        }
        return "redirect:/exercise/{exid}/change-teams";
    }


    @PostMapping("/exercise/{exid}/results/editUser")
    @PreAuthorize("@accessChecker.hasAssessRight(#eid)")
    public String postEditUser(@PathVariable("exid") String eid,
                                @RequestParam(name = "group", required = false) String groupid,
                                @RequestParam("team") String team,
                                @RequestParam("studentid") String studentid,
                                RedirectAttributes redirectAttributes) {
        metrics.registerAccessExercise();

        changeStudentGroupAndTeam(eid, Optional.ofNullable(groupid), team, studentid, redirectAttributes);
        return "redirect:/exercise/{exid}/results";
    }

    private void changeStudentGroupAndTeam(String eid, Optional<String> groupidOpt, String team, String studentid, RedirectAttributes redirectAttributes) {
        Exercise exercise = exerciseDao.getExercise(eid);
        User user = userDao.getUserByStudentId(studentid);

        List<ExerciseRights> exerciseRights = userDao.getUserRightsForExercise(user.getUserid(), eid);
        ExerciseRights exerciseRight;
        if (exerciseRights.size() > 1) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Nutzer " + user + " ist kein Student."));
            return;
        } else if (exerciseRights.size() == 0) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Student " + user +" ist nicht für die Übung registriert."));
            return;
        } else {
            exerciseRight = exerciseRights.get(0);
        }



        if (exercise == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Fehler beim Laden der Übung."));
            return;
        } else if (exerciseRight.getRole() != ExerciseRights.Role.student) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Nutzer ist kein Student."));
            return;
        }

        if (groupidOpt.isPresent()) {
            String groupid = groupidOpt.get();
            Group group = groupDao.getGroup(eid, groupid);
            if (group == null && !groupid.equals("-")) {
                redirectAttributes.addFlashAttribute("errors",
                        singletonList("Gruppe + " + group + " konnte nicht gefunden werden."));
                return;
            }
            if (accessChecker.hasAdminRight(eid)) {
                if (groupid.equals("-")) {
                    exerciseRight.setGroupId(null);
                } else {
                    exerciseRight.setGroupId(group.getGroupId());
                }
            }
        }

        if (accessChecker.hasAssessRight(eid, exerciseRight.getGroupId())) {
            if (team == null || team.equals("")) {
                exerciseRight.setTeamId(null);
            } else {
                exerciseRight.setTeamId(team);
            }
        }

        userDao.updateUserRights(user.getUserid(), exerciseRight);
        accessChecker.expireUserSession(user.getUsername());
    }

    @GetMapping("/exercise/{exid}/results/deleteUser/{sid}")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String postDeleteUser(@PathVariable("exid") String eid,
                                 @PathVariable("sid") String studentid,
                                RedirectAttributes redirectAttributes) {
        metrics.registerAccessExercise();

        Exercise exercise = exerciseDao.getExercise(eid);
        User user = userDao.getUserByStudentId(studentid);
        List<ExerciseRights> exerciseRights = userDao.getUserRightsForExercise(user.getUserid(), eid);
        ExerciseRights exerciseRight;
        if (exerciseRights.size() > 1) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Nutzer ist kein Student."));
            return "redirect:/exercise/{exid}/results";
        } else if (exerciseRights.size() == 0) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Student ist nicht für die Übung registriert."));
            return "redirect:/exercise/{exid}/results";
        } else {
            exerciseRight = exerciseRights.get(0);
        }

        if (exercise == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Fehler beim Laden der Übung."));
        } else if (exerciseRight.getRole() != ExerciseRights.Role.student) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Nutzer ist kein Student."));
        } else {
            userDao.deleteUserRights(user.getUserid(), eid);
            accessChecker.expireUserSession(user.getUsername());
        }
        return "redirect:/exercise/{exid}/results";
    }

    @GetMapping("/exercise/{exid}/admission")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getAdmissions(@PathVariable("exid") String exercise, Model model) {
        metrics.registerAccessExercise();
        List<Admission> admissions = admissionDao.getAdmissions(exercise);

        model.addAttribute("exercise", exercise);
        model.addAttribute("admissions", admissions);
        return "admission";
    }

    @PostMapping("/exercise/{exid}/admission")
    @ResponseBody
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postAdmissions(
            @PathVariable("exid") String exercise,
            @RequestParam("file") MultipartFile file
    ) {
        metrics.registerAccessExercise();

        try(InputStream stream = file.getInputStream();
            Reader reader = new InputStreamReader(stream)) {
            List<Admission> admissions = new CsvToBeanBuilder<Admission>(reader).withType(Admission.class).withSeparator(csvSeparator).build().parse();
            admissions = admissions.stream().filter(admission -> admission.getStudentId() != null).collect(Collectors.toList());
            admissions.forEach(admission -> admission.setExercise(exercise));
            admissionDao.createOrUpdateAdmissions(admissions.stream().filter(admission -> admission.getMessage() != null).collect(Collectors.toList()));
            //delete admission if message is null
            admissions.stream().filter(admission -> admission.getMessage() == null).forEach(admissionDao::deleteAdmission);
        } catch (IOException e) {
            throw new UploadException("Error reading file.", e);
        }

        return "Zulassungen geupdatet";
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/attendance")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getAttendancePage(@PathVariable("exid") String exercise,
                                    @PathVariable("sid") String sheet,
                                    Model model) {
        metrics.registerAccessExercise();
        Map<String, List<StudentInfo>> studentsByGroup = securityTools.getAccessibleStudentsByGroup(exercise, sheet);
        studentsByGroup.replaceAll((k, v) -> {
            v.sort(FIRST_LAST_IGNORE_CASE);
            return v;
        });
        List<String> groups = studentsByGroup.keySet().stream().sorted().collect(Collectors.toList());
        Map<String, Attendance> attendanceMap = attendanceDao.getAttendanceForSession(exercise, sheet).stream()
                .collect(Collectors.toMap(Attendance::getStudentid, Function.identity()));
        Map<String, Boolean> attendance = studentsByGroup.entrySet().stream()
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .map(StudentInfo::getId)
                .collect(Collectors.toMap(
                        Function.identity(),
                        sid -> attendanceMap.containsKey(sid) && attendanceMap.get(sid).isAttended()
                ));

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("groups", groups);
        model.addAttribute("students", studentsByGroup);
        model.addAttribute("attendance", attendance);
        model.addAttribute("isAssistant", accessChecker.hasAdminRight(exercise));
        return "attendance";
    }

    //Controller for SimilarityChecker
    @GetMapping("/exercise/{exid}/sheet/{sid}/similarityScore")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getSimilarityCheckerPage(@PathVariable("exid") String exercise,
                                    @PathVariable("sid") String sheet,
                                    Model model) {
        metrics.registerAccessExercise();

        //get all the results of the similarityCheck
        ArrayList<SimilarityScore[]> scores = SimilarityCheckerDao.getScores(exercise, sheet);

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("isAssistant", accessChecker.hasAdminRight(exercise));
        model.addAttribute("scores", scores);
        return "similarityScore";
    }

    //Controller for SimilarityChecker one assignment view
    @GetMapping("/exercise/{exid}/sheet/{sid}/similarityScoreForAssignment/{aid}")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getSimilarityCheckerAssignmentPage(@PathVariable("exid") String exercise,
                                           @PathVariable("sid") String sheet,
                                            @PathVariable("aid") String assignment,
                                           Model model) {
        metrics.registerAccessExercise();

        //get all the results of the similarityCheck
        SimilarityScore[] scores = SimilarityCheckerDao.getScoresForAssignment(exercise, sheet, assignment);

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("scores", scores);
        model.addAttribute("assignment", assignment);
        return "similarityScoresForAssignment";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/attendance")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    public String postAttendance(@PathVariable("exid") String exercise,
                                 @PathVariable("sid") String sheet,
                                 @RequestParam("group") String groupid,
                                 @RequestBody MultiValueMap<String, String> formParams) {
        metrics.registerAccessExercise();
        formParams.entrySet().stream()
                .filter(e -> e.getKey().startsWith("stud-"))
                .map(e -> {
                    String studentid = e.getKey().substring(5);
                    boolean attended = evalToBool(e.getValue());
                    return new Attendance(sheet, exercise, studentid, attended);
                })
                .forEach(a -> attendanceDao.createOrUpdateAttendance(a));
        return "redirect:/exercise/{exid}/sheet/{sid}/attendance";
    }

    private boolean evalToBool(List<String> multival) {
        return multival.contains("true");
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/upload")
    @ResponseBody
    @PreAuthorize("@accessChecker.hasUploadRight(#exercise, #group, #team)")
    public ResponseEntity<String> postUploadFile(
            @PathVariable("exid") String exercise,
            @PathVariable("sid") String sheet,
            @RequestParam("group") String group,
            @RequestParam("team") String team,
            @RequestParam("assignment") String assignment,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Keine Datei angegeben");
        }

        metrics.registerAccessUpload();
        Team t = new Team(group, team);
        String uploaderStudentid = accessChecker.getAuthentication().getStudentid();
        uploadManager.putUpload(exercise, sheet, assignment, t, file, uploaderStudentid);
        metrics.registerUploadSize( file.getSize());
        return ResponseEntity.ok("Datei hochgeladen");
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/deleteUpload")
    @ResponseBody
    @PreAuthorize("@accessChecker.hasUploadRight(#exercise, #group, #team)")
    public String postDeleteFile(
            @PathVariable("exid") String exercise,
            @PathVariable("sid") String sheet,
            @RequestParam("group") String group,
            @RequestParam("team") String team,
            @RequestParam("assignment") String assignment,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam("uploaddate") String uploaddate
    ) {
        // fix for deleting files with empty name
        if (filename == null) {
            filename = "";
        }
        metrics.registerAccessUpload();
        String studentId = accessChecker.getAuthentication().getStudentid();
        Upload upload = new Upload();
        upload.setDeleteDate(DateTime.now().withMillisOfSecond(0));
        upload.setDeleterStudentid(studentId);
        upload.setExercise(exercise);
        upload.setSheet(sheet);
        upload.setAssignment(assignment);
        upload.setTeam(new Team(group, team));
        upload.setFilename(filename);
        upload.setUploadDate(DateTime.parse(uploaddate));

        uploadsDao.markDeleted(upload);
        return "Datei gelöscht";
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/overview")
    @PreAuthorize("@accessChecker.canAccess(#exercise)")
    public String getOverview(@PathVariable("exid") String exercise,
                              @PathVariable("sid") String sheet,
                              Model model) {
        metrics.registerAccessOverview();

        Map<Team, List<StudentInfo>> students = securityTools.getAccessibleStudentsByTeam(exercise, sheet);
        List<Team> accessibleTeams = students.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());


        Map<AssignmentAndTeam, Testresult> testResults;
        if (accessChecker.isOnlyStudent(exercise)) {
            Team studentTeam = securityTools.getStudentTeam(exercise, sheet);
            testResults = testresultDao.getLatestTestresults(exercise, sheet, studentTeam);
        } else {
            testResults = testresultDao.getLatestTestresults(exercise, sheet, null);
        }

        Map<AssignmentAndTeam, Result> results = resultsDao.getResultsForSheet(exercise, sheet).stream()
                .collect(Collectors.toMap(Result::getAssignmentAndTeam,
                        Function.identity()));
        List<Assignment> assignments = assignmentDao.getAllAssignments(exercise, sheet);
        Map<String, Deltapoints> deltaPoints = deltapointsDao.getAllDeltapoints(exercise, sheet).stream()
                .collect(Collectors.toMap(Deltapoints::getStudentid, Function.identity()));

        Map<Team, Map<String, List<Upload>>> uploads = uploadsDao.getUploadsForSheet(exercise, sheet).stream()
                .collect(Collectors.groupingBy(Upload::getTeam,
                        Collectors.groupingBy(Upload::getAssignment)));

        Map<Team, Comment> comments = commentsDao.getAllComments(exercise, sheet).stream()
                .collect(Collectors.toMap(Comment::getTeam, Function.identity()));


        List<OverviewDataForTeam> overviewData = new ArrayList<>();
        for (Team team : accessibleTeams) {
            OverviewDataForTeam teamData = new OverviewDataForTeam();
            teamData.team = team;
            teamData.exercise = exercise;
            teamData.sheet = sheet;

            teamData.comment = comments.get(team);

            Map<String, List<Upload>> teamUploads = uploads.getOrDefault(team, Collections.emptyMap());

            teamData.assignments = new ArrayList<>();
            for (Assignment assignment : assignments) {
                AssignmentAndTeam assignmentAndTeam = new AssignmentAndTeam(assignment.getId(), team);
                OverviewAssignmentData a = new OverviewAssignmentData();
                a.assignment = assignment;
                Result res = results.get(assignmentAndTeam);
                if (res != null) {
                    a.points = res.getPoints();
                }
                a.testresult = testResults.get(assignmentAndTeam);

                ///20190113
                ///Added testResultDetails field in OverviewAssignmentData class and initialize it here
                ///to show test info in overview page with assignments and uploads details
                try {
                    a.testResultDetails = TestResultDetails.fromJson(a.testresult.getResult());
                } catch (Exception e) {
                    // ignore
                }

                a.testExists = rteServices.isTestAvailable(new TestName(exercise, sheet, assignment.getId()));

                a.addUploads(teamUploads.getOrDefault(assignment.getId(), emptyList()));
                a.snapshot = UploadsDao.latestSnapshotFromUploads(a.getUploads()).orElseGet(() -> DateTime.now());
                teamData.assignments.add(a);
                a.addAnnotations(getAccessibleAnnotations(exercise,sheet,team));
                Map<Integer, List<FileWarnings.Warning>> fileWarnings = warningsDao.getWarningsForSheet(exercise, sheet, team);
                fileWarnings.forEach((k,v) -> {
                    a.addWarnings(v,k);
                });
                if (accessChecker.isOnlyStudent(exercise)) {
                    a.markUnread(annotationDao.getUnreadUploads(accessChecker.getAuthentication().getStudentid(), exercise, sheet));
                }
            }

            teamData.students = new ArrayList<>();
            for (StudentInfo stud : students.get(team)) {
                OverviewStudentData s = new OverviewStudentData();
                s.student = stud;
                Deltapoints delta = deltaPoints.get(stud.getId());
                if (delta != null) {
                    s.delta = delta.getDelta();
                    s.reason = delta.getReason();
                }
                teamData.students.add(s);
            }


            overviewData.add(teamData);
        }

        model.addAttribute("overviewData", overviewData);
        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        return "overview";
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/{aid}/{gid}/{tid}/fragment")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public String getOverviewFragment(@PathVariable("exid") String exercise,
                                      @PathVariable("sid") String sheet,
                                      @PathVariable("aid") String assignmentid,
                                      @PathVariable("gid") String groupid,
                                      @PathVariable("tid") String teamid,
                                      Model model) {
        Team team = new Team(groupid, teamid);
        OverviewDataForTeam data = new OverviewDataForTeam();
        data.team = team;
        data.exercise = exercise;
        data.sheet = sheet;

        data.students = new ArrayList<>();
        List<StudentInfo> students = securityTools.getTeamMembers(exercise, sheet, team);
        for (StudentInfo stud : students) {
            OverviewStudentData s = new OverviewStudentData();
            s.student = stud;
            data.students.add(s);
        }

        Assignment assignment = assignmentDao.getAssignment(exercise, sheet, assignmentid);
        OverviewAssignmentData a = new OverviewAssignmentData();
        a.assignment = assignment;
        Result res = resultsDao.getResult(exercise, sheet, assignmentid, team);
        if (res != null) {
            a.points = res.getPoints();
        }
        a.annotationCount = 0; // TODO get annotation count and fix the original overview page
        a.testresult = testresultDao.getLatestTestresult(exercise, sheet, assignmentid, team);
        if (a.testresult != null) {
            try {
                a.testResultDetails = TestResultDetails.fromJson(a.testresult.getResult());
            } catch (Exception e) {
                // ignore
            }
        }

        a.testExists = rteServices.isTestAvailable(new TestName(exercise, sheet, assignment.getId()));

        a.addUploads(uploadsDao.getUploadsForAssignmentAndTeam(exercise, sheet, assignmentid, team));
        a.snapshot = UploadsDao.latestSnapshotFromUploads(a.getUploads()).orElseGet(() -> DateTime.now());

        a.addAnnotations(getAccessibleAnnotations(exercise,sheet,team));
        Map<Integer, List<FileWarnings.Warning>> fileWarnings = warningsDao.getWarningsForSheet(exercise, sheet, team);
        fileWarnings.forEach((k,v) -> {
            a.addWarnings(v,k);
        });
        if (accessChecker.isOnlyStudent(exercise)) {
            a.markUnread(annotationDao.getUnreadUploads(accessChecker.getAuthentication().getStudentid(), exercise, sheet));
        }

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("data", data);
        model.addAttribute("assign", a);

        return "overview-assign-frag";
    }


    @GetMapping("/exercise/{exid}/sheet/{sid}/assessment")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getAssessmentPage(@PathVariable("exid") String exercise,
                                    @PathVariable("sid") String sheet,
                                    Model model) {
        metrics.registerAccessAssessment();
        List<Assignment> assignments = assignmentDao.getAllAssignments(exercise, sheet);
        assignments.sort(Comparator.comparing(Assignment::getLabel));

        Map<Team, List<StudentInfo>> accessibleStudents = securityTools.getAccessibleStudentsByTeam(exercise, sheet);
        List<Team> accessibleTeams = accessibleStudents.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        List<Team> teams;
        if (accessChecker.hasAdminRight(exercise)) {
            teams = accessibleTeams;
        } else {
            teams = accessibleTeams.stream()
                    .filter(t -> accessChecker.hasAssessRight(exercise, t.getGroup()))
                    .collect(Collectors.toList());
        }

        Map<Team, Map<String, List<Result>>> allTeamsResults =
                resultsDao.getResultsForListOfTeams(exercise, sheet, teams)
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getTeam(), Collectors.groupingBy(r -> r.getAssignment())));
        Map<Team, Map<String, List<Upload>>> allTeamsTotalUploads =
                uploadsDao.getTotalUploadsForListOfTeams(exercise, sheet, teams)
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getTeam(), Collectors.groupingBy(r -> r.getAssignment())));
        List<Testresult> allTestResults = testresultDao.getTestresultsForListOfTeams(exercise, sheet, teams);
        long totalCompiled = allTestResults.stream().filter(result -> result.isCompiled()).count();
        long totalNotCompiled = allTestResults.stream().filter(result -> !result.isCompiled()).count();
        long totalTestsPassed = allTestResults.stream().mapToInt(result -> result.getTestsPassed()).sum();
        long totalTests = allTestResults.stream().mapToInt(result -> result.getTestsTotal()).sum();
        Map<Team, Map<String, List<Testresult>>> allTeamsTestresults =
                allTestResults
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getTeam(), Collectors.groupingBy(r -> r.getAssignment())));
        ///Counting statistics section
        long compiledPortion;
        long notCompiledPortion;
        if (totalCompiled != 0) {
            compiledPortion = Math.round(((double) totalCompiled / (totalCompiled + totalNotCompiled)) * 100.0);
            notCompiledPortion = 100 - compiledPortion;
        } else {
            compiledPortion = 0;
            notCompiledPortion = 100;
        }

        long testsPassedPortion;
        long testsNotPassedPortion;
        if (totalTests != 0) {
            testsPassedPortion = Math.round(((double) totalTestsPassed / totalTests) * 100.0);
            testsNotPassedPortion = 100 - testsPassedPortion;
        } else {
            testsPassedPortion = 0;
            testsNotPassedPortion = 0;
        }

        Map<Team, Boolean> commentHidden = commentsDao.getAllComments(exercise, sheet).stream()
                .collect(Collectors.groupingBy(Comment::getTeam, Collectors.reducing(false,Comment::isHidden, (a,b) -> a || b)));

        model.addAttribute("compiledPortion", compiledPortion + "%");
        model.addAttribute("notCompiledPortion", notCompiledPortion + "%");
        model.addAttribute("testsPassedPortion", testsPassedPortion + "%");
        model.addAttribute("testsNotPassedPortion", testsNotPassedPortion + "%");
        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("assignments", assignments);
        model.addAttribute("teams", teams);
//        model.addAttribute("results", results);
        model.addAttribute("allTeamsResults", allTeamsResults);
        model.addAttribute("allTeamsTotalUploads", allTeamsTotalUploads);
        model.addAttribute("allTeamsTestresults", allTeamsTestresults);
        model.addAttribute("commentHidden", commentHidden);
        return "assessment";
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/assessment/{group}/{team}")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    public String getAssessmentForTeam(@PathVariable("exid") String exercise,
                                       @PathVariable("sid") String sheet,
                                       @PathVariable("group") String groupid,
                                       @PathVariable("team") String teamid,
                                       Model model) {
        metrics.registerAccessAssessment();
        Team team = new Team(groupid, teamid);
        List<StudentInfo> studentsInTeam = securityTools.getTeamMembers(exercise, sheet, team);
        List<Map<String, Object>> studentsWithDeltas = studentsInTeam.stream()
                .map(stud -> {
                    Deltapoints deltapoints = deltapointsDao.getDeltapointsForStudent(exercise, sheet, stud.getId());
                    Map<String, Object> studentWithDeltas = new HashMap<>();
                    studentWithDeltas.put("id", stud.getId());
                    studentWithDeltas.put("firstname", stud.getFirstname());
                    studentWithDeltas.put("lastname", stud.getLastname());
                    studentWithDeltas.put("delta", deltapoints == null ? 0 : deltapoints.getDelta());
                    studentWithDeltas.put("comment", deltapoints == null ? "" : deltapoints.getReason());
                    return studentWithDeltas;
                })
                .collect(Collectors.toList());

        List<Assignment> assignments = assignmentDao.getAllAssignments(exercise, sheet);
        List<Result> resultsForTeam = resultsDao.getResultsForTeam(exercise, sheet, team);
        List<Testresult> testresultsForTeam = testresultDao.getLatestTestresultsForTeam(exercise, sheet, team);
        Map<String, List<Upload>> uploadsByAssignment = uploadsDao.getUploadsForSheetAndTeam(exercise, sheet, team).stream().collect(Collectors.groupingBy(Upload::getAssignment));
        List<Map<String, Object>> assignmentsWithResults = assignments.stream()
                .map(assignment -> {
                    Map<String, Object> assign = new HashMap<>();
                    assign.put("id", assignment.getId());
                    assign.put("label", assignment.getLabel());
                    assign.put("maxpoints", assignment.getMaxpoints());
                    assign.put("points", resultsForTeam.stream().filter(result -> assignment.getId().equals(result.getAssignment())).findAny().map(Result::getPoints).orElse(0.0f));
                    assign.put("testsPassed", testresultsForTeam.stream().filter(testresult -> assignment.getId().equals(testresult.getAssignment())).findAny().map(Testresult::getTestsPassed).orElse(0));
                    assign.put("testsTotal", testresultsForTeam.stream().filter(testresult -> assignment.getId().equals(testresult.getAssignment())).findAny().map(Testresult::getTestsTotal).orElse(0));
                    assign.put("snapshot", UploadsDao.latestSnapshotFromUploads(uploadsByAssignment.getOrDefault(assignment.getId(), new ArrayList<>())).orElseGet(DateTime::now));
                    return assign;
                })
                .collect(Collectors.toList());
        Comment comment = commentsDao.getComment(exercise, sheet, team);
        String commentRaw;
        if (comment == null) {
            commentRaw = "";
        } else {
            commentRaw = comment.getComment();
        }


        String commentHtml = Markdown.toHtml(commentRaw);

        List<String> feedbackUploads = uploadManager.getFeedbackUploads(exercise, sheet, team);

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("team", team);
        model.addAttribute("students", studentsWithDeltas);
        model.addAttribute("assignments", assignmentsWithResults);
        model.addAttribute("comment", commentRaw);
        model.addAttribute("hidden", comment != null && comment.isHidden());
        model.addAttribute("comment_html", commentHtml);
        model.addAttribute("feedbackuploads", feedbackUploads);
        model.addAttribute("isAssistant", accessChecker.hasAdminRight(exercise));
        return "assessment-team";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/result")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    public String postResult(@PathVariable("exid") String exercise,
                             @PathVariable("sid") String sheet,
                             @RequestParam("group") String groupid,
                             @RequestParam("team") String teamid,
                             @RequestParam("comment") String comment,
                             @RequestParam(value = "hidden", defaultValue = "false") boolean hidden,
                             @RequestBody MultiValueMap<String, String> formData,
                             RedirectAttributes redirectAttributes) {
        Map<String, Object> logInfo = new LinkedHashMap<>();
        logInfo.put("exercise", exercise);
        logInfo.put("sheet", sheet);
        logInfo.put("group", groupid);
        logInfo.put("team", teamid);
        logInfo.put("comment", comment);
        logInfo.put("tutor", accessChecker.getAuthentication().getName());
        logInfo.put("formData", formData);
        logger.info("Update student results {}", Json.toJson(logInfo));
        metrics.registerAccessAssessment();
        Team team = new Team(groupid, teamid);
        List<Assignment> assignments = assignmentDao.getAllAssignments(exercise, sheet);
        List<StudentInfo> studentsInTeam = securityTools.getTeamMembers(exercise, sheet, team);

        Comment commentTO = new Comment(sheet, exercise, team, comment, hidden);
        commentsDao.createOrUpdateComment(commentTO);

        List<String> errors = new LinkedList<>();
        // get data from the form
        List<AssignmentWithPoints> assignmentWithPoints = assignments.stream()
                .map(a -> new AssignmentWithPoints(a, formData.getFirst("asgn-" + a.getId())))
                .filter(a -> a.pointsValid())
                .map(a -> {
                    try {
                        float points = Float.parseFloat(a.pointsRaw);
                        return a.withPoints(points);
                    } catch (NumberFormatException e) {
                        errors.add("Keine gültige Zahl: " + a.pointsRaw);
                        return a;
                    }
                })
                .collect(Collectors.toList());


        Map<String, String> deltasByStudent = studentsInTeam.stream()
                .collect(Collectors.toMap(StudentInfo::getId, s -> formData.getFirst("delta-" + s.getId())));

        Map<String, String> commentsByStudent = studentsInTeam.stream()
                .collect(Collectors.toMap(StudentInfo::getId, s -> formData.getFirst("comment-" + s.getId())));

        // check validity of input
        double assignmentSum = assignmentWithPoints.stream()
                .mapToDouble(a -> a.points)
                .sum();

        double assignmentMaxPoints = assignmentWithPoints.stream()
                .mapToDouble(a -> a.assignment.getMaxpoints())
                .sum();

        // check that deltas do not lead to negative points
        try {
            boolean validDeltas = deltasByStudent.values().stream()
                    .mapToDouble(v -> v != null ? Double.parseDouble(v) : 0.0)
                    .allMatch(v -> v + assignmentSum >= 0 && v + assignmentSum <= assignmentMaxPoints);
            if (!validDeltas)
                errors.add("Delta Punkte dürfen nicht zu negativen Werten oder zu Werten über der maximalen Punktzahl führen.");

            // check that points are not negative and not greater than max points of assignment
            assignmentWithPoints
                    .forEach(a -> {
                        if (a.points < 0)
                            errors.add("Punkte dürfen nicht negativ sein: " + a.assignment.getLabel());
                        else if (a.points > a.assignment.getMaxpoints())
                            errors.add("Punkte dürfen nicht größer der maximalen Punktzahl sein: " + a.assignment.getLabel());
                    });

        } catch (NumberFormatException e) {
            errors.add("Falsche Eingabe bei Delta Punkten: Keine Zahl.");
        }

        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", errors);
            redirectAttributes.addAttribute("gid", groupid);
            redirectAttributes.addAttribute("tid", teamid);
            return "redirect:/exercise/{exid}/sheet/{sid}/assessment/{gid}/{tid}";
        } else {
            for (StudentInfo student : studentsInTeam) {
                deltapointsDao.createOrUpdateDeltapoints(new Deltapoints(sheet, exercise, student.getId(),
                        Float.parseFloat(deltasByStudent.getOrDefault(student.getId(), "0.0")),
                        commentsByStudent.get(student.getId())));
                try {
                    resultsDao.createStudentres(new Studentres(sheet, exercise, student.getId(), team));
                } catch (DataIntegrityViolationException e) {
                    // ignore, we already have a team-binding for this student
                }
            }
            for (AssignmentWithPoints assignment : assignmentWithPoints) {
                resultsDao.createOrUpdateResult(new Result(assignment.assignment.getId(), sheet, exercise, team, assignment.points));
                logger.info("added result for {}, sheet {}, team {}, assignment {}, points {}.  (user {})",
                        exercise, sheet, team, assignment.assignment.getId(), assignment.points, accessChecker.getAuthentication().getName());
            }
        }

        // all done and well, redirect depending on clicked button
        if (formData.containsKey("save-continue")) {
            List<Team> allTeams = securityTools.getAccessibleTeams(exercise)
                    .stream()
                    .filter(t -> accessChecker.hasAssessRight(exercise, t.getGroup()))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            int currentPosition = allTeams.indexOf(team);
            Team nextTeam = allTeams.get((currentPosition + 1) % allTeams.size());
            redirectAttributes.addAttribute("gid", nextTeam.getGroup());
            redirectAttributes.addAttribute("tid", nextTeam.getTeam());
            return "redirect:/exercise/{exid}/sheet/{sid}/assessment/{gid}/{tid}";
        } else if (formData.containsKey("save")) {
            redirectAttributes.addAttribute("gid", groupid);
            redirectAttributes.addAttribute("tid", teamid);
            return "redirect:/exercise/{exid}/sheet/{sid}/assessment/{gid}/{tid}";
        } else {
            return "redirect:/exercise/{exid}/sheet/{sid}/assessment";
        }
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/admin")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getSheetAdminPage(@PathVariable("exid") String exercise,
                               @PathVariable("sid") String sheet,
                               Model model) {
        metrics.registerAccessAdmin();
        List<Assignment> assignments = assignmentDao.getAllAssignments(exercise, sheet);
        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("assignments", assignments);
        return "sheet-admin";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/assignment")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postCreateAssignment(@PathVariable("exid") String exercise,
                                       @PathVariable("sid") String sheet,
                                       @RequestParam("id") String newId,
                                       @RequestParam("label") String label,
                                       @RequestParam("maxpoints") float maxpoints,
                                       @RequestParam(value = "statistics", defaultValue = "false") boolean statistics) {
        metrics.registerAccessAdmin();
        logger.info("adding assignment for {}/{} with label {}", exercise, sheet, label);
        assignmentDao.createOrUpdateAssignment(new Assignment(newId, exercise, sheet, label, maxpoints, statistics));
        return "redirect:/exercise/{exid}/sheet/{sid}/admin";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/assignment/{asid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteAssignment(@PathVariable("exid") String exercise,
                                       @PathVariable("sid") String sheet,
                                       @PathVariable("asid") String assignment,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccessAdmin();
        try {
            assignmentDao.deleteAssignment(exercise, sheet, assignment);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Aufgabe konnte nicht gelöscht werden, weil noch Ergebnisse vorhanden sind!"));
        }
        return "redirect:/exercise/{exid}/sheet/{sid}/admin";
    }

    @GetMapping("/exercise/{exid}/admin")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExerciseAdminPage(@PathVariable("exid") String exercise,
                               Model model) {
        metrics.registerAccessAdmin();
        List<Sheet> sheets = sheetDao.getSheetsForExercise(exercise);
        model.addAttribute("exercise", exercise);
        model.addAttribute("sheets", sheets);
        return "exercise-admin";
    }

    @PostMapping("/exercise/{exid}/sheet")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postCreateSheet(@PathVariable("exid") String exercise,
                                       @RequestParam("id") String newId,
                                       @RequestParam("label") String label) {
        metrics.registerAccessAdmin();
        logger.info("adding sheet for {} with label {}", exercise, label);
        sheetDao.createOrUpdateSheet(new Sheet(newId, exercise, label));
        return "redirect:/exercise/{exid}/admin";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteSheet(@PathVariable("exid") String exercise,
                                       @PathVariable("sid") String sheet,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccessAdmin();
        try {
            sheetDao.deleteSheet(exercise, sheet);
            logger.info("deleted sheet for {} with id {}", exercise, sheet);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Übungsblatt konnte nicht gelöscht werden, weil noch Aufgaben vorhanden sind!"));
        }
        return "redirect:/exercise/{exid}/admin";
    }

    private String loadAnnotatedFilecontent(String exercise, String sheet, String assignment, Team team, Path file, int fileId) {
        //in addition annotations are loaded in the javascript
        List<Annotation> annotations = getAccessibleAnnotations(exercise, sheet, team, fileId);
        Map<Integer, List<Annotation>> annotationsByLine = annotations.stream().collect(Collectors.groupingBy(Annotation::getLine));
        List<FileWarnings.Warning> warnings = warningsDao.getWarningsForFile(fileId);
        Map<Integer, List<FileWarnings.Warning>> warningsByLine = warnings.stream().collect(Collectors.groupingBy(FileWarnings.Warning::getBegin_line));

        StringBuilder builder = new StringBuilder();
        int linenum = 1;

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\t", "    ");
                line = HtmlUtils.htmlEscape(line);
                List<Annotation> lineAnnotations = annotationsByLine.get(linenum);
                if (lineAnnotations != null) {
                    for (Annotation annotation : lineAnnotations) {
                        builder.append("<div class=\"annotationbox commentbox popover right nocode\"><div class=\"arrow\"></div><div class=\"comment popover-content\">");
                        String html = Markdown.toHtml(annotation.getAnnotationObj());
                        html = html.replace("\n", " ").replace("\r", " ");
                        builder.append(html);
                        builder.append("</div></div>");
                    }
                }
                List<FileWarnings.Warning> lineWarnings = warningsByLine.get(linenum);
                if (lineWarnings != null) {
                    for (FileWarnings.Warning warning : lineWarnings) {
                        builder.append("<div class=\"warningbox commentbox popover right nocode\"><div class=\"comment popover-content\">");
                        String html = Markdown.toHtml(warning.getMarkdown());
                        html = html.replace("\n", " ").replace("\r", " ");
                        builder.append(html);
                        builder.append("</div></div>");
                    }
                }
                builder.append(line).append(System.lineSeparator());
                linenum++;
            }
        } catch (IOException e) {
            throw new UploadException("Error reading upload", e);
        }
        return builder.toString();
    }

    //only return annotations if not hidden or assessor
    private List<Annotation> getAccessibleAnnotations(String exercise, String sheet, Team team, int fileId) {
        if (commentsDao.isHiddenComment(exercise, sheet, team) && !accessChecker.hasAssessRight(exercise, team.getGroup())) {
            return new ArrayList<>();
        } else {
            return annotationDao.getAnnotationsForFile(fileId);
        }
    }

    //only return annotations if not hidden or assessor
    private List<Annotation> getAccessibleAnnotations(String exercise, String sheet,  Team team) {
        if (commentsDao.isHiddenComment(exercise, sheet, team) && !accessChecker.hasAssessRight(exercise, team.getGroup())) {
            return new ArrayList<>();
        } else {
            return annotationDao.getAnnotationsForSheet(exercise, sheet, team);
        }
    }


    private Map<String, Object> constructUploadModel(String exercise, String sheet, String assignment, Team team, String filename) {
        Map<String, Object> uploadModel = new HashMap<>();
        Path file = uploadManager.getUploadPath(exercise, sheet, assignment, team, filename);
        String rawFilename = file.getFileName().toString();
        String langClass = null;
        String filecontent = null;
        PreviewFileType fileType;
        String extension = FileUtils.getExtension(rawFilename);
        fileType = Constants.previewFileTypeByExtension(extension);

        DateTime uploadDate = UploadManager.internalFilenameToDate(filename);
        String uploadName = UploadManager.internalFilenameToFilename(filename);
        int fileId = uploadsDao.getUploadId(exercise, sheet, assignment, team, uploadName, uploadDate);

        if (fileType == PreviewFileType.Text && file.toFile().length() < Constants.MAX_DISPLAY_FILESIZE) {
            langClass = Constants.LANG_CLASS_MAPPING.getOrDefault(extension, "");
            filecontent = loadAnnotatedFilecontent(exercise, sheet, assignment, team, file, fileId);
        }

        uploadModel.put("filename", uploadName);
        uploadModel.put("rawFilename", rawFilename);
        uploadModel.put("fileType", fileType);
        uploadModel.put("langClass", langClass);
        uploadModel.put("filecontent", filecontent);
        uploadModel.put("fileid", fileId);
        return uploadModel;
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/assignment/{asid}/team/{gid}/{tid}/view/{filename:[0-9]+-.+}")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public String getViewFilePage(@PathVariable("exid") String exercise,
                                  @PathVariable("sid") String sheet,
                                  @PathVariable("asid") String assignment,
                                  @PathVariable("gid") String groupid,
                                  @PathVariable("tid") String teamid,
                                  @PathVariable("filename") String filename,
                                  Model model) {
        metrics.registerAccessFile();
        Team team = new Team(groupid, teamid);
        boolean hasEditRight = accessChecker.hasAssessRight(exercise, groupid);

        Map<String, Object> uploadModel = constructUploadModel(exercise, sheet, assignment, team, filename);

        //mark annotation read
        annotationDao.markRead(accessChecker.getAuthentication().getStudentid(), (int) uploadModel.get("fileid"));

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("assignment", assignment);
        model.addAttribute("team", team);
        model.addAttribute("hasEditRight", hasEditRight);
        model.addAllAttributes(uploadModel);
        return "file-page";
    }

    //Controller to display two files at the same time for similarity checker
    @GetMapping("/exercise/{exid}/sheet/{sid}/compare/{asid}/{gid1}/{tid1}/{filename1:[0-9]+-.+}/{gid2}/{tid2}/{filename2:[0-9]+-.+}")
    public String getCompareFilesPage(@PathVariable("exid") String exercise,
                                  @PathVariable("sid") String sheet,
                                  @PathVariable("asid") String assignment,
                                  @PathVariable("gid1") String groupid1,
                                  @PathVariable("tid1") String teamid1,
                                  @PathVariable("filename1") String filename1,
                                  @PathVariable("gid2") String groupid2,
                                  @PathVariable("tid2") String teamid2,
                                  @PathVariable("filename2") String filename2,
                                  Model model) {
        metrics.registerAccessFile();
        Team team1 = new Team(groupid1, teamid1);
        boolean hasEditRight1 = true; // TODO accessChecker.hasAssessRight(exercise, groupid1);
        Team team2 = new Team(groupid2, teamid2);
        boolean hasEditRight2 = true; // TODO accessChecker.hasAssessRight(exercise, groupid2);

        //get uploadmodels and modify keys so two can be used.
        Map<String, Object> uploadModel1Unedited = constructUploadModel(exercise, sheet, assignment, team1, filename1);
        Map<String, Object> uploadModel1 = new HashMap<String, Object>() {};
        for (Map.Entry<String, Object> entry : uploadModel1Unedited.entrySet()) {
            uploadModel1.put(entry.getKey() + "1", entry.getValue());
        }

        Map<String, Object> uploadModel2Unedited = constructUploadModel(exercise, sheet, assignment, team2, filename2);
        Map<String, Object> uploadModel2 = new HashMap<String, Object>() {};
        for (Map.Entry<String, Object> entry : uploadModel2Unedited.entrySet()) {
            uploadModel2.put(entry.getKey() + "2", entry.getValue());
        }



        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("assignment", assignment);
        model.addAttribute("team1", team1); //can't rename this
        model.addAttribute("hasEditRight1", hasEditRight1);
        model.addAttribute("team2", team2);
        model.addAttribute("hasEditRight2", hasEditRight2);
        model.addAllAttributes(uploadModel1);
        model.addAllAttributes(uploadModel2);
        return "compare-files";
    }

    //show uploads for snapshot
    @GetMapping("/exercise/{exid}/sheet/{sid}/assignment/{asid}/team/{gid}/{tid}/view/{date:[0-9]+}")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public String getFilesByTimePage(@PathVariable("exid") String exercise,
                                  @PathVariable("sid") String sheet,
                                  @PathVariable("asid") String assignment,
                                  @PathVariable("gid") String groupid,
                                  @PathVariable("tid") String teamid,
                                  @PathVariable("date") String date,
                                  Model model) {
        metrics.registerAccessFile();
        Team team = new Team(groupid, teamid);
        boolean hasEditRight = accessChecker.hasAssessRight(exercise, groupid);
        List<Map<String, Object>> uploadModels = new LinkedList<>();
        List<Upload> uploads;
        DateTime parsedDate;
        try {
            parsedDate = Constants.DATE_FORMATER.parseDateTime(date);
            uploads = uploadsDao.getSnapshot(exercise, sheet, assignment, team, parsedDate);

            for (Upload upload : uploads) {
                uploadModels.add(constructUploadModel(exercise, sheet, assignment, team, UploadManager.internFilename(upload.getUploadDate(), upload.getFilename())));
            }

            //mark files read
            if (accessChecker.isOnlyStudent(exercise)) {
                List<Integer> markFilesRead = uploadModels.stream().map(u -> ((Integer) u.get("fileid"))).collect(Collectors.toList());

                if (!markFilesRead.isEmpty()) {
                    annotationDao.markRead(accessChecker.getAuthentication().getStudentid(), markFilesRead);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Ungültiger Pfad.");
        }

        List<DateTime> snapshots = UploadsDao.snapshotFromUploads(uploads);
        Optional<DateTime> snapshot = snapshots.stream().filter(s -> s.getMillis() <= parsedDate.getMillis()).max(Comparator.naturalOrder());


        if (snapshot.isPresent()) {
            Testresult testResult = testresultDao.getTestResultDetails(exercise, sheet, assignment, team, snapshot.get());
            if (testResult != null) {
                /*
                    Check if all tested files exist.

                    Needed if not the last added file is deleted. Then the upload date from the last added file is used
                    as shapshot for the test, which tested with the deleted file.
                 */
                List<Upload> testUploads = uploadsDao.getSnapshot(exercise, sheet, assignment, team, testResult.getSnapshot());
                if (CollectionUtils.isEqualCollection(uploads, testUploads)) {
                    try {
                        model.addAttribute("testResultDetails", TestResultDetails.fromJson(testResult.getResult()));
                    } catch (Exception e) {
                        // ignore
                    }
                    model.addAttribute("testResult", testResult);
                }
            }
        }

        boolean showSidebar = uploadModels.size() > 1;

        model.addAttribute("exercise", exercise);
        model.addAttribute("sheet", sheet);
        model.addAttribute("assignment", assignment);
        model.addAttribute("team", team);
        model.addAttribute("hasEditRight", hasEditRight);
        model.addAttribute("showSidebar", showSidebar);
        model.addAttribute("uploads", uploadModels);
        return "files-page";
    }

    //redirect old links without version
    @GetMapping("/exercise/{exid}/sheet/{sid}/assignment/{asid}/team/{gid}/{tid}/view")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    public String getViewFilesPage(@PathVariable("exid") String exercise,
                                   @PathVariable("sid") String sheet,
                                   @PathVariable("asid") String assignment,
                                   @PathVariable("gid") String groupid,
                                   @PathVariable("tid") String teamid,
                                   Model model) {
        metrics.registerAccessFile();
        return "redirect:/exercise/{exid}/sheet/{sid}/assignment/{asid}/team/{gid}/{tid}/view/" + Constants.DATE_FORMATER.print(DateTime.now());
    }

    @GetMapping("/exercise/{exid}/sheet/{sheetid}/assignment/{assignid}/team/{gid}/{tid}/{fileid}/annotations")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    @ResponseBody
    public List<MDAnnotation> getAnnotations(@PathVariable("exid") String exercise,
                                             @PathVariable("sheetid") String sheet,
                                             @PathVariable("assignid") String assignment,
                                             @PathVariable("gid") String groupid,
                                             @PathVariable("tid") String teamid,
                                             @PathVariable("fileid") int fileId) {
        metrics.registerAccessAnnotations();
        Team team = new Team(groupid, teamid);
        List<Annotation> annotations = getAccessibleAnnotations(exercise, sheet, team, fileId);
        return annotations.stream()
                .map(a -> {
                    MDAnnotation mdann = new MDAnnotation();
                    mdann.setLine(a.getLine());
                    mdann.setText(a.getAnnotationObj());
                    mdann.setMarkdown(Markdown.toHtml(a.getAnnotationObj()));
                    return mdann;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/exercise/{exid}/sheet/{sheetid}/assignment/{assignid}/team/{gid}/{tid}/{fileid}/warnings")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    @ResponseBody
    public List<MDAnnotation> getWarnings(@PathVariable("exid") String exercise,
                                             @PathVariable("sheetid") String sheet,
                                             @PathVariable("assignid") String assignment,
                                             @PathVariable("gid") String groupid,
                                             @PathVariable("tid") String teamid,
                                             @PathVariable("fileid") int fileId) {
        metrics.registerAccessAnnotations();
        Team team = new Team(groupid, teamid);
        List<FileWarnings.Warning> warnings = warningsDao.getWarningsForFile(fileId);
        return warnings.stream()
                .map(a -> {
                    MDAnnotation mdann = new MDAnnotation();
                    mdann.setLine(a.getBegin_line());
                    mdann.setText(a.getMarkdown());
                    mdann.setMarkdown(Markdown.toHtml(a.getMarkdown()));
                    return mdann;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/exercise/{exid}/sheet/{sheetid}/assignment/{assignid}/team/{gid}/{tid}/{fileid}/annotations")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    @ResponseBody
    public Map<String, String> saveAnnotation(
            @PathVariable("exid") String exercise,
            @PathVariable("sheetid") String sheet,
            @PathVariable("assignid") String assignment,
            @PathVariable("gid") String groupid,
            @PathVariable("tid") String teamid,
            @PathVariable("fileid") int fileId,
            @RequestParam("text") String text,
            @RequestParam("lineNr") int lineNr) {
        metrics.registerAccessAnnotations();
        Team team = new Team(groupid, teamid);

        text = text.trim();

        Annotation annotation = new Annotation(fileId, lineNr, text);

        if (text.isEmpty()) {
            annotationDao.deleteAnnotation(annotation);
            annotationDao.markRead(fileId);
            return ImmutableMap.of("status", "deleted",
                    "markdown", "");
        }

        annotationDao.createOrUpdateAnnotation(annotation);
        List<String> studentIds = userDao.getStudentsInTeam(exercise, team).stream()
                .map(StudentInfo::getId).collect(Collectors.toList());
        annotationDao.markUnread(studentIds, fileId);

        return ImmutableMap.of(
                "status", "updated",
                "markdown", Markdown.toHtml(text));
    }


    @PostMapping("/exercise/{exid}/sheet/{sid}/team/{gid}/{tid}/feedback/{filename}/delete")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    public String postDeleteFeedbackUpload(@PathVariable("exid") String exercise,
                                           @PathVariable("sid") String sheet,
                                           @PathVariable("gid") String groupid,
                                           @PathVariable("tid") String teamid,
                                           @PathVariable("filename") String filename) {
        metrics.registerAccessFeedback();
        Team team = new Team(groupid, teamid);
        Path path = uploadManager.getFeedbackFilePath(exercise, sheet, team, filename);
        try {
            Files.delete(path);
            return "redirect:/exercise/{exid}/sheet/{sid}/assessment/{gid}/{tid}";
        } catch (IOException e) {
            throw new UploadException("Could not delete feedback file", e);
        }
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/feedback")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise, #groupid)")
    public String postUploadFeedback(@PathVariable("exid") String exercise,
                                     @PathVariable("sid") String sheet,
                                     @RequestParam("group") String groupid,
                                     @RequestParam("team") String teamid,
                                     @RequestParam("file") MultipartFile file,
                                     RedirectAttributes redirectAttributes) {
        metrics.registerAccessFeedback();
        Team team = new Team(groupid, teamid);
        uploadManager.putFeedbackUpload(exercise, sheet, team, file);
        redirectAttributes.addAttribute("group", groupid);
        redirectAttributes.addAttribute("team", teamid);
        return "redirect:/exercise/{exid}/sheet/{sid}/assessment/{group}/{team}";
    }

    @GetMapping("/exercise/admin")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getExerciseAdmin(Model model) {
        metrics.registerAccessAdmin();

        List<Exercise> exercises = exerciseDao.getAllExercises();
        Map<String, Integer> studentCount = userDao.getStudentCountForExercise();
        model.addAttribute("exercises", exercises);
        model.addAttribute("studentCount", studentCount);
        return "lecture-admin";
    }

    @PostMapping("/exercise/admin")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postCreateExercise(@RequestParam("id") String exercise,
                                  @RequestParam("lecture") String lecture,
                                  @RequestParam("term") String term) {
        metrics.registerAccessAdmin();
        logger.info("adding exercise with id {}", exercise);
        exerciseDao.createOrUpdateExercise(new Exercise(exercise, lecture, term));
        return "redirect:/exercise/admin";
    }

    @PostMapping("/exercise/admin/{exid}/delete")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postDeleteExercise(@PathVariable("exid") String exercise,
                                  RedirectAttributes redirectAttributes) {
        metrics.registerAccessAdmin();
        try {
            exerciseDao.deleteExercise(exercise);
            logger.info("deleted exercise with id {}", exercise);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung konnte nicht gelöscht werden, weil noch Übungen vorhanden sind!"));
        }
        return "redirect:/exercise/admin";
    }

    @GetMapping("/exercise/join")
    public String getExerciseJoin(Model model) {
        metrics.registerAccessExercise();

        List<Exercise> exercises = exerciseDao.getJoinableExercises();
        model.addAttribute("exercises", exercises);
        model.addAttribute("joinedExercises", securityTools.getAccessibleExercises());
        return "lecture-join";
    }

    @PostMapping("/exercise/join/{exid}")
    public String postJoinExercise(@PathVariable("exid") String exercise,
                                     RedirectAttributes redirectAttributes) {
        metrics.registerAccessExercise();

        if (!accessChecker.isStudent()) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesungen können nur von Studenten angemeldet werden!"));
            return "redirect:/exercise/join";
        }

        if (accessChecker.canAccess(exercise)) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Sie sind bereits für die Vorlesung angemeldet."));
            return "redirect:/exercise/join";
        }

        Exercise exercise1 = exerciseDao.getExercise(exercise);
        if (exercise1 == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung existiert nicht."));
            return "redirect:/exercise/join";
        }

        if (!exercise1.isRegistrationOpen()) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung kann nicht angemeldet werden."));
            return "redirect:/exercise/join";
        }

        ExerciseRights exerciseRights = new ExerciseRights();
        exerciseRights.setExerciseId(exercise);
        exerciseRights.setRole(ExerciseRights.Role.student);

        int userid = accessChecker.getAuthentication().getUserid();
        userDao.addUserRights(userid, exerciseRights);
        accessChecker.updateRights();

        switch (exercise1.getGroupJoin()) {
        case GROUP:
            return "redirect:/exercise/{exid}/groups";
        case PREFERENCES:
            return "redirect:/exercise/{exid}/groups/preferences";
        default:
            return "redirect:/exercise/{exid}";
        }
    }

    @PostMapping("/exercise/unjoin/{exid}")
    public String postUnjoinExercise(@PathVariable("exid") String exercise,
                                   RedirectAttributes redirectAttributes) {
        metrics.registerAccessExercise();

        if (!accessChecker.isStudent()) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesungen können nur von Studenten abgemeldet werden!"));
            return "redirect:/exercise/join";
        }

        if (!accessChecker.canAccess(exercise)) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Sie sind nicht für die Vorlesung angemeldet."));
            return "redirect:/exercise/join";
        }

        Exercise exercise1 = exerciseDao.getExercise(exercise);
        if (exercise1 == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung existiert nicht."));
            return "redirect:/exercise/join";
        }

        if (!exercise1.isRegistrationOpen()) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung kann nicht abgemeldet werden."));
            return "redirect:/exercise/join";
        }

        int userid = accessChecker.getAuthentication().getUserid();

        //TODO check
        try {
            userDao.deleteUserRights(userid, exercise);
            accessChecker.updateRights();
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Vorlesung konnte nicht abgemeldet werden."));
        }
        return "redirect:/exercise/join";
    }

    public static class OverviewAssignmentData {
        public TestResultDetails testResultDetails;
        private Assignment assignment;
        private float points;
        private int annotationCount = 0;
        private Map<Integer, UploadWithAnnotations> uploads = new HashMap<>();
        private Testresult testresult;
        private boolean testExists;
        private DateTime snapshot;


        public List<Upload> activeUploads() {
            if (uploads == null || uploads.isEmpty()) {
                return Collections.emptyList();
            }
            return uploads.values().stream().filter(u -> u.upload.getDeleteDate() == null).map(u -> u.upload).collect(Collectors.toList());
        }

        public List<Upload> deletedUploads() {
            if (uploads == null || uploads.isEmpty()) {
                return Collections.emptyList();
            }
            //sort from new to old
            return uploads.values().stream().filter(u -> u.upload.getDeleteDate() != null)
                    .map(u -> u.upload)
                    .sorted(Comparator.comparingLong((Upload upload) -> upload.getUploadDate().getMillis()).reversed())
                    .collect(Collectors.toList());
        }

        public List<UploadWithAnnotations > deletedUploadsWithAnnotations() {
            return uploads.values().stream().filter(u -> u.upload.getDeleteDate() != null)
                    .sorted(Comparator.comparingLong((UploadWithAnnotations u) -> u.upload.getUploadDate().getMillis()).reversed())
                    .collect(Collectors.toList());
        }

        public boolean isTestResultUpToDate() {
            if (snapshot == null || testresult == null) {
                return false;
            }
            return snapshot.equals(testresult.getSnapshot());
        }

        public void addUploads(List<Upload> uploads) {
            uploads.forEach(upload -> this.uploads.put(upload.getId(), new UploadWithAnnotations(upload)));
        }

        public void addAnnotations(List<Annotation> annotations) {
            for (Annotation annotation : annotations) {
                if (uploads.containsKey(annotation.getFileId())) {
                    uploads.get(annotation.getFileId()).annotations.add(annotation);
                }
            }

            annotationCount = uploads.values().stream().filter(u -> u.upload.getDeleteDate() == null).mapToInt(u -> u.annotations.size()).sum();
        }

        public void addWarnings(List<FileWarnings.Warning> warnings, int fileId) {
            for (FileWarnings.Warning warning : warnings) {
                if (uploads.containsKey(fileId)) {
                    uploads.get(fileId).warnings.add(warning);
                }
            }
        }

        public Assignment getAssignment() {
            return assignment;
        }

        public float getPoints() {
            return points;
        }

        public int getAnnotationCount() {
            return annotationCount;
        }

        public int getWarningsCount() {
            return  uploads.values().stream().filter(u -> u.upload.getDeleteDate() == null).mapToInt(u -> u.warnings.size()).sum();
        }

        public List<Upload> getUploads() {
            return uploads.values().stream().map(u -> u.upload).collect(Collectors.toList());
        }

        public Testresult getTestresult() {
            return testresult;
        }

        public boolean isTestExists() {
            return testExists;
        }

        public DateTime getSnapshot() {
            return snapshot;
        }

        public int deletedAnnotationCount() {
            return  uploads.values().stream().filter(u -> u.upload.getDeleteDate() != null).mapToInt(u -> u.annotations.size()).sum();
        }

        public int deletedWarningsCount() {
            return  uploads.values().stream().filter(u -> u.upload.getDeleteDate() != null).mapToInt(u -> u.warnings.size()).sum();
        }

        public void markUnread(List<Integer> files) {
            for (Integer file : files) {
                if (uploads.get(file) != null) {
                    uploads.get(file).unread = true;
                }
            }
        }

        public int unreadCount() {
            return (int) uploads.values().stream().filter(u -> u.unread &&  u.upload.getDeleteDate() == null).count();
        }

        public int deletedUnreadCount() {
            return (int) uploads.values().stream().filter(u -> u.unread &&  u.upload.getDeleteDate() != null).count();
        }

    }

    public static class OverviewStudentData {
        public StudentInfo student;
        public float delta;
        public String reason;

        public boolean deltaPointsAvailable() {
            return deltaReasonAvailable() || delta != 0;
        }

        public boolean deltaReasonAvailable() {
            return reason != null && !reason.isEmpty();
        }
    }

    public class OverviewDataForTeam {
        public Team team;
        public String exercise;
        public String sheet;
        public Comment comment;
        public List<OverviewAssignmentData> assignments;
        public List<OverviewStudentData> students;

        /**
         * parses the markdown in the comment and returns the html
         */
        public String commentAsHtml() {
            return (comment == null)? Markdown.toHtml("") : Markdown.toHtml(comment.getComment());
        }

        public boolean isCommentHidden() {
            return comment != null && comment.isHidden();
        }

        public String getStudentName(String studentId) {
            return students.stream()
                    .filter(s -> s.student.getId().equals(studentId))
                    .findAny()
                    .map(s -> s.student.getFirstname() + " " + s.student.getLastname())
                    .orElseGet(() -> {
                        if (accessChecker.isAssistant()) {
                            // only assistents can see studentId
                            return studentId;
                        } else {
                            return "Unbekannt";
                        }
                    });
        }
    }

    private class AssignmentWithPoints {
        Assignment assignment;
        String pointsRaw;
        float points;

        AssignmentWithPoints(Assignment a, String points) {
            this.assignment = a;
            this.pointsRaw = points;
        }

        boolean pointsValid() {
            return pointsRaw != null;
        }

        AssignmentWithPoints withPoints(float points) {
            this.points = points;
            return this;
        }
    }

    public static class SheetResult {
        public Sheet sheet;
        public float maxPoints;
        public Float points = null;
        public Boolean attended;
        public Boolean unreadAnnotations;

        public SheetResult(Sheet sheet) {
            this.sheet = sheet;
        }
    }

    public static class UploadWithAnnotations {
        public Upload upload;
        public List<Annotation> annotations = new ArrayList<>();
        public List<FileWarnings.Warning> warnings = new ArrayList<>();

        public UploadWithAnnotations(Upload upload) {
            this.upload = upload;
        }

        public boolean unread = false;
    }

    public static class ExamOverview {
        public Exam exam;
        public boolean isRegistered = false;

        public ExamOverview(Exam exam) {
            this.exam = exam;
        }
    }
}
