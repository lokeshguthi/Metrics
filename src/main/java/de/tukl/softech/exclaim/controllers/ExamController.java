package de.tukl.softech.exclaim.controllers;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.dao.*;
import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.AccessChecker;
import de.tukl.softech.exclaim.transferdata.*;
import de.tukl.softech.exclaim.utils.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Controller
public class ExamController {
    private static final Logger logger = LoggerFactory.getLogger(ExamController.class);

    private AccessChecker accessChecker;
    private MetricsService metrics;
    private ExamDao examDao;
    private ExerciseDao exerciseDao;
    private UserDao userDao;

    @Value("${exclaim.csv.separator:;}")
    private char csvSeparator;

    public ExamController(AccessChecker accessChecker, MetricsService metrics,
                          ExamDao examDao, ExerciseDao exerciseDao, UserDao userDao) {
        this.accessChecker = accessChecker;
        this.metrics = metrics;
        this.examDao = examDao;
        this.exerciseDao = exerciseDao;
        this.userDao = userDao;
    }

    @GetMapping("/exercise/{exid}/exam/admin")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExams(@PathVariable("exid") String exercise, Model model) {
        metrics.registerAccess("exam-admin");
        List<Exam> exams = examDao.getExamsForExercise(exercise);
        model.addAttribute("exercise", exercise);
        model.addAttribute("exams", exams);
        return "exam-admin";
    }

