
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import proj.Job;
import proj.JobApplication;
import proj.JobRepository;
import proj.JobService;
import proj.JobStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest(classes = JobApplication.class)
public class JobConcurrencyTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Test
    public void testConcurrentJobProcessingPreventsDuplicateExecution() throws InterruptedException {
        // Arrange: Seed a fresh valid job into the database
        Job job = Job.builder()
                .payload("{\"fail\": false}")
                .status(JobStatus.PENDING)
                .retryCount(0)
                .build();
        final Job savedJob = jobRepository.save(job);

        int numberOfThreads = 5;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        // Act: Fire off multiple processing requests concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            service.submit(() -> {
                try {
                    latch.await(); // wait for the start signal
                    jobService.processPendingJob(savedJob.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Open the floodgates for all threads simultaneously
        doneLatch.await(); // Wait for all threads to complete execution

        // Assert: Reload job state. It should be completed cleanly without duplicate iterations
        Job updatedJob = jobRepository.findById(savedJob.getId()).orElseThrow();
        Assertions.assertEquals(JobStatus.COMPLETED, updatedJob.getStatus());
        Assertions.assertEquals(0, updatedJob.getRetryCount()); 
    }
}
