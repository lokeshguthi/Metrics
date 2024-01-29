package de.tukl.softech.exclaim.monitoring;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final CollectorRegistry registry;
    private final Counter accessCounter;
    private final Histogram uploadSize;
    private final Counter errorCounter;

    public MetricsService(CollectorRegistry registry) {
        this.registry = registry;

        accessCounter = Counter.build()
                .name("exclaim_access_total").help("Total number of accesses")
                .labelNames("page").register(registry);
        uploadSize = Histogram.build()
                .name("exclaim_upload_size").help("Size of uploaded files")
                // 1KB 100KB 1MB
                .buckets(1024, 102400, 1048567).register(registry);
        errorCounter = Counter.build()
                .name("exclaim_errors_total").help("Total number of errors").register(registry);

        accessCounter.labels("attendance").inc(0);
        accessCounter.labels("upload").inc(0);
        accessCounter.labels("overview").inc(0);
        accessCounter.labels("assessment").inc(0);
        accessCounter.labels("admin").inc(0);
        accessCounter.labels("file").inc(0);
        accessCounter.labels("annotations").inc(0);
        accessCounter.labels("feedback").inc(0);
        accessCounter.labels("home").inc(0);
        accessCounter.labels("zip").inc(0);
        accessCounter.labels("test").inc(0);
        accessCounter.labels("teststatistics").inc(0);
        accessCounter.labels("exam-admin").inc(0);
        accessCounter.labels("exam").inc(0);
        accessCounter.labels("user").inc(0);
        accessCounter.labels("activation").inc(0);
        accessCounter.labels("group").inc(0);
    }

    public CollectorRegistry getRegistry() {
        return registry;
    }

    public void registerAccess(String page) {
        accessCounter.labels(page).inc();
    }

    public void registerAccessExercise() {
        registerAccess("attendance");
    }

    public void registerAccessUpload() {
        registerAccess("upload");
    }

    public void registerAccessOverview() {
        registerAccess("overview");
    }

    public void registerAccessAssessment() {
        registerAccess("assessment");
    }

    public void registerAccessAdmin() {
        registerAccess("admin");
    }

    public void registerAccessFile() {
        registerAccess("file");
    }

    public void registerAccessAnnotations() {
        registerAccess("annotations");
    }

    public void registerAccessFeedback() {
        registerAccess("feedback");
    }

    public void registerAccessHome() {
        registerAccess("home");
    }

    public void registerAccessZip() {
        registerAccess("zip");
    }

    public void registerAccessTest() {
        registerAccess("test");
    }

    public void registerAccessTestStatistics() {
        registerAccess("teststatistics");
    }

    public void registerAccessExam() {
        registerAccess("exam");
    }

    public void registerAccessUser() {
        registerAccess("user");
    }

    public void registerAccessActivation() {
        registerAccess("activation");
    }

    public void registerAccessGroup() {
        registerAccess("group");
    }

    public void registerUploadSize(double size) {
        uploadSize.observe(size);
    }

    public void registerException(Throwable e) {
        errorCounter.inc();
    }
}