    @PostMapping("/exercise/{exid}/exam")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postCreateExam(@PathVariable("exid") String exercise,
                                 @RequestParam("id") String newId,
                                 @RequestParam("label") String label,
                                 @RequestParam("date") String date,
                                 @RequestParam("location") String location) {
        metrics.registerAccess("exam-admin");
        logger.info("adding exam for {} with label {}", exercise, label);

        DateTime dateTime = new DateTime(date);

        Exam exam = new Exam(newId, exercise, label, dateTime, location, false, false);
        examDao.createOrUpdateExam(exam);
        return "redirect:/exercise/{exid}/exam/admin";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteExam(@PathVariable("exid") String exercise,
                                       @PathVariable("eid") String id,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");
        try {
            examDao.deleteExam(id, exercise);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Klausur konnte nicht gelöscht werden, weil noch Aufgaben vorhanden sind!"));
        }
        return "redirect:/exercise/{exid}/exam/admin";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/tasks")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExamTasks(@PathVariable("exid") String exercise,
                               @PathVariable("eid") String exam,
                               Model model) {
        metrics.registerAccess("exam-admin");
        List<ExamTask> tasks = examDao.getExamTasks(exercise, exam);
        model.addAttribute("exercise", exercise);
        model.addAttribute("exam", exam);
        model.addAttribute("tasks", tasks);
        return "exam-tasks";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/task")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postCreateExamTask(@PathVariable("exid") String exercise,
                                 @PathVariable("eid") String exam,
                                 @RequestParam("id") String id,
                                 @RequestParam("maxPoints") float maxPoints) {
        metrics.registerAccess("exam-admin");
        logger.info("adding exam task for exercise {} and exam {} with label {}", exercise, exam, id);

        examDao.createOrUpdateExamTask(new ExamTask(exercise, exam, id, maxPoints));
        return "redirect:/exercise/{exid}/exam/{eid}/tasks";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/task/{tid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteTask(@PathVariable("exid") String exercise,
                                       @PathVariable("eid") String exam,
                                       @PathVariable("tid") String task,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");
        try {
            examDao.deleteExamTask(exercise, exam, task);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Aufgabe konnte nicht gelöscht werden, weil noch Ergebnisse vorhanden sind!"));
        }
        return "redirect:/exercise/{exid}/exam/{eid}/tasks";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/grades")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExamGrades(@PathVariable("exid") String exercise,
                               @PathVariable("eid") String exam,
                               Model model) {
        metrics.registerAccess("exam-admin");
        List<ExamGrade> grades = examDao.getExamGrades(exercise, exam);
        model.addAttribute("exercise", exercise);
        model.addAttribute("exam", exam);
        model.addAttribute("grades", grades);
        return "exam-grades";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/grade")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postCreateExamGrade(@PathVariable("exid") String exercise,
                                     @PathVariable("eid") String exam,
                                     @RequestParam("grade") String grade,
                                     @RequestParam("minPoints") float minPoints) {
        metrics.registerAccess("exam-admin");
        logger.info("adding exam grade for exercise {} and exam {} with label {}", exercise, exam, grade);

        examDao.createOrUpdateExamGrade(new ExamGrade(exercise, exam, grade, minPoints));
        return "redirect:/exercise/{exid}/exam/{eid}/grades";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/grade/{gid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteGrade(@PathVariable("exid") String exercise,
                                       @PathVariable("eid") String exam,
                                       @PathVariable("gid") String grade,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");
        try {
            examDao.deleteExamGrade(exercise, exam, grade);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Note konnte nicht gelöscht werden, weil noch Ergebnisse vorhanden sind!"));
        }
        return "redirect:/exercise/{exid}/exam/{eid}/grades";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/participants")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExamParticipants(@PathVariable("exid") String exercise,
                                @PathVariable("eid") String exam,
                                Model model) {
        metrics.registerAccess("exam-admin");
        List<ExamTask> tasks = examDao.getExamTasks(exercise, exam);
        List<ExamParticipant> participants = new ArrayList<>();

        List<String> participantIds = examDao.getExamParticipants(exercise, exam);
        List<StudentInfo> exerciseStudents = userDao.getStudentsInExercise(exercise);
        Map<String, Map<String, Float>> results = examDao.getExamResults(exercise, exam);
        List<ExamGrade> grades = examDao.getExamGrades(exercise, exam);

        for (StudentInfo student : exerciseStudents) {
            if (participantIds.contains(student.getId())) {
                ExamParticipant examParticipant = new ExamParticipant(student);
                participants.add(examParticipant);
                if (results.containsKey(student.getId())) {
                    examParticipant.results = results.get(student.getId());
                }
                examParticipant.grade = getGrade(grades, examParticipant);
            }
        }

        Exam examDetails = examDao.getExam(exercise, exam);

        model.addAttribute("exercise", exercise);
        model.addAttribute("exam", exam);
        model.addAttribute("examDetails", examDetails);
        model.addAttribute("tasks", tasks);
        model.addAttribute("participants", participants);
        return "exam-participants";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/participants/import")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExamParticipantsImport(@PathVariable("exid") String exercise,
                                      @PathVariable("eid") String exam,
                                      Model model) {
        metrics.registerAccess("exam-admin");

        model.addAttribute("exercise", exercise);
        model.addAttribute("exam", exam);
        return "exam-participants-import";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/participants/import")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postImportParticipants(@PathVariable("exid") String exercise,
                                  @PathVariable("eid") String exam,
                                  @RequestParam("studentids") String ids,
                                  RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");
        List<String> participantIds = userDao.getStudentsInExercise(exercise).stream().map(StudentInfo::getId).collect(Collectors.toList());
        List<String> importIds = new ArrayList<>();
        StringJoiner notImported = new StringJoiner(", ");

        String[] lines = ids.split("\n");
        for (String line : lines) {
            String id  = line.trim();
            if (id.length() == 0) continue;
            if (participantIds.contains(id)) {
                importIds.add(id);
            } else {
                notImported.add(id);
            }
        }
        examDao.addExamParticipants(exercise, exam, importIds);
        redirectAttributes.addFlashAttribute("resultmessage", importIds.size() + " Studenten importiert!");
        if (notImported.length() > 0) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Folgende Studenten konnten nicht importiert werden, da sie nicht in der Übung sind: " + notImported.toString()));
        }

        return "redirect:/exercise/{exid}/exam/{eid}/participants/import";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/result/add")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getExamAddResult(@PathVariable("exid") String exercise,
                                            @PathVariable("eid") String exam,
                                            @RequestParam(value = "studentid", required = false) String studentId,
                                            ExamResultForm resultForm,
                                            Model model) {
        metrics.registerAccess("exam-admin");

        List<ExamTask> tasks = examDao.getExamTasks(exercise, exam);

        float maxPoints = tasks.stream().map(ExamTask::getMaxPoints).reduce(Float::sum).orElse(0f);

        if (studentId != null) {
            Map<String, Float> results = examDao.getExamResultsForStudent(exercise, exam, studentId);
            resultForm = new ExamResultForm(studentId, results);
        }

        model.addAttribute("exercise", exercise);
        model.addAttribute("exam", exam);
        model.addAttribute("tasks", tasks);
        model.addAttribute("maxPoints", maxPoints);
        model.addAttribute("resultForm", resultForm);
        return "exam-add-result";
    }

    //TODO correct rights
    @GetMapping("/exercise/{exid}/exam/{eid}/student")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    @ResponseBody
    public Map<String, Object> getStudent(@PathVariable("exid") String exercise,
                                          @PathVariable("eid") String exam,
                             @RequestParam("studentid") String studentId) {
        metrics.registerAccess("exam-admin");

        User user = userDao.getUserByStudentId(studentId);

        if (user == null) {
            return ImmutableMap.of("firstname", "-", "lastname", "-", "newparticipant", "false");
        } else {
            boolean isParticipant = examDao.isParticipant(exercise, exam, studentId);
            Map<String, Float> examResults = examDao.getExamResultsForStudent(exercise, exam, studentId);
            return ImmutableMap.of("firstname", user.getFirstname(),
                    "lastname", user.getLastname(),
                    "newparticipant", !isParticipant,
                    "results", examResults);
        }
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/result/add")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postExamResult(@PathVariable("exid") String exercise,
                                 @PathVariable("eid") String exam,
                                 @ModelAttribute("resultForm") ExamResultForm resultForm,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        metrics.registerAccess("exam-admin");

        String studentId = resultForm.studentId;
        User user = userDao.getUserByStudentId(studentId);

        redirectAttributes.addAttribute("exid", exercise);
        redirectAttributes.addAttribute("eid", exam);

        if (user == null) {
            bindingResult.reject("User does not exist.");
            redirectAttributes.addFlashAttribute("errors",
                                singletonList("Benutzer existiert nicht."));
        } else {
            if (!inExercise(exercise, studentId)) {
                // Add user to exercise if necessary
                ExerciseRights exerciseRight = new ExerciseRights();
                exerciseRight.setUserId(user.getUserid());
                exerciseRight.setExerciseId(exercise);
                exerciseRight.setRole(ExerciseRights.Role.student);
                userDao.addUserRights(user.getUserid(), exerciseRight);
            }
            examDao.addExamParticipants(exercise, exam, singletonList(studentId));
            examDao.addExamResults(exercise, exam, studentId, resultForm.results);
        }
        return "redirect:/exercise/{exid}/exam/{eid}/result/add";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/participant/{sid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteParticipant(@PathVariable("exid") String exercise,
                                  @PathVariable("eid") String exam,
                                  @PathVariable("sid") String studentId,
                                  RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");

        examDao.deleteExamResults(exercise, exam, studentId);
        examDao.removeExamParticipant(exercise, exam, studentId);

        return "redirect:/exercise/{exid}/exam/{eid}/participants";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/registrationStatus")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postExamRegistrationStatus(@PathVariable("exid") String exercise,
                                         @PathVariable("eid") String exam,
                                         @RequestParam("registration") boolean registration,
                                         RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");

        examDao.updateExamRegistrationStatus(exercise, exam, registration);
        return "redirect:/exercise/{exid}/exam/{eid}/participants";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/showResults")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postExamShowResults(@PathVariable("exid") String exercise,
                                             @PathVariable("eid") String exam,
                                             @RequestParam("showResults") boolean showResults,
                                             RedirectAttributes redirectAttributes) {
        metrics.registerAccess("exam-admin");

        examDao.updateExamShowResults(exercise, exam, showResults);
        return "redirect:/exercise/{exid}/exam/{eid}/participants";
    }

    @PostMapping("/exercise/{exid}/exam/{eid}/register")
    public String postExamRegister(@PathVariable("exid") String exercise,
                                      @PathVariable("eid") String exam,
                                      @RequestParam("register") boolean register,
                                      RedirectAttributes redirectAttributes) {
        metrics.registerAccessExam();
        boolean isOnlyStudent  = accessChecker.isOnlyStudent(exercise);
        if (isOnlyStudent) {
            String studentId = accessChecker.getAuthentication().getStudentid();
            Exam examDetails = examDao.getExam(exercise, exam);

            if (examDetails.isRegistrationOpen()) {
                if (register) {
                    examDao.addExamParticipants(exercise, exam, singletonList(studentId));
                } else {
                    examDao.removeExamParticipant(exercise, exam, studentId);
                }
            } else {
                //TODO
                redirectAttributes.addFlashAttribute("registerError", "An- und Abmelden aktuell nicht möglich!");
            }
        }
        return "redirect:/exercise/{exid}";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/result")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise) || @examDao.showResults(#exercise, #exam)")
    public String getExamResult(@PathVariable("exid") String exercise,
                                   @PathVariable("eid") String exam,
                                   Model model) {
        metrics.registerAccessExam();
        boolean isOnlyStudent  = accessChecker.isOnlyStudent(exercise);

        List<ExamGrade> grades = examDao.getExamGrades(exercise, exam);
        List<ExamTask> tasks = examDao.getExamTasks(exercise, exam);
        Exam examDetails = examDao.getExam(exercise, exam);
        String studentId = "-";
        String grade = "-";
        float sumPoints = 0f;
        Map<String, Float> results = new HashMap<>();

        if (isOnlyStudent) {
            studentId = accessChecker.getAuthentication().getStudentid();
            results = examDao.getExamResultsForStudent(exercise, exam, studentId);

            sumPoints = results.values().stream().reduce(Float::sum).orElse(0f);
            grade = getGrade(grades, sumPoints);
        }

        float maxPoints = tasks.stream().map(ExamTask::getMaxPoints).reduce(Float::sum).orElse(0f);

        model.addAttribute("exercise", exercise);
        model.addAttribute("examDetails", examDetails);
        model.addAttribute("tasks", tasks);
        model.addAttribute("maxPoints", maxPoints);
        model.addAttribute("grade", grade);
        model.addAttribute("grades", grades);
        model.addAttribute("studentId", studentId);
        model.addAttribute("results", results);
        model.addAttribute("sumPoints", sumPoints);
        return "exam-result";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/evaluation")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    public String getExamEvaluation(@PathVariable("exid") String exercise,
                                @PathVariable("eid") String exam,
                                Model model) {
        metrics.registerAccessExam();
        Exam examDetails = examDao.getExam(exercise, exam);

        List<ExamTask> tasks = examDao.getExamTasks(exercise, exam);
        Map<String, Map<String, Float>> results = examDao.getExamResults(exercise, exam);
        int maxTaskPoints = (int) Math.ceil(tasks.stream().map(ExamTask::getMaxPoints).max(Comparator.comparingDouble(Float::doubleValue)).orElse(0f));

        Map<Integer, Map<String, Integer>> resultTable = new LinkedHashMap<>();
        for (int i = 0; i <= maxTaskPoints; i++) {
            resultTable.put(i, new HashMap<>());
        }

        for (Map.Entry<String, Map<String, Float>> student : results.entrySet()) {
            for (Map.Entry<String, Float> entry : student.getValue().entrySet()) {
                int points = (int) Math.ceil(entry.getValue());
                resultTable.get(points).merge(entry.getKey(), 1, Integer::sum);
            }

        }

        ExamGroupResults groupResults = examDao.getExamGroupResults(exercise, exam);
        List<ExamGrade> grades = examDao.getExamGrades(exercise, exam);

        model.addAttribute("exercise", exercise);
        model.addAttribute("examDetails", examDetails);
        model.addAttribute("tasks", tasks);
        model.addAttribute("resultTable", resultTable);
        model.addAttribute("groupResults", groupResults);
        model.addAttribute("grades", grades);
        return "exam-evaluation";
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/gradeoverview")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise) || @examDao.showResults(#exercise, #exam)")
    @ResponseBody
    public Map<String, Integer> getExamGradeOverview(@PathVariable("exid") String exercise,
                                            @PathVariable("eid") String exam) {
        metrics.registerAccessExam();

        List<ExamGrade> grades = examDao.getExamGrades(exercise, exam);
        Map<String, Float> points = examDao.getExamPoints(exercise, exam);
        LinkedHashMap<String, Integer> gradeOverview = new LinkedHashMap<>();

        grades.forEach(grade -> gradeOverview.put(grade.getGrade(), 0));

        for (Map.Entry<String, Float> entry : points.entrySet()) {
            gradeOverview.merge(getGrade(grades, entry.getValue()), 1, Integer::sum);
        }

        return gradeOverview;
    }

    @GetMapping("/exercise/{exid}/exam/{eid}/pointsoverview")
    @PreAuthorize("@accessChecker.hasAssessRight(#exercise)")
    @ResponseBody
    public Map<String, Object> getExamPointsOverview(@PathVariable("exid") String exercise,
                                                     @PathVariable("eid") String exam) {
        metrics.registerAccessExam();

        Map<String, Object> result = new HashMap<>();

        int maxPoints = (int) Math.ceil(examDao.getExamTasks(exercise, exam).stream().map(ExamTask::getMaxPoints).reduce(Float::sum).orElse(0f));
        result.put("maxPoints", maxPoints);

        Map<String, Float> points = examDao.getExamPoints(exercise, exam);

        TreeMap<Integer, Integer> pointCount = new TreeMap<>();
        for (Map.Entry<String, Float> entry : points.entrySet()) {
            pointCount.merge((int) Math.ceil(entry.getValue()), 1, Integer::sum);
        }
        result.put("points", pointCount);
        return result;
    }


    //TODO change when move function to exclaim
    private boolean inExercise(String exercise, String studentId) {
        List<StudentInfo> studentInfos = userDao.getStudentsInExercise(exercise);
        for (StudentInfo studentInfo : studentInfos) {
            if (studentInfo.getId().equals(studentId)) {
                return true;
            }
        }
        return false;
    }


    //grades need to be sorted by minpoints from high to low
    private String getGrade(List<ExamGrade> grades, ExamParticipant participant) {
        if (participant.results.isEmpty()) {
            return "nt";
        }
        float points = participant.getSum();
        return getGrade(grades, points);
    }

    //grades need to be sorted by minpoints from high to low
    private String getGrade(List<ExamGrade> grades, float points) {
        for (ExamGrade grade : grades) {
            if (grade.getMinPoints() <= points) {
                return grade.getGrade();
            }
        }
        return "";
    }

    public static class ExamParticipant {

        private StudentInfo student;
        private Map<String, Float> results;
        private String grade = "";

        public ExamParticipant(StudentInfo student) {
            this.student = student;
            results = new HashMap<>();
        }

        public ExamParticipant(StudentInfo student, Map<String, Float> results) {
            this.student = student;
            this.results = results;
        }

        public StudentInfo getStudent() {
            return student;
        }

        public Map<String, Float> getResults() {
            return results;
        }

        public void setResults(Map<String, Float> results) {
            this.results = results;
        }

        public float getSum() {
            return results.values().stream().reduce(Float::sum).orElse(0f);
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }
    }

    public static class ExamResultForm {
        private String studentId;
        private Map<String, Float> results;

        public ExamResultForm(String studentId, Map<String, Float> results) {
            this.studentId = studentId;
            this.results = results;
        }

        public ExamResultForm() {
            this.results = new HashMap<>();
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public Map<String, Float> getResults() {
            return results;
        }

        public void setResults(Map<String, Float> results) {
            this.results = results;
        }
    }

}
