package de.tukl.softech.exclaim.controllers;

import com.google.common.collect.ImmutableMap;
import de.tukl.softech.exclaim.dao.AssignmentDao;
import de.tukl.softech.exclaim.dao.SimilarityCheckerDao;
import de.tukl.softech.exclaim.dao.TestresultDao;
import de.tukl.softech.exclaim.dao.UploadsDao;
import de.tukl.softech.exclaim.data.Assignment;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Testresult;
import de.tukl.softech.exclaim.data.Upload;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.transferdata.TestResultDetails;
import de.tukl.softech.exclaim.transferdata.TestStatistics;
import de.tukl.softech.exclaim.utils.AsyncUtils;
import de.tukl.softech.exclaim.utils.RteServices;
import de.tukl.softech.exclaim.utils.RteServices.TestName;
import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class TestController {

    private TestresultDao testresultDao;

    private UploadsDao uploadsDao;

    private RteServices rteServices;
    private MetricsService metrics;
    private final AssignmentDao assignmentDao;
    private SimilarityCheckerDao similarityCheckerDao;

    public TestController(TestresultDao testresultDao, UploadsDao uploadsDao, RteServices rteServices,
                          MetricsService metrics, AssignmentDao assignmentDao, SimilarityCheckerDao similarityCheckerDao) {
        this.testresultDao = testresultDao;
        this.uploadsDao = uploadsDao;
        this.rteServices = rteServices;
        this.metrics = metrics;
        this.assignmentDao = assignmentDao;
        this.similarityCheckerDao = similarityCheckerDao;
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/{aid}/test")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exid, #groupId, #teamId, #sid)")
    public String postRequestTest(
            @PathVariable("exid") String exid,
            @PathVariable("sid") String sid,
            @PathVariable("aid") String aid,
            @RequestParam("group") String groupId,
            @RequestParam("team") String teamId,
            @RequestParam(value = "snapshot", required = false, defaultValue = "") String snapshotStr,
            RedirectAttributes redirectAttributes
    ) throws ExecutionException, InterruptedException {
        metrics.registerAccessTest();
        TestName testName = new TestName(exid, sid, aid);
        Team team = new Team(groupId, teamId);
        DateTime snapshot;
        if (!snapshotStr.isEmpty()) {
            snapshot = DateTime.parse(snapshotStr);
        } else {
            snapshot = DateTime.now();
        }
        int requestNr = rteServices.requestTest(testName, team, snapshot, true);
        redirectAttributes.addAttribute("group", groupId);
        redirectAttributes.addAttribute("team", teamId);
        redirectAttributes.addAttribute("requestNr", requestNr);
        return "redirect:/exercise/{exid}/sheet/{sid}/{aid}/team/{group}/{team}/test/{requestNr}";
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/alltests")
    @ResponseBody
    @PreAuthorize("@accessChecker.hasAdminRight(#exid)")
    public String postRequestAllTestsForSheet(
            @PathVariable("exid") String exid,
            @PathVariable("sid") String sid,
            @RequestParam(value = "snapshot", required = false) String snapshotStr
    ) {
        metrics.registerAccessAdmin();
        DateTime snapshot = (snapshotStr == null) ? null : DateTime.parse(snapshotStr);

        List<Upload> uploads = uploadsDao.getUploadsForSheet(exid, sid);
        Map<String, List<Upload>> byAssignment = uploads.stream().collect(Collectors.groupingBy(Upload::getAssignment));
        for (String aId : byAssignment.keySet()) {
            TestName testName = new TestName(exid, sid, aId);
            if (rteServices.isTestAvailable(testName)) {
                byAssignment.get(aId).stream()
                        .map(Upload::getTeam)
                        .distinct()
                        .forEach(team -> rteServices.requestTest(testName, team, snapshot, false));
            }
        }
        return "ok";
    }

    @GetMapping("/exercise/{exid}/sheet/{sid}/{aid}/team/{groupid}/{teamid}/test")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exid, #groupid, #teamid, #sid)")
    public String getTestResultOverview(
            @PathVariable("exid") String exid,
            @PathVariable("sid") String sid,
            @PathVariable("aid") String aid,
            @PathVariable("groupid") String groupid,
            @PathVariable("teamid") String teamid,
            Model model
    ) {
        metrics.registerAccessTest();
        return getTestResult(exid, sid, aid, groupid, teamid, -1, model);
    }


    @GetMapping("/exercise/{exid}/sheet/{sid}/{aid}/team/{groupid}/{teamid}/test/{requestnr}")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exid, #groupid, #teamid, #sid)")
    public String getTestResult(
            @PathVariable("exid") String exid,
            @PathVariable("sid") String sid,
            @PathVariable("aid") String aid,
            @PathVariable("groupid") String groupid,
            @PathVariable("teamid") String teamid,
            @PathVariable("requestnr") int requestnr,
            Model model

    ) {
        metrics.registerAccessTest();
        Team team = new Team(groupid, teamid);
        try {
            List<Testresult> testResults = testresultDao.getAllTestResultDetails(exid, sid, aid, team);
            Testresult testResult = testResults.stream()
                    .filter(tr -> tr.getRequestnr() == requestnr)
                    .findAny()
                    .orElse(null);

            //get map in reverse order
            Map<DateTime, List<Testresult>> resultsBySnapshot =
                    new TreeMap<>(testResults.stream().collect(Collectors.groupingBy(Testresult::getSnapshot))).descendingMap();
            resultsBySnapshot.values().forEach(Collections::reverse);

            List<DateTime> snapshots = uploadsDao.getSnapshots(exid, sid, aid, team);
            for (DateTime snapshot : snapshots) {
                if (!resultsBySnapshot.containsKey(snapshot)) {
                    resultsBySnapshot.put(snapshot, Collections.emptyList());
                }
            }

            if (testResult != null) {
                try {
                    model.addAttribute("testResultDetails", TestResultDetails.fromJson(testResult.getResult()));
                } catch (Exception e) {
                    // ignore
                }
                model.addAttribute("testResult", testResult);
            }

            Assignment assignment = assignmentDao.getAssignment(exid, sid, aid);

            model.addAttribute("exercise", exid);
            model.addAttribute("sheet", sid);
            model.addAttribute("assignment", aid);
            model.addAttribute("team", team);
            model.addAttribute("requestnr", requestnr);
            model.addAttribute("resultsBySnapshot", resultsBySnapshot);
            model.addAttribute("showstatistics", assignment.getStatistics());

            return "testresult";
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Test-Ergebnis nicht gefunden.");
        }
    }

    @GetMapping("/exercise/{exid}/sheet/{sheetid}/{assignid}/team/{gid}/{tid}/test/{requestnr}/statistics")
    @PreAuthorize("@accessChecker.hasShowUploadRight(#exercise, #groupid, #teamid, #sheet)")
    @ResponseBody
    public TestStatistics getTestStatistics(@PathVariable("exid") String exercise,
                                                  @PathVariable("sheetid") String sheet,
                                                  @PathVariable("assignid") String assignment,
                                                  @PathVariable("gid") String groupid,
                                                  @PathVariable("tid") String teamid,
                                                  @PathVariable("requestnr") int requestnr) {
        metrics.registerAccessTestStatistics();
        Team team = new Team(groupid, teamid);
        List<TestStatistics.TestPassed> testPassed = testresultDao.getTestsPassedByTeam(exercise, sheet, assignment);

        //map with number of passed tests: number of teams who passed n tests
        Map <Integer, Long> passedCount = testPassed.stream()
                .collect(Collectors.groupingBy(TestStatistics.TestPassed::getPassed, Collectors.counting()));

        Optional<TestStatistics.TestPassed> testPassedByTeam = testPassed.stream()
                .filter(testPassed1 -> testPassed1.getTeam().equals(team)).findFirst();

        int passedByTeam = 0;
        long teamPlace;
        if (testPassedByTeam.isPresent()) {
            passedByTeam = testPassedByTeam.get().getPassed();
            teamPlace = testPassed.stream().filter(t -> t.getPassed() > testPassedByTeam.get().getPassed()
                || (t.getPassed() == testPassedByTeam.get().getPassed()
                    && t.getSnapshot().isBefore(testPassedByTeam.get().getSnapshot()))).count() + 1;
        } else {
            teamPlace = testPassed.stream().filter(t -> t.getPassed() > 0).count() + 1;
        }

        Assignment a = assignmentDao.getAssignment(exercise, sheet, assignment);
        if (!a.getStatistics()) {
            // return dummy statistics
            return new TestStatistics(passedByTeam, 1, ImmutableMap.of(passedByTeam, 1L));
        }

        return new TestStatistics(passedByTeam, (int) teamPlace, passedCount);
    }

    @PostMapping("/exercise/{exid}/sheet/{sid}/similarityCheckerStartTest")
    @ResponseBody
    @PreAuthorize("@accessChecker.isAssistant()")
    public String postRequestAllTestsForSheet(
            @PathVariable("exid") String exid,
            @PathVariable("sid") String sid
    ) throws IOException {
        metrics.registerAccessAdmin();

        //make files with paths
        similarityCheckerDao.makePathsToUploads(exid, sid);


        //loop through all of these and check them
        File directoryPath = new File("similarityChecker/files/" + exid + "/" + sid + "/paths/");
        String[] contentsOfSheet = directoryPath.list();

        for (String file : contentsOfSheet) {
            ArrayList<String> argumentsForChecker = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(
                    "similarityChecker/files/" + exid + "/" + sid + "/paths/" + file))) {
                String line = reader.readLine();
                while (line != null) {
                    argumentsForChecker.add(line);
                    // read next line
                    line = reader.readLine();
                }

                //run the checks, but only if there is at least two files
                if (argumentsForChecker.size() >= 2) {
                    similarityCheckerDao.runCheck(argumentsForChecker, exid, sid, file);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //add empty files, so that the number of assignments matches up
        directoryPath = new File("similarityChecker/files/" + exid + "/" + sid + "/checks/");
        contentsOfSheet = directoryPath.list();

        if (contentsOfSheet == null) {
            return "error";
        }
        //find out max assingment id
        int max = 0;
        for (String oneResult : contentsOfSheet) {
            int currAssignment = Integer.parseInt(oneResult.split("_")[0]);
            if (currAssignment > max) {
                max = currAssignment;
            }
        }

        for (int i = 1; i < max; i++) {
            File result = new File("similarityChecker/files/" + exid + "/" + sid + "/checks/" + i + "_Results.txt");
            if (!result.exists()) {
                result.createNewFile();
            }
        }


        return "ok";
    }


}
