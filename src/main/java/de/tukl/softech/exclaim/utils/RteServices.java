package de.tukl.softech.exclaim.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tukl.softech.exclaim.dao.TestresultDao;
import de.tukl.softech.exclaim.dao.UploadsDao;
import de.tukl.softech.exclaim.dao.WarningsDao;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.data.Testresult;
import de.tukl.softech.exclaim.data.Upload;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.transferdata.*;
import de.tukl.softech.exclaim.uploads.UploadManager;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class RteServices {
    private static final Logger logger = LoggerFactory.getLogger(RteServices.class);
    private static final String EXCLAIM_API_URL = "";

    /**
     * how many concurrent requests to RTE can be executed simultaneously
     */
    private static final int testConcurrency = 5;


    /**
     * how often to retry a failed test-run
     */
    private static final int maxRetries = 5;
    /**
     * queue for interactive-requests
     */
    private final LinkedHashSet<Job> jobQueue = new LinkedHashSet<>();
    /**
     * queue for batch-requests
     */
    private final LinkedHashSet<Job> jobQueueBatch = new LinkedHashSet<>();

    @Value("${exclaim.rteurl}")
    private String rteUrl;
    @Value("${exclaim.apiKey}")
    private String apiKey;
    private UploadManager uploadManager;
    private TestresultDao testresultDao;
    private UploadsDao uploadsDao;
    private MetricsService metrics;
    private SimpMessagingTemplate broker;
    private TransactionTemplate transactionTemplate;
    private WarningsDao warningsDao;
    /**
     * A cache for the list of available tests
     */
    private AtomicReference<TestCache> availableTestsCache = new AtomicReference<>();

    public RteServices(UploadManager uploadManager, TestresultDao testresultDao, UploadsDao uploadsDao,
                       MetricsService metrics, SimpMessagingTemplate broker, TransactionTemplate transactionTemplate,
                       WarningsDao warningsDao) {
        this.uploadManager = uploadManager;
        this.testresultDao = testresultDao;
        this.uploadsDao = uploadsDao;
        this.metrics = metrics;
        this.broker = broker;
        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.warningsDao = warningsDao;

        new JobQueueExporter().register(metrics.getRegistry());

        for (int i = 0; i < testConcurrency; i++) {
            processLoop();
        }
    }

    private <T> T rteGet(String url, MultiValueMap<String, String> params, Class<T> resultType) {
        RestTemplate restTemplate = new RestTemplate();

        logger.debug("Access to RTE under '{}{}{}'", rteUrl, EXCLAIM_API_URL, url);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(rteUrl + EXCLAIM_API_URL + url)
                .queryParams(params)
                .queryParam("apiKey", apiKey);

//        ResponseEntity<T> response = restTemplate.getForEntity(builder.build().toUri(), resultType);
        ResponseEntity<String> response = restTemplate.getForEntity(builder.build().toUri(), String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(response.getBody(), resultType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            return  response.getBody();
        }
        throw new IllegalStateException("Communication problem with RTE");
    }

    private <T> T rtePost(String url, Object request, Class<T> resultType) {
        RestTemplate restTemplate = new RestTemplate();

        logger.debug("Access to RTE under '{}{}{}'", rteUrl, EXCLAIM_API_URL, url);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(rteUrl + EXCLAIM_API_URL + url)
                .queryParam("apiKey", apiKey);

        ResponseEntity<T> response = restTemplate.postForEntity(builder.build().toUri(), request, resultType);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        throw new IllegalStateException("Communication problem with RTE");
    }

    /**
     * asks the rte which tests are available
     * returns a list of testnames
     */
    public List<TestName> availableTestsRequest() {
        RteListTestsResponse res = rteGet("/listtests", new LinkedMultiValueMap<>(), RteListTestsResponse.class);
        return res.getTests().stream()
                .map(TestName::fromPath)
                .filter(Optional::isPresent) // in Java 9 filter+map can be replaced with flatMap Optional::stream
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<TestName> updateAvailableTestsCache() {
        try {
            List<TestName> testNames = availableTestsRequest();
            availableTestsCache.set(new TestCache(testNames));
            return testNames;
        } catch (RestClientException e) {
            logger.error("Could not contact RTE to get available tests: " + e.getMessage());
            metrics.registerException(e);
            return null;
        } catch (Exception e) {
            logger.error("Error updating available tests cache", e);
            metrics.registerException(e);
            return null;
        }
    }

    private List<TestName> availableTests() {
        TestCache cached = availableTestsCache.get();
        if (cached == null || cached.time.isBefore(LocalDateTime.now().minus(5, ChronoUnit.SECONDS))) {
            List<TestName> tests = updateAvailableTestsCache();
            if (tests != null) {
                return tests;
            }
        }
        if (cached == null) {
            return Collections.emptyList();
        }
        return cached.tests;
    }

    private boolean testExists(TestName testName) {
        return availableTests().contains(testName);
    }

    /**
     * Executes the test with the given name
     */
    public RteResult executeTest(Job job) {

        LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("test", job.testName.toString());

        List<Upload> uploads = job.uploads;

        params.add("numfiles", uploads.size());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        for (int i = 0; i < uploads.size(); i++) {
            Upload upload = uploads.get(i);
            File file = uploadManager.getUploadFile(upload);
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + file.getAbsolutePath());
            }
            FileSystemResource r = new FileSystemResource(file) {
                @Override
                public String getFilename() {
                    return UploadManager.internalFilenameToFilename(file.getName());
                }
            };
            params.add("file" + i, r);
        }
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(params, headers);
        broker.convertAndSend(testResultsChannel(job), TestResultMsg.started(job));
        String res = rtePost("/test", requestEntity, String.class);
        return RteResult.fromJson(res);
    }

    /**
     * adds a new job to the queue
     */
    private void jobEnque(Job job, boolean interactive) {
        synchronized (jobQueue) {
            LinkedHashSet<Job> q = interactive ? this.jobQueue : this.jobQueueBatch;

            if (!q.contains(job)) {
                q.add(job);
                this.jobQueue.notify();
            }
        }
    }

    /**
     * gets the next job from the job queue.
     * Blocks until a job is available.
     */
    private Job jobDequeue() throws InterruptedException {
        while (true) {
            synchronized (jobQueue) {
                if (jobQueue.isEmpty() && jobQueueBatch.isEmpty()) {
                    jobQueue.wait(60000);
                } else if (!jobQueue.isEmpty()) {
                    Iterator<Job> it = jobQueue.iterator();
                    Job res = it.next();
                    it.remove();
                    return res;
                } else if (!jobQueueBatch.isEmpty()) {
                    Iterator<Job> it = jobQueueBatch.iterator();
                    Job res = it.next();
                    it.remove();
                    return res;

                }
            }
        }
    }

    private class JobQueueExporter extends Collector {

        @Override
        public List<MetricFamilySamples> collect() {
            List<MetricFamilySamples> mfs = new ArrayList<>();
            mfs.add(new GaugeMetricFamily(
                    "exclaim_job_backlog",
                    "Number of jobs in the job queue",
                    getJobCount()));
            mfs.add(new GaugeMetricFamily(
                    "exclaim_batchjob_backlog",
                    "Number of batch jobs in the batch job queue",
                    getBatchJobCount()));
            return mfs;
        }
    }

    public int getJobCount() {
        synchronized (jobQueue) {
            return jobQueue.size();
        }
    }

    public int getBatchJobCount() {
        synchronized (jobQueue) {
            return jobQueueBatch.size();
        }
    }

    /**
     * submits the test and stores results in the database
     */
    private Testresult submitTestRequest(Job job) throws JsonProcessingException {
        logger.debug("Processing job " + job);
        try {
            DateTime timeStarted = DateTime.now();
            RteResult result = executeTest(job);
            TestResultDetails details = result.getTest_result();
            List<ClocResult> clocResults = result.getCloc_result();

            DateTime timeDone = DateTime.now();

            Testresult res = new Testresult();
            res.setExercise(job.testName.exerciseId);
            res.setSheet(job.testName.sheetId);
            res.setAssignment(job.testName.assignmentId);
            res.setTeam(job.team);
            res.setRequestnr(job.requestNr);
            res.setTimeStarted(timeStarted);
            res.setTimeDone(timeDone);

            res.setCompiled(details.isCompiled());
            res.setInternalError(details.getInternal_error() != null);
            res.setTestsTotal(details.getTests_executed());
            res.setTestsPassed(details.getTests_executed() - details.getTests_failed());
            res.setMissingFiles(details.hasMissing_files());
            res.setIllegalFiles(details.hasIllegal_files());
            res.setResult(new ObjectMapper().writeValueAsString(details));

            // metrics from the test file set here from the clocResult set generated from RTE
            res.setCodeCommentsNumber(clocResults.get(0).getComments_number());
            res.setLinesOfCodeNumber(clocResults.get(0).getLoc_number());
            
            testresultDao.storeTestResult(res);
            warningsDao.deleteWarnings(job.uploads.stream().map(Upload::getId).collect(Collectors.toSet()));

            if (result.getFile_warnings() != null && result.getFile_warnings().size() > 0) {
                Iterator<FileWarnings> fileWarningsIterator = result.getFile_warnings().iterator();
                while (fileWarningsIterator.hasNext()) {
                    FileWarnings file = fileWarningsIterator.next();
                    for (Upload upload : job.uploads) {
                        if (file.getFilename().equals(upload.getFilename())) {
                            file.setFileId(upload.getId());
                            break;
                        }
                    }
                    if (file.getFileId() == 0) {
                        logger.warn("Could not find upload for file: " + file.getFilename());
                        fileWarningsIterator.remove();
                    }
                }
                warningsDao.createWarnings(result.getFile_warnings());
            }

            return res;
        } catch (Exception e) {
            String msg = "Error when executing testcase " + job;
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException ce = (HttpClientErrorException) e;
                msg += "\n" + ce.getResponseBodyAsString();
            }
            logger.error(msg, e);
            Testresult oldDetails = testresultDao.getTestResultDetails(job.testName.exerciseId, job.testName.sheetId, job.testName.assignmentId, job.team, job.requestNr);

            if (oldDetails.getRetries() < maxRetries) {
                // increment test result tries
                testresultDao.updateTestresultTries(oldDetails);
                // job will be picked up later by recoverLoop
            } else {
                // Too many retries, just store the error
                oldDetails.setResult(e.getMessage());
                oldDetails.setCompiled(false);
                testresultDao.storeTestResult(oldDetails);
            }
            throw e;
        }
    }

    public static class TestResultMsg {
        String exercise;
        String sheet;
        String assignment;
        String group;
        String team;
        int request;
        String status;

        public TestResultMsg() {
        }

        private TestResultMsg(String exercise, String sheet, String assignment, String group, String team, int request) {
            this.exercise = exercise;
            this.sheet = sheet;
            this.assignment = assignment;
            this.group = group;
            this.team = team;
            this.request = request;
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

        public String getAssignment() {
            return assignment;
        }

        public void setAssignment(String assignment) {
            this.assignment = assignment;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public int getRequest() {
            return request;
        }

        public void setRequest(int request) {
            this.request = request;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public static TestResultMsg started(Job job) {
            TestResultMsg res = new TestResultMsg(job.testName.exerciseId, job.testName.sheetId, job.testName.assignmentId, job.team.getGroup(), job.team.getTeam(), job.requestNr);
            res.status = "started";
            return res;
        }

        public static TestResultMsg done(Job job) {
            TestResultMsg res = new TestResultMsg(job.testName.exerciseId, job.testName.sheetId, job.testName.assignmentId, job.team.getGroup(), job.team.getTeam(), job.requestNr);
            res.status = "done";
            return res;
        }

        public static TestResultMsg failed(Job job) {
            TestResultMsg res = new TestResultMsg(job.testName.exerciseId, job.testName.sheetId, job.testName.assignmentId, job.team.getGroup(), job.team.getTeam(), job.requestNr);
            res.status = "failed";
            return res;
        }
    }

    private void processJobFromQueue() throws InterruptedException {
        Job job = jobDequeue();
        try {
            submitTestRequest(job);
            broker.convertAndSend(testResultsChannel(job), TestResultMsg.done(job));
        } catch (Exception exception) {
            broker.convertAndSend(testResultsChannel(job), TestResultMsg.failed(job));
        }
    }

    public static String testResultsChannel(Job job) {
        return "/topic/testresults/" + job.testName.exerciseId + "/" + job.testName.sheetId + "/" + job.team.getGroup() + "/" + job.team.getTeam();
    }

    private void processLoop() {
        Thread t = new Thread(() -> {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    processJobFromQueue();
                }
            } catch (InterruptedException e) {
                logger.error("RTE processLoop interrupted", e);
            }
        });
        t.start();
    }


    @Scheduled(fixedRateString = "PT60s")
    public void recoverUnfinishedJobs() {
        try {
            List<Testresult> unfinished = testresultDao.getUnfinishedTestRequests();
            for (Testresult u : unfinished) {
                List<Upload> uploads = uploadsDao.getSnapshot(u.getExercise(), u.getSheet(), u.getAssignment(), u.getTeam(), u.getSnapshot());
                jobEnque(new Job(
                        new TestName(u.getExercise(), u.getSheet(), u.getAssignment()),
                        u.getTeam(),
                        u.getRequestnr(),
                        uploads
                ), false);
            }
        } catch (Exception e) {
            logger.warn("Could not recover unfinished jobs", e);
            metrics.registerException(e);
        }
    }

    /**
     * request to execute a test for a given team on RTE.
     */
    public int requestTest(TestName testName, Team team, DateTime snapshot, boolean interactive) {
        DateTime testSnapshot;
        if (snapshot == null) {
            // get latest snapshot:
            List<DateTime> snapshots = uploadsDao.getSnapshots(testName.exerciseId, testName.sheetId, testName.assignmentId, team);
            testSnapshot = snapshots.stream().max(Comparator.naturalOrder()).orElseGet(DateTime::now);
        } else {
            testSnapshot = snapshot;
        }


        // get current files associated with this assignment:
        List<Upload> uploads = uploadsDao.getSnapshot(testName.exerciseId, testName.sheetId, testName.assignmentId, team, testSnapshot);

        Testresult testResult;
        // we use synchronization here, because the transaction alone does not guarantee
        // that we won't get a conflict (would be better to use an IDENTITY type column)
        synchronized (this) {
            testResult = transactionTemplate.execute(status -> {
                // get a request number
                int requestNr = testresultDao.getNextRequestnr(testName.exerciseId, testName.sheetId, testName.assignmentId, team);

                // insert request into database
                Testresult tr = new Testresult();
                tr.setExercise(testName.exerciseId);
                tr.setSheet(testName.sheetId);
                tr.setAssignment(testName.assignmentId);
                tr.setTeam(team);
                tr.setRequestnr(requestNr);
                tr.setTimeRequest(DateTime.now());
                tr.setSnapshot(testSnapshot);
                testresultDao.createTestresult(tr);
                return tr;
            });
        }


        jobEnque(new Job(
                testName,
                team,
                testResult.getRequestnr(),
                uploads
        ), interactive);
        return testResult.getRequestnr();
    }

    public boolean isTestAvailable(TestName testName) {
        return availableTests().contains(testName);
    }

    private static class TestCache {
        /**
         * time the cache was last updated
         */
        LocalDateTime time;

        /**
         * list of tests in the cache
         */
        List<TestName> tests;

        public TestCache(List<TestName> tests) {
            this.time = LocalDateTime.now();
            this.tests = tests;
        }

    }

    public static class Job {
        TestName testName;
        Team team;
        int requestNr;
        List<Upload> uploads;

        public Job(TestName testName, Team team, int requestNr, List<Upload> uploads) {
            this.testName = testName;
            this.team = team;
            this.requestNr = requestNr;
            this.uploads = uploads;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Job job = (Job) o;
            return requestNr == job.requestNr &&
                    Objects.equals(testName, job.testName) &&
                    Objects.equals(team, job.team);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testName, team, requestNr);
        }

        @Override
        public String toString() {
            return "Job (" + testName + ", " + team + ", " + requestNr + ", " + uploads + ")";
        }

    }

    public static class TestName {
        private final String exerciseId;
        private final String sheetId;
        private final String assignmentId;

        public TestName(String exerciseId, String sheetId, String assignmentId) {
            this.exerciseId = exerciseId;
            this.sheetId = sheetId;
            this.assignmentId = assignmentId;
        }

        static Optional<TestName> fromPath(String path) {
            String[] parts = path.split("[/\\\\]");
            if (parts.length == 3) {
                return Optional.of(new TestName(parts[0], parts[1], parts[2]));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public String toString() {
            return exerciseId + "/" + sheetId + "/" + assignmentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestName testName = (TestName) o;
            return Objects.equals(exerciseId, testName.exerciseId) &&
                    Objects.equals(sheetId, testName.sheetId) &&
                    Objects.equals(assignmentId, testName.assignmentId);
        }


        @Override
        public int hashCode() {
            return Objects.hash(exerciseId, sheetId, assignmentId);
        }
    }

}
