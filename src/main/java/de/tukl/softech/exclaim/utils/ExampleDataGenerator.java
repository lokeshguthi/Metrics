package de.tukl.softech.exclaim.utils;

import java.io.File;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import de.tukl.softech.exclaim.dao.*;
import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.transferdata.ExerciseRights;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import de.tukl.softech.exclaim.uploads.UploadManager;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.github.javafaker.Faker;

@Component
@ConditionalOnProperty(name = "exclaim.generate-example-data", havingValue = "true")
public class ExampleDataGenerator implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(ExampleDataGenerator.class);
    private final String[] days = IntStream.rangeClosed(1, 5).mapToObj(i -> DayOfWeek.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH)).toArray(String[]::new);
    private final String[] times = new String[]{"08:15", "10:00", "11:45", "13:45", "15:30", "17:45"};
    private final Faker faker;
    private final String password;
    private final UserDao userDao;
    private final ExerciseDao exerciseDao;
    private final GroupDao groupDao;
    private final SheetDao sheetDao;
    private final AssignmentDao assignmentDao;
    private final AttendanceDao attendanceDao;
    private final ResultsDao resultsDao;
    private final ExamDao examDao;
    private final UploadsDao uploadsDao;

    public ExampleDataGenerator(UserDao userDao, ExerciseDao exerciseDao, GroupDao groupDao, SheetDao sheetDao, AssignmentDao assignmentDao, AttendanceDao attendanceDao, ResultsDao resultsDao, ExamDao examDao, UploadsDao uploadsDao) {
        this.faker = new Faker(new Locale("de"), new Random(0));
        this.password = new BCryptPasswordEncoder(4).encode("password");
        this.userDao = userDao;
        this.exerciseDao = exerciseDao;
        this.groupDao = groupDao;
        this.sheetDao = sheetDao;
        this.assignmentDao = assignmentDao;
        this.attendanceDao = attendanceDao;
        this.resultsDao = resultsDao;
        this.examDao = examDao;
        this.uploadsDao = uploadsDao;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userDao.getAllUsers().isEmpty()) {
            logger.info("About to generate example data");
            generateUsers();
            generateExercises();
            logger.info("Example data genaration completed!");
        } else {
            logger.error("Database contains users, not generating any data!");
        }
    }

    private void generateUsers() {
        logger.info("Generating users...");
        for (int i = 1; i <= 500; i++) {
            String username = "u" + i;
            String studentid = Integer.toString(100000 + i);
            String firstname = faker.name().firstName();
            String lastname = faker.name().lastName();
            String email = (firstname + " " + lastname).toLowerCase().replaceAll(" ", ".").replaceAll("[^a-z.]", "") + "@example.com";
            User user = new User(0, username, firstname, lastname, studentid, email, password, true, false, "");
            userDao.createUser(user);
        }
    }

    private void generateExercises() {
        logger.info("Generating exercise data...");
        generateExercise("Demo", "Demoveranstaltung", "", 12, 305, 2, 14, 4, 30);
    }

    private void generateExercise(String exerciseId, String lecture, String term, int groups, int participants, int teamSize, int sheets, int assignmentsPerSheet, int pointsPerSheet) {
        createExerciseAndGroupsWithStudents(exerciseId, lecture, term, groups, participants, teamSize);
        createExerciseSheets(exerciseId, sheets, assignmentsPerSheet, pointsPerSheet);
        createAttendanceAndResults(exerciseId);
        createPreliminaryExam(exerciseId);
        createFinalExam(exerciseId);
        importUploads(exerciseId);
    }

    private void createExerciseAndGroupsWithStudents(String exerciseId, String lecture, String term, int groups, int participants, int teamSize) {
        exerciseDao.createOrUpdateExercise(new Exercise(exerciseId, lecture, term));
        User[] users = userDao.getAllUsers().toArray(new User[0]);
        Set<Integer> usedUsers = new HashSet<>();
        int maxSize = -Math.floorDiv(-participants, groups); // ceil(participants/groups)
        for (int i = 0; i < groups; i++) {
            // Create group
            String groupId = String.format("%0" + Integer.toString(groups).length() + "d", i + 1);
            int day = days.length * i / groups;
            int time = times.length * (days.length * i % groups) / groups;
            String location = generateLocation();
            Group group = new Group(exerciseId, groupId, days[day], times[time], location, maxSize);
            groupDao.createOrUpdateGroup(group);

            // Add random user as tutor
            User tutor;
            do {
                tutor = users[faker.number().numberBetween(0, users.length)];
            } while (!usedUsers.add(tutor.getUserid()));
            ExerciseRights tutorRights = new ExerciseRights();
            tutorRights.setExerciseId(exerciseId);
            tutorRights.setRole(ExerciseRights.Role.tutor);
            tutorRights.setGroupId(groupId);
            userDao.addUserRights(tutor.getUserid(), tutorRights);

            // Add random students
            int alreadyAdded = usedUsers.size() - (i + 1); // subtract tutors
            int remainingGroups = groups - i;
            int studentsToDistribute = participants - alreadyAdded - remainingGroups * (maxSize - 1);
            boolean additionalStudent = faker.number().numberBetween(0, remainingGroups) < studentsToDistribute;
            int studentsForThisGroup = additionalStudent ? maxSize : maxSize - 1;
            int teams = studentsForThisGroup / teamSize;
            for (int j = 0; j < studentsForThisGroup; j++) {
                User student;
                do {
                    student = users[faker.number().numberBetween(0, users.length)];
                } while (!usedUsers.add(student.getUserid()));
                ExerciseRights studentRights = new ExerciseRights();
                studentRights.setExerciseId(exerciseId);
                studentRights.setRole(ExerciseRights.Role.student);
                studentRights.setGroupId(group.getGroupId());
                studentRights.setTeamId(String.format("%0" + Integer.toString(teams).length() + "d", j % teams + 1));
                userDao.addUserRights(student.getUserid(), studentRights);
            }
        }
    }

    private void createExerciseSheets(String exerciseId, int sheets, int assignmentsPerSheet, int pointsPerSheet) {
        for (int i = 1; i <= sheets; i++) {
            String sheetId = String.format("%0" + Integer.toString(sheets).length() + "d", i);
            String sheetLabel = "Übungsblatt " + i;
            Sheet sheet = new Sheet(sheetId, exerciseId, sheetLabel);
            sheetDao.createOrUpdateSheet(sheet);

            int remainingPoints = pointsPerSheet;
            for (int j = 1; j <= assignmentsPerSheet; j++) {
                String assignmentId = Integer.toString(j);
                String assignmentLabel = "Aufgabe " + j;
                int points;
                if (j == assignmentsPerSheet) {
                    points = remainingPoints;
                    remainingPoints = 0;
                } else if (remainingPoints == 0) {
                    points = 0;
                } else {
                    points = faker.number().numberBetween(0, remainingPoints / 2 + 1);
                    remainingPoints -= points;
                }
                boolean statistics = faker.bool().bool();
                Assignment assignment = new Assignment(assignmentId, exerciseId, sheetId, assignmentLabel, points, statistics);
                assignmentDao.createOrUpdateAssignment(assignment);
            }
        }
    }

    private void createAttendanceAndResults(String exerciseId) {
        List<StudentInfo> students = userDao.getStudentsInExercise(exerciseId);
        for (Sheet sheet : sheetDao.getSheetsForExercise(exerciseId)) {
            // Attendance and teams
            for (StudentInfo student : students) {
                boolean attended = faker.number().randomDigit() <= 7; // 70%
                Attendance attendance = new Attendance(sheet.getId(), exerciseId, student.getId(), attended);
                attendanceDao.createOrUpdateAttendance(attendance);
                Studentres studentres = new Studentres(sheet.getId(), exerciseId, student.getId(), student.getTeam());
                resultsDao.createStudentres(studentres);
            }

            // Results
            for (Assignment assignment : assignmentDao.getAllAssignments(exerciseId, sheet.getId())) {
                for (StudentInfo student : students) {
                    Team team = student.getTeam();
                    Result result = resultsDao.getResult(exerciseId, sheet.getId(), assignment.getId(), team);
                    if (result == null) {
                        int maxPoints = (int) assignment.getMaxpoints();
                        int lower = maxPoints / faker.number().numberBetween(1, maxPoints + 1);
                        float points = (float) faker.number().numberBetween(lower, maxPoints);
                        result = new Result(assignment.getId(), sheet.getId(), exerciseId, team, points);
                        resultsDao.createOrUpdateResult(result);
                    }
                }
            }
        }
    }

    private void createPreliminaryExam(String exerciseId) {
        String examId = "ZK";
        String label = "Zwischenklausur";
        Calendar calendar = new GregorianCalendar();
        do {
            calendar.setTime(faker.date().past(90, 30, TimeUnit.DAYS));
        } while (ImmutableSet.of(Calendar.SATURDAY, Calendar.SUNDAY).contains(calendar.get(Calendar.DAY_OF_WEEK)));
        DateTime time = calendarToTime(calendar);
        String location = generateLocation();
        Exam exam = new Exam(examId, exerciseId, label, time, location, false, true);
        examDao.createOrUpdateExam(exam);

        List<StudentInfo> students = userDao.getStudentsInExercise(exerciseId);
        List<String> studentIds = students.stream().map(StudentInfo::getId).collect(Collectors.toList());
        examDao.addExamParticipants(exerciseId, examId, studentIds);
        // TODO: add tasks, points and grades
    }

    private void createFinalExam(String exerciseId) {
        String examId = "AK";
        String label = "Abschlussklausur";
        Calendar calendar = new GregorianCalendar();
        do {
            calendar.setTime(faker.date().future(90, 30, TimeUnit.DAYS));
        } while (ImmutableSet.of(Calendar.SATURDAY, Calendar.SUNDAY).contains(calendar.get(Calendar.DAY_OF_WEEK)));
        DateTime time = calendarToTime(calendar);
        String location = generateLocation();
        Exam exam = new Exam(examId, exerciseId, label, time, location, true, false);
        examDao.createOrUpdateExam(exam);
    }

    private void importUploads(String exerciseId) {
        File uploadsDir = Paths.get(".", Constants.DATA_PATH, exerciseId).toFile();
        if (uploadsDir.isDirectory()) {
            logger.info("Importing uploads for exercise {}...", exerciseId);
            for (File sheetDir : uploadsDir.listFiles()) {
                String sheetId = sheetDir.getName();
                if (sheetDao.getSheet(exerciseId, sheetId) == null) {
                    logger.warn("Not importing uploads for nonexistent sheet {} in exercise {}", sheetId, exerciseId);
                    continue;
                }
                Set<String> assignments = assignmentDao.getAllAssignments(exerciseId, sheetId).stream().map(Assignment::getId).collect(Collectors.toSet());
                for (File teamDir : sheetDir.listFiles()) {
                    Team team = TeamConverter.convertToTeam(teamDir.getName());
                    List<StudentInfo> teamMembers = userDao.getStudentsInTeam(exerciseId, team);
                    if (teamMembers.isEmpty()) {
                        logger.warn("Not importing uploads from nonexistent team \"{}\" for sheet {} in exercise {}", team, sheetId, exerciseId);
                        continue;
                    }
                    for (File assignmentDir : teamDir.listFiles()) {
                        String assignmentId = assignmentDir.getName();
                        if (!assignments.contains(assignmentId)) {
                            logger.warn("Not importing uploads for nonexistent assignment {} for sheet {} from team \"{}\" in exercise {}", assignmentId, sheetId, team, exerciseId);
                            continue;
                        }
                        for (File uploadFile : assignmentDir.listFiles()) {
                            String filename = uploadFile.getName();
                            DateTime uploadDate = UploadManager.internalFilenameToDate(filename);
                            String uploadName = UploadManager.internalFilenameToFilename(filename);
                            Upload upload = new Upload();
                            upload.setExercise(exerciseId);
                            upload.setSheet(sheetId);
                            upload.setTeam(team);
                            upload.setAssignment(assignmentId);
                            upload.setUploadDate(uploadDate);
                            upload.setFilename(uploadName);
                            upload.setUploaderStudentid(teamMembers.get(faker.number().numberBetween(0, teamMembers.size() - 1)).getId());
                            uploadsDao.insertUpload(upload);
                        }
                    }
                }
            }
        }
    }

    private String generateLocation() {
        int building = faker.number().numberBetween(1, 100);
        int room = faker.number().numberBetween(100, 600);
        return String.format("%d-%03d", building, room);
    }

    private DateTime calendarToTime(Calendar calendar) {
        DateTime time = new DateTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                faker.number().numberBetween(8, 16),
                faker.bool().bool() ? 0 : 30
        );
        return time;
    }
}
